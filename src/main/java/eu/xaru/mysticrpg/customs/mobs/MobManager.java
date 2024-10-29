package eu.xaru.mysticrpg.customs.mobs;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.IndicatorManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class MobManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, CustomMob> mobConfigurations;
    private final Map<UUID, CustomMobInstance> activeMobs = new HashMap<>();
    private final Random random = new Random();
    private final ItemManager itemManager;
    private final IndicatorManager indicatorManager;
    private final EconomyHelper economyHelper;


    public MobManager(JavaPlugin plugin, Map<String, CustomMob> mobConfigurations, EconomyHelper economyHelper) {
        this.plugin = plugin;
        this.economyHelper = economyHelper;
        this.mobConfigurations = mobConfigurations;
        this.itemManager = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class).getItemManager();
        this.indicatorManager = ModuleManager.getInstance().getModuleInstance(IndicatorManager.class);


        Bukkit.getPluginManager().registerEvents(this, plugin);

    }

    /**
     * Spawns a custom mob at the specified location.
     *
     * @param customMob The custom mob configuration.
     * @param location  The location where the mob will spawn.
     */
    public void spawnMobAtLocation(CustomMob customMob, Location location) {
        if (customMob == null) {
            Bukkit.getLogger().log(Level.WARNING, "CustomMob is null.");
            return;
        }
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, customMob.getEntityType());

        // Set custom attributes
        applyCustomAttributes(mob, customMob);

        // Set custom HP and name tag
        mob.setCustomName(createMobNameTag(customMob, customMob.getHealth()));
        mob.setCustomNameVisible(true);
        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customMob.getHealth());
        }
        mob.setHealth(customMob.getHealth());

        CustomMobInstance mobInstance = new CustomMobInstance(customMob, location, mob);
        activeMobs.put(mob.getUniqueId(), mobInstance);

        Bukkit.getLogger().log(Level.INFO, "Spawned mob: " + customMob.getName() + " at location: " + location);
    }

    /**
     * Applies custom attributes to the mob.
     *
     * @param mob       The mob entity.
     * @param customMob The custom mob configuration.
     */
    private void applyCustomAttributes(LivingEntity mob, CustomMob customMob) {
        // Apply movement speed
        AttributeInstance speedAttribute = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(customMob.getMovementSpeed());
        }

        // Apply base armor
        AttributeInstance armorAttribute = mob.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(customMob.getBaseArmor());
        }

        // Apply base damage
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(customMob.getBaseDamage());
        }

        // Set equipment if any
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null && customMob.getEquipment() != null) {
            // Helmet
            if (customMob.getEquipment().getHelmet() != null) {
                ItemStack helmet = getCustomItemStack(customMob.getEquipment().getHelmet());
                equipment.setHelmet(helmet);
            }
            // Chestplate
            if (customMob.getEquipment().getChestplate() != null) {
                ItemStack chestplate = getCustomItemStack(customMob.getEquipment().getChestplate());
                equipment.setChestplate(chestplate);
            }
            // Leggings
            if (customMob.getEquipment().getLeggings() != null) {
                ItemStack leggings = getCustomItemStack(customMob.getEquipment().getLeggings());
                equipment.setLeggings(leggings);
            }
            // Boots
            if (customMob.getEquipment().getBoots() != null) {
                ItemStack boots = getCustomItemStack(customMob.getEquipment().getBoots());
                equipment.setBoots(boots);
            }
            // Weapon
            if (customMob.getEquipment().getWeapon() != null) {
                ItemStack weapon = getCustomItemStack(customMob.getEquipment().getWeapon());
                equipment.setItemInMainHand(weapon);
            }
        }
    }

    /**
     * Retrieves a custom ItemStack from the ItemManager.
     *
     * @param customItemId The ID of the custom item.
     * @return The ItemStack if found; otherwise, null.
     */
    private ItemStack getCustomItemStack(String customItemId) {
        CustomItem customItem = itemManager.getCustomItem(customItemId);
        if (customItem != null) {
            return customItem.toItemStack();
        }
        return null;
    }

    public CustomMob getCustomMobFromEntity(LivingEntity entity) {
        CustomMobInstance mobInstance = findMobInstance(entity);
        if (mobInstance != null) {
            return mobInstance.getCustomMob();
        }
        return null;
    }

    /**
     * Creates a formatted name tag for the mob.
     *
     * @param customMob    The custom mob configuration.
     * @param currentHealth The current health of the mob.
     * @return The formatted name tag.
     */
    private String createMobNameTag(CustomMob customMob, double currentHealth) {
        return ChatColor.translateAlternateColorCodes('&',
                String.format("[LVL%d] %s [%.1f❤]", customMob.getLevel(), customMob.getName(), currentHealth));
    }

    public Map<String, CustomMob> getMobConfigurations() {
        return mobConfigurations;
    }


    /**
     * Finds the CustomMobInstance associated with the given entity.
     *
     * @param entity The entity to search for.
     * @return The CustomMobInstance if found; otherwise, null.
     */
    public CustomMobInstance findMobInstance(LivingEntity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    /**
     * Handles damage events for mobs.
     *
     * @param event The damage event.
     */
    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        CustomMobInstance mobInstance = findMobInstance(livingEntity);
        if (mobInstance == null) return; // Not a custom mob

        CustomMob customMob = mobInstance.getCustomMob();

        // Capture the damage before modifying it
        double damage = event.getFinalDamage();

        // Reduce custom HP
        double currentHp = mobInstance.getCurrentHp() - damage;
        mobInstance.setCurrentHp(currentHp);

        // Update name tag
        livingEntity.setCustomName(createMobNameTag(customMob, currentHp));

        // Show damage indicator at mob's location
        if (indicatorManager != null && damage > 0) {
            Location location = livingEntity.getLocation().clone().add(0, livingEntity.getHeight() / 2, 0);
            indicatorManager.showDamageIndicator(location, damage);
        } else {
            if (indicatorManager == null) {
                Bukkit.getLogger().log(Level.WARNING, "IndicatorManager is not initialized.");
            }
        }

        // Check if mob is dead
        if (currentHp <= 0) {
            // Mob dies
            activeMobs.remove(livingEntity.getUniqueId());
            // Handle mob death
            LivingEntity killer = null;
            if (event instanceof EntityDamageByEntityEvent edbe) {
                if (edbe.getDamager() instanceof LivingEntity attacker) {
                    killer = attacker;
                }
            }
            handleMobDeath(mobInstance, killer);
            livingEntity.remove();
        }

        // Set event damage to zero to prevent default health reduction
        event.setDamage(0);

        // Apply knockback if applicable
        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player playerAttacker) {
                // Apply knockback to the mob
                Vector direction = livingEntity.getLocation().toVector().subtract(playerAttacker.getLocation().toVector()).normalize();
                livingEntity.setVelocity(direction.multiply(0.4));
            }
        }
    }

    /**
     * Handles the death of a mob, awarding XP and dropping items.
     *
     * @param mobInstance The instance of the dead mob.
     * @param killer      The entity that killed the mob.
     */
    public void handleMobDeath(CustomMobInstance mobInstance, LivingEntity killer) {
        // Log mob death
        Bukkit.getLogger().log(Level.INFO, "Mob died: " + mobInstance.getCustomMob().getName());

        if (killer instanceof Player player) {
            double currencyReward = mobInstance.getCustomMob().getCurrencyReward();

            // [ADDED] Define the chance for currency reward (100% for testing)
            double currencyChance = 1.0; // 1.0 represents 100%

            if (random.nextDouble() <= currencyChance) {
                if (economyHelper != null) {
                    economyHelper.addBalance(player, currencyReward);
                    player.sendMessage(ChatColor.GOLD + "You have received $" + currencyReward + " for killing " + mobInstance.getCustomMob().getName() + "!");
                } else {
                    player.sendMessage(ChatColor.RED + "Economy system is not available. Cannot reward currency.");
                }
            }
            // [ADDED] Update quest progress in QuestModule
            QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
            if (questModule != null) {
                questModule.updateQuestProgressOnMobDeath(player, mobInstance.getCustomMob());
            } else {
                Bukkit.getLogger().warning("QuestModule is not available. Cannot update quest progress.");
            }
        }

        // Give XP to the player who killed the mob
        if (killer instanceof Player player) {
            int experienceReward = mobInstance.getCustomMob().getExperienceReward();

            // Retrieve LevelModule instance when needed
            LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
            if (levelModule != null) {
                levelModule.addXp(player, experienceReward);

                // Show XP indicator at the mob's death location
                if (indicatorManager != null) {
                    Location deathLocation = mobInstance.getEntity().getLocation();
                    indicatorManager.showXPIndicator(deathLocation, experienceReward);
                } else {
                    Bukkit.getLogger().log(Level.WARNING, "IndicatorManager is not initialized.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Unable to add XP. LevelModule is not available.");
                Bukkit.getLogger().severe("LevelModule is not available. Cannot add XP to player.");
            }
        }

        // Handle drops
        for (CustomMob.DropItem dropItem : mobInstance.getCustomMob().getDrops()) {
            if (random.nextDouble() <= dropItem.getChance()) {
                ItemStack drop = null;
                if ("custom_item".equalsIgnoreCase(dropItem.getType())) {
                    // Get the custom item from ItemManager
                    CustomItem customItem = itemManager.getCustomItem(dropItem.getId());
                    if (customItem != null) {
                        drop = customItem.toItemStack();
                    }
                } else if ("material".equalsIgnoreCase(dropItem.getType())) {
                    Material material = Material.matchMaterial(dropItem.getMaterial().toUpperCase());
                    if (material != null) {
                        drop = new ItemStack(material, dropItem.getAmount());
                    }
                }

                if (drop != null) {
                    mobInstance.getEntity().getWorld().dropItemNaturally(mobInstance.getEntity().getLocation(), drop);
                }
            }
        }
    }
}
