package eu.xaru.mysticrpg.customs.mobs;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.party.Party;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
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
    
    private final EconomyHelper economyHelper;
    private final PartyHelper partyHelper;

    public MobManager(JavaPlugin plugin, Map<String, CustomMob> mobConfigurations, EconomyHelper economyHelper) {
        this.plugin = plugin;
        this.economyHelper = economyHelper;
        this.mobConfigurations = mobConfigurations;
        this.itemManager = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class).getItemManager();
        

        // Get PartyHelper instance
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule != null) {
            this.partyHelper = partyModule.getPartyHelper();
        } else {
            this.partyHelper = null;
            Bukkit.getLogger().warning("PartyModule is not available. Party features will be disabled.");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Spawns a custom mob at the specified location.
     *
     * @param customMob The custom mob configuration.
     * @param location  The location where the mob will spawn.
     */
    public CustomMobInstance spawnMobAtLocation(CustomMob customMob, Location location) {
        if (customMob == null) {
            Bukkit.getLogger().log(Level.WARNING, "CustomMob is null.");
            return null;
        }
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, customMob.getEntityType());

        // Set custom attributes
        applyCustomAttributes(mob, customMob);

        // Set custom HP and name tag
        mob.setCustomName(createMobNameTag(customMob, customMob.getHealth()));
        mob.setCustomNameVisible(true);
        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customMob.getHealth());
        }
        mob.setHealth(customMob.getHealth());

        CustomMobInstance mobInstance = new CustomMobInstance(customMob, location, mob);
        activeMobs.put(mob.getUniqueId(), mobInstance);

        Bukkit.getLogger().log(Level.INFO, "Spawned mob: " + customMob.getName() + " at location: " + location);

        return mobInstance;
    }



    /**
     * Applies custom attributes to the mob.
     *
     * @param mob       The mob entity.
     * @param customMob The custom mob configuration.
     */
    private void applyCustomAttributes(LivingEntity mob, CustomMob customMob) {
        // Apply movement speed
        AttributeInstance speedAttribute = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(customMob.getMovementSpeed());
        }

        // Apply base armor
        AttributeInstance armorAttribute = mob.getAttribute(Attribute.ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(customMob.getBaseArmor());
        }

        // Apply base damage
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.ATTACK_DAMAGE);
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
     * @param customMob     The custom mob configuration.
     * @param currentHealth The current health of the mob.
     * @return The formatted name tag.
     */
    private String createMobNameTag(CustomMob customMob, double currentHealth) {
        return Utils.getInstance().$(String.format("[LVL%d] %s [%.1f❤]", customMob.getLevel(), customMob.getName(), currentHealth));
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

        // Record the last damager if applicable
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager instanceof Player playerDamager) {
                mobInstance.setLastDamager(playerDamager.getUniqueId());
            } else if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player playerShooter) {
                    mobInstance.setLastDamager(playerShooter.getUniqueId());
                }
            }
        }

        // Reduce custom HP
        double currentHp = mobInstance.getCurrentHp() - damage;
        mobInstance.setCurrentHp(currentHp);

        // Update name tag
        livingEntity.setCustomName(createMobNameTag(customMob, currentHp));


        // Check if mob is dead
        if (currentHp <= 0) {
            // Mob dies
            activeMobs.remove(livingEntity.getUniqueId());

            // Handle mob death
            handleMobDeath(mobInstance);
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
     */
    public void handleMobDeath(CustomMobInstance mobInstance) {
        // Log mob death
        Bukkit.getLogger().log(Level.INFO, "Mob died: " + mobInstance.getCustomMob().getName());

        UUID killerUUID = mobInstance.getLastDamager();
        Bukkit.getLogger().log(Level.INFO, "Mob's last damager UUID: " + killerUUID);

        if (killerUUID != null) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && killer.isOnline()) {
                Bukkit.getLogger().log(Level.INFO, "Killer found: " + killer.getName());
                int experienceReward = mobInstance.getCustomMob().getExperienceReward();
                Bukkit.getLogger().log(Level.INFO, "Experience Reward: " + experienceReward);

                // Retrieve LevelModule instance when needed
                LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
                if (levelModule != null) {
                    Bukkit.getLogger().log(Level.INFO, "LevelModule found.");

                    // Get the party members
                    List<Player> partyMembers = new ArrayList<>();

                    if (partyHelper != null) {
                        Bukkit.getLogger().log(Level.INFO, "PartyHelper found.");
                        Party party = partyHelper.getParty(killer.getUniqueId());
                        if (party != null) {
                            Bukkit.getLogger().log(Level.INFO, "Party found for killer. Party members:");
                            // Get online members
                            for (UUID memberUUID : party.getMembers()) {
                                Player member = Bukkit.getPlayer(memberUUID);
                                if (member != null && member.isOnline()) {
                                    Bukkit.getLogger().log(Level.INFO, "- " + member.getName());
                                    partyMembers.add(member);
                                } else {
                                    Bukkit.getLogger().log(Level.INFO, "- Member UUID " + memberUUID + " is offline or not found.");
                                }
                            }
                        } else {
                            Bukkit.getLogger().log(Level.INFO, "Killer is not in a party.");
                            // Not in a party, only the killer
                            partyMembers.add(killer);
                        }
                    } else {
                        Bukkit.getLogger().log(Level.INFO, "PartyHelper not available.");
                        // PartyHelper not available
                        partyMembers.add(killer);
                    }

                    int partySize = partyMembers.size();
                    Bukkit.getLogger().log(Level.INFO, "Party size: " + partySize);

                    // Cap party size at 3
                    if (partySize > 3) {
                        partySize = 3;
                        partyMembers = partyMembers.subList(0, 3);
                    }

                    int xpPerPlayer = experienceReward / partySize;
                    Bukkit.getLogger().log(Level.INFO, "XP per player: " + xpPerPlayer);

                    for (Player member : partyMembers) {
                        Bukkit.getLogger().log(Level.INFO, "Adding XP to member: " + member.getName());
                        levelModule.addXp(member, xpPerPlayer);
                        // Show XP indicator at the mob's death location

                        member.sendMessage(Utils.getInstance().$("You received " + xpPerPlayer + " XP from killing " + mobInstance.getCustomMob().getName() + "."));
                    }
                } else {
                    killer.sendMessage(Utils.getInstance().$("Unable to add XP. LevelModule is not available."));
                    Bukkit.getLogger().severe("LevelModule is not available. Cannot add XP to player.");
                }

                // Currency reward (only the killer gets this)
                double currencyReward = mobInstance.getCustomMob().getCurrencyReward();

                // Define the chance for currency reward (100% for testing)
                double currencyChance = 1.0; // 1.0 represents 100%

                if (random.nextDouble() <= currencyChance) {
                    if (economyHelper != null) {
                        economyHelper.addBalance(killer, currencyReward);
                        killer.sendMessage(Utils.getInstance().$("You have received $" + currencyReward + " for killing " + mobInstance.getCustomMob().getName() + "!"));
                    } else {
                        killer.sendMessage(Utils.getInstance().$("Economy system is not available. Cannot reward currency."));
                    }
                }

                // Update quest progress (only for the killer)
                QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
                if (questModule != null) {
                    questModule.updateQuestProgressOnMobDeath(killer, mobInstance.getCustomMob());
                } else {
                    Bukkit.getLogger().warning("QuestModule is not available. Cannot update quest progress.");
                }
            } else {
                Bukkit.getLogger().log(Level.WARNING, "Killer is null or offline.");
            }
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Mob died but no killer was found.");
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
