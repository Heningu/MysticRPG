package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.mobs.actions.Action;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.customs.mobs.actions.Condition;
import eu.xaru.mysticrpg.customs.mobs.actions.steps.DelayActionStep;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.party.Party;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
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
            DebugLogger.getInstance().warning("PartyModule is not available. Party features will be disabled.");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Schedule tasks
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobAnimations, 0L, 5L); // Runs every 5 ticks
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkCombatStatus, 0L, 20L); // Runs every second
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, 0L, 10L); // Adjust interval as needed
    }

    public CustomMobInstance spawnMobAtLocation(CustomMob customMob, Location location) {
        if (location.getWorld() == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Cannot spawn mob '" + customMob + "' because location world is null.", 0);
            return null;
        }

        if (customMob == null) {
            DebugLogger.getInstance().log(Level.WARNING, "CustomMob is null.");
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

        // Apply the model using ModelHandler
        String modelId = customMob.getModelId();
        ModeledEntity modeledEntity = null;

        if (modelId != null && !modelId.isEmpty()) {
            modeledEntity = ModelHandler.applyModel(mob, modelId);

            // Hide the base entity
            if (modeledEntity != null) {
                modeledEntity.setBaseEntityVisible(false);
            }
        } else {
            // Ensure the base entity is visible if no model is applied
            mob.setInvisible(false);
        }

        CustomMobInstance mobInstance = new CustomMobInstance(customMob, location, mob, modeledEntity);
        activeMobs.put(mob.getUniqueId(), mobInstance);

        // Trigger onSpawn actions
        executeActions(mobInstance, ActionTriggers.ON_SPAWN);

        DebugLogger.getInstance().log(Level.INFO, "Spawned mob: " + customMob.getName() + " at location: " + location);

        return mobInstance;
    }

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

        // Disable default attack damage
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(0.0); // Set attack damage to zero
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

    private String createMobNameTag(CustomMob customMob, double currentHealth) {
        return Utils.getInstance().$(String.format("[LVL%d] %s [%.1f❤]", customMob.getLevel(), customMob.getName(), currentHealth));
    }

    public Map<String, CustomMob> getMobConfigurations() {
        return mobConfigurations;
    }

    public CustomMobInstance findMobInstance(LivingEntity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        CustomMobInstance mobInstance = findMobInstance(livingEntity);
        if (mobInstance == null) return; // Not a custom mob

        CustomMob customMob = mobInstance.getCustomMob();

        // Capture the damage before modifying it
        double damage = event.getFinalDamage();

        // Record the last damager and set target if applicable
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager instanceof Player playerDamager) {
                mobInstance.setLastDamager(playerDamager.getUniqueId());
                mobInstance.setTarget(playerDamager);
                mobInstance.setInCombat(true);
                DebugLogger.getInstance().log("Mob " + mobInstance.getCustomMob().getName() + " was damaged by player " + playerDamager.getName());
            } else if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player playerShooter) {
                    mobInstance.setLastDamager(playerShooter.getUniqueId());
                    mobInstance.setTarget(playerShooter);
                    mobInstance.setInCombat(true);
                    DebugLogger.getInstance().log("Mob " + mobInstance.getCustomMob().getName() + " was shot by player " + playerShooter.getName());
                }
            }
        }

        // Reduce custom HP
        double currentHp = mobInstance.getCurrentHp() - damage;
        mobInstance.setCurrentHp(currentHp);

        // Update name tag
        livingEntity.setCustomName(createMobNameTag(customMob, currentHp));

        // Trigger onDamaged actions
        executeActions(mobInstance, ActionTriggers.ON_DAMAGED);

        // Update boss bar
        if (mobInstance.getBossBarHandler() != null) {
            mobInstance.getBossBarHandler().updateBossBar();
        }

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

        // Play hurt animation if applicable
        String modelId = mobInstance.getCustomMob().getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            ModelHandler.playAnimation(livingEntity, modelId, "hurt", 0.0, 0.0, 1.0, false);
        }
    }

    public void handleMobDeath(CustomMobInstance mobInstance) {
        // Log mob death
        DebugLogger.getInstance().log(Level.INFO, "Mob died: " + mobInstance.getCustomMob().getName());

        UUID killerUUID = mobInstance.getLastDamager();
        DebugLogger.getInstance().log(Level.INFO, "Mob's last damager UUID: " + killerUUID);

        if (killerUUID != null) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && killer.isOnline()) {
                DebugLogger.getInstance().log(Level.INFO, "Killer found: " + killer.getName());
                int experienceReward = mobInstance.getCustomMob().getExperienceReward();
                DebugLogger.getInstance().log(Level.INFO, "Experience Reward: " + experienceReward);

                // Retrieve LevelModule instance when needed
                LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
                if (levelModule != null) {
                    DebugLogger.getInstance().log(Level.INFO, "LevelModule found.");

                    // Get the party members
                    List<Player> partyMembers = new ArrayList<>();

                    if (partyHelper != null) {
                        DebugLogger.getInstance().log(Level.INFO, "PartyHelper found.");
                        Party party = partyHelper.getParty(killer.getUniqueId());
                        if (party != null) {
                            DebugLogger.getInstance().log(Level.INFO, "Party found for killer. Party members:");
                            // Get online members
                            for (UUID memberUUID : party.getMembers()) {
                                Player member = Bukkit.getPlayer(memberUUID);
                                if (member != null && member.isOnline()) {
                                    DebugLogger.getInstance().log(Level.INFO, "- " + member.getName());
                                    partyMembers.add(member);
                                } else {
                                    DebugLogger.getInstance().log(Level.INFO, "- Member UUID " + memberUUID + " is offline or not found.");
                                }
                            }
                        } else {
                            DebugLogger.getInstance().log(Level.INFO, "Killer is not in a party.");
                            // Not in a party, only the killer
                            partyMembers.add(killer);
                        }
                    } else {
                        DebugLogger.getInstance().log(Level.INFO, "PartyHelper not available.");
                        // PartyHelper not available
                        partyMembers.add(killer);
                    }

                    int partySize = partyMembers.size();
                    DebugLogger.getInstance().log(Level.INFO, "Party size: " + partySize);

                    // Cap party size at 3
                    if (partySize > 3) {
                        partySize = 3;
                        partyMembers = partyMembers.subList(0, 3);
                    }

                    int xpPerPlayer = experienceReward / partySize;
                    DebugLogger.getInstance().log(Level.INFO, "XP per player: " + xpPerPlayer);

                    for (Player member : partyMembers) {
                        DebugLogger.getInstance().log(Level.INFO, "Adding XP to member: " + member.getName());
                        levelModule.addXp(member, xpPerPlayer);
                        // Show XP indicator at the mob's death location

                        member.sendMessage(Utils.getInstance().$("You received " + xpPerPlayer + " XP from killing " + mobInstance.getCustomMob().getName() + "."));
                    }
                } else {
                    killer.sendMessage(Utils.getInstance().$("Unable to add XP. LevelModule is not available."));
                    DebugLogger.getInstance().severe("LevelModule is not available. Cannot add XP to player.");
                }

                // Currency reward (only the killer gets this)
                int currencyReward = mobInstance.getCustomMob().getCurrencyReward();

                // Define the chance for currency reward (100% for testing)
                double currencyChance = 1.0; // 1.0 represents 100%

                if (random.nextDouble() <= currencyChance) {
                    if (economyHelper != null) {
                        economyHelper.addHeldGold(killer, currencyReward);
                        killer.sendMessage(Utils.getInstance().$("You have received $" + currencyReward + " for killing " + mobInstance.getCustomMob().getName() + "!"));
                    } else {
                        killer.sendMessage(Utils.getInstance().$("Economy system is not available. Cannot reward currency."));
                    }
                }

                // Update quest progress (only for the killer)
                QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
                if (questModule != null) {
                   // questModule.updateQuestProgressOnMobDeath(killer, mobInstance.getCustomMob());
                } else {
                    DebugLogger.getInstance().warning("QuestModule is not available. Cannot update quest progress.");
                }
            } else {
                DebugLogger.getInstance().log(Level.WARNING, "Killer is null or offline.");
            }
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "Mob died but no killer was found.");
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

        // Remove boss bar if present
        if (mobInstance.getBossBarHandler() != null) {
            mobInstance.getBossBarHandler().removeAllPlayers();
        }

        // Destroy the modeled entity if it exists
        if (mobInstance.getModeledEntity() != null) {
            mobInstance.getModeledEntity().destroy();
        }
    }

    private void executeActions(CustomMobInstance mobInstance, String trigger) {
        List<Action> actions = mobInstance.getCustomMob().getActions().get(trigger);
        if (actions != null) {
            for (Action action : actions) {
                long currentTime = System.currentTimeMillis();
                long cooldownMillis = (long) (action.getCooldown() * 1000);
                if (currentTime - action.getLastExecutionTime() >= cooldownMillis) {
                    // Check if mob is already performing an action
                    if (mobInstance.isPerformingAction()) {
                        DebugLogger.getInstance().log("Mob is already performing an action, skipping.");
                        continue;
                    }
                    // Check target conditions
                    boolean conditionsMet = true;
                    for (Condition condition : action.getTargetConditions()) {
                        boolean result = condition.evaluate(mobInstance.getEntity(), mobInstance.getTarget());
                        DebugLogger.getInstance().log("Evaluating condition: " + condition + " Result: " + result);
                        if (!result) {
                            conditionsMet = false;
                            break;
                        }
                    }
                    if (conditionsMet) {
                        DebugLogger.getInstance().log("Conditions met for action on trigger " + trigger);
                        action.setLastExecutionTime(currentTime);
                        mobInstance.setPerformingAction(true);
                        executeActionSteps(mobInstance, action.getSteps(), 0);
                    } else {
                        DebugLogger.getInstance().log("Conditions not met for action on trigger " + trigger);
                    }
                } else {
                    DebugLogger.getInstance().log("Action on trigger " + trigger + " is on cooldown.");
                }
            }
        } else {
            DebugLogger.getInstance().log("No actions found for trigger " + trigger);
        }
    }

    private void executeActionSteps(CustomMobInstance mobInstance, List<ActionStep> steps, int index) {
        if (index >= steps.size()) {
            // Action steps completed
            mobInstance.setPerformingAction(false);
            return;
        }

        ActionStep step = steps.get(index);

        if (step instanceof DelayActionStep) {
            DelayActionStep delayStep = (DelayActionStep) step;
            long delayTicks = (long) (delayStep.getDelaySeconds() * 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeActionSteps(mobInstance, steps, index + 1);
            }, delayTicks);
        } else {
            step.execute(mobInstance);
            executeActionSteps(mobInstance, steps, index + 1);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity livingEntity)) return;

        CustomMobInstance mobInstance = findMobInstance(livingEntity);
        if (mobInstance == null) return;

        // If mob is performing an action, cancel the event
        if (mobInstance.isPerformingAction()) {
            event.setCancelled(true);
            return;
        }

        // Cancel the event to prevent default damage and knockback
        event.setCancelled(true);

        DebugLogger.getInstance().log("Mob " + mobInstance.getCustomMob().getName() + " attempted to attack " + event.getEntity().getName());

        // Only set target if the entity is a valid target
        if (event.getEntity() instanceof LivingEntity targetEntity && !targetEntity.equals(mobInstance.getEntity())) {
            mobInstance.setTarget(targetEntity);
            mobInstance.setInCombat(true);
            executeActions(mobInstance, ActionTriggers.ON_ATTACK);
        } else {
            DebugLogger.getInstance().warning("Invalid attack target. Skipping action.");
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        CustomMobInstance mobInstance = findMobInstance(livingEntity);
        if (mobInstance == null) return;

        if (event.getTarget() != null && event.getReason() == EntityTargetEvent.TargetReason.CLOSEST_PLAYER) {
            if (!mobInstance.isInCombat()) {
                mobInstance.setInCombat(true);
                executeActions(mobInstance, ActionTriggers.ON_ENTER_COMBAT);
            }
            mobInstance.setTarget(event.getTarget());
        } else if (event.getTarget() == null) {
            mobInstance.setTarget(null);
            mobInstance.setInCombat(false);
            executeActions(mobInstance, ActionTriggers.ON_LEAVE_COMBAT);
        }
    }

    private void checkCombatStatus() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            boolean hasNearbyPlayer = hasNearbyPlayer(mobInstance);
            if (mobInstance.isInCombat() && !hasNearbyPlayer) {
                mobInstance.setInCombat(false);
                mobInstance.setTarget(null);
                executeActions(mobInstance, ActionTriggers.ON_LEAVE_COMBAT);
                DebugLogger.getInstance().log("Mob " + mobInstance.getCustomMob().getName() + " has left combat.");
            }
        }
    }

    private boolean hasNearbyPlayer(CustomMobInstance mobInstance) {
        double detectionRange = 20.0; // Adjust this range as needed
        for (Player player : mobInstance.getEntity().getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(mobInstance.getEntity().getLocation()) <= detectionRange * detectionRange) {
                return true;
            }
        }
        return false;
    }

    private void updateMobAnimations() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            updateMobAnimation(mobInstance);
        }
    }

    public void updateMobAnimation(CustomMobInstance mobInstance) {
        LivingEntity entity = mobInstance.getEntity();
        if (entity.isDead()) return;

        String animationName = null;

        // Check if mob is moving
        boolean isMoving = entity.getVelocity().lengthSquared() > 0.02;

        // Get AnimationConfig
        AnimationConfig animationConfig = mobInstance.getCustomMob().getAnimationConfig();

        if (mobInstance.isPerformingAction()) {
            // If performing an action, keep the current animation
            DebugLogger.getInstance().log("Mob is performing an action. Skipping animation update.");
            return;
        }

        // Use the standard idle and walk animations
        if (isMoving) {
            animationName = animationConfig.getWalkAnimation();
        } else {
            animationName = animationConfig.getIdleAnimation();
        }

        DebugLogger.getInstance().log("Determined animation: " + animationName + " for mob: " + mobInstance.getCustomMob().getName());

        // Play the animation if not already playing
        if (animationName != null && !animationName.equals(mobInstance.getCurrentAnimation())) {
            DebugLogger.getInstance().log("Playing animation: " + animationName + " for mob: " + mobInstance.getCustomMob().getName());
            mobInstance.setCurrentAnimation(animationName);
            String modelId = mobInstance.getCustomMob().getModelId();
            if (modelId != null && !modelId.isEmpty()) {
                ModelHandler.playAnimation(entity, modelId, animationName, 0.0, 0.0, 1.0, true);
            }
        } else {
            DebugLogger.getInstance().log("Animation '" + animationName + "' is already playing for mob: " + mobInstance.getCustomMob().getName());
        }
    }

    private void updateBossBars() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            MobBossBarHandler bossBarHandler = mobInstance.getBossBarHandler();
            if (bossBarHandler != null) {
                bossBarHandler.updateBossBar();

                // Update players in range
                double rangeSquared = bossBarHandler.getRange() * bossBarHandler.getRange();
                Set<Player> playersInRange = new HashSet<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(mobInstance.getEntity().getWorld())) {
                        if (player.getLocation().distanceSquared(mobInstance.getEntity().getLocation()) <= rangeSquared) {
                            playersInRange.add(player);
                        }
                    }
                }

                // Add new players
                for (Player player : playersInRange) {
                    if (!bossBarHandler.getPlayersInRange().contains(player)) {
                        bossBarHandler.addPlayer(player);
                    }
                }

                // Remove players who are no longer in range
                for (Player player : new HashSet<>(bossBarHandler.getPlayersInRange())) {
                    if (!playersInRange.contains(player)) {
                        bossBarHandler.removePlayer(player);
                    }
                }
            }
        }
    }

    public static class ActionTriggers {
        public static final String ON_ATTACK = "onAttack";
        public static final String ON_SPAWN = "onSpawn";
        public static final String ON_ENTER_COMBAT = "onEnterCombat";
        public static final String ON_LEAVE_COMBAT = "onLeaveCombat";
        public static final String ON_DAMAGED = "onDamaged";
    }
}