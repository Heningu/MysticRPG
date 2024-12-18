package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.items.effects.Effect;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStone;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneModule;
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
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobAnimations, 0L, 5L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkCombatStatus, 0L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, 0L, 10L);
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

        // Set custom HP and name
        mob.setCustomName(createMobNameTag(customMob, customMob.getHealth()));
        mob.setCustomNameVisible(true);
        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customMob.getHealth());
        }
        mob.setHealth(customMob.getHealth());

        // Apply the model
        String modelId = customMob.getModelId();
        ModeledEntity modeledEntity = null;

        if (modelId != null && !modelId.isEmpty()) {
            modeledEntity = ModelHandler.applyModel(mob, modelId);
            // Hide the base entity
            if (modeledEntity != null) {
                modeledEntity.setBaseEntityVisible(false);
            }
        } else {
            // Ensure visible if no model
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
        // Movement speed
        AttributeInstance speedAttribute = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(customMob.getMovementSpeed());
        }

        // Armor
        AttributeInstance armorAttribute = mob.getAttribute(Attribute.ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(customMob.getBaseArmor());
        }

        // Attack damage to zero
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(0.0);
        }

        // Equipment
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null && customMob.getEquipment() != null) {
            if (customMob.getEquipment().getHelmet() != null) {
                equipment.setHelmet(getCustomItemStack(customMob.getEquipment().getHelmet()));
            }
            if (customMob.getEquipment().getChestplate() != null) {
                equipment.setChestplate(getCustomItemStack(customMob.getEquipment().getChestplate()));
            }
            if (customMob.getEquipment().getLeggings() != null) {
                equipment.setLeggings(getCustomItemStack(customMob.getEquipment().getLeggings()));
            }
            if (customMob.getEquipment().getBoots() != null) {
                equipment.setBoots(getCustomItemStack(customMob.getEquipment().getBoots()));
            }
            if (customMob.getEquipment().getWeapon() != null) {
                equipment.setItemInMainHand(getCustomItemStack(customMob.getEquipment().getWeapon()));
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
        if (mobInstance == null) return;

        CustomMob customMob = mobInstance.getCustomMob();

        double damage = event.getFinalDamage();

        // Record last damager
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager instanceof Player playerDamager) {
                mobInstance.setLastDamager(playerDamager.getUniqueId());
                mobInstance.setTarget(playerDamager);
                mobInstance.setInCombat(true);
            } else if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player playerShooter) {
                    mobInstance.setLastDamager(playerShooter.getUniqueId());
                    mobInstance.setTarget(playerShooter);
                    mobInstance.setInCombat(true);
                }
            }
        }

        double currentHp = mobInstance.getCurrentHp() - damage;
        mobInstance.setCurrentHp(currentHp);

        livingEntity.setCustomName(createMobNameTag(customMob, currentHp));

        // Trigger onDamaged actions
        executeActions(mobInstance, ActionTriggers.ON_DAMAGED);

        // Update boss bar
        if (mobInstance.getBossBarHandler() != null) {
            mobInstance.getBossBarHandler().updateBossBar();
        }

        // Check if mob is dead
        if (currentHp <= 0) {
            activeMobs.remove(livingEntity.getUniqueId());
            handleMobDeath(mobInstance);
            livingEntity.remove();
        }

        // Prevent default damage
        event.setDamage(0);

        // Knockback
        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player playerAttacker) {
                Vector direction = livingEntity.getLocation().toVector().subtract(playerAttacker.getLocation().toVector()).normalize();
                livingEntity.setVelocity(direction.multiply(0.4));
            }
        }

        // Hurt animation
        String modelId = mobInstance.getCustomMob().getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            ModelHandler.playAnimation(livingEntity, modelId, "hurt", 0.0, 0.0, 1.0, false);
        }
    }

    public void handleMobDeath(CustomMobInstance mobInstance) {
        DebugLogger.getInstance().log(Level.INFO, "Mob died: " + mobInstance.getCustomMob().getName());

        UUID killerUUID = mobInstance.getLastDamager();
        DebugLogger.getInstance().log(Level.INFO, "Mob's last damager UUID: " + killerUUID);

        if (killerUUID != null) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && killer.isOnline()) {
                DebugLogger.getInstance().log(Level.INFO, "Killer found: " + killer.getName());
                int baseXp = mobInstance.getCustomMob().getExperienceReward();
                int baseGold = mobInstance.getCustomMob().getCurrencyReward();

                LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
                if (levelModule != null) {
                    DebugLogger.getInstance().log(Level.INFO, "LevelModule found.");

                    List<Player> partyMembers = new ArrayList<>();

                    if (partyHelper != null) {
                        DebugLogger.getInstance().log(Level.INFO, "PartyHelper found.");
                        Party party = partyHelper.getParty(killer.getUniqueId());
                        if (party != null) {
                            DebugLogger.getInstance().log(Level.INFO, "Party found for killer. Party members:");
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
                            partyMembers.add(killer);
                        }
                    } else {
                        DebugLogger.getInstance().log(Level.INFO, "PartyHelper not available.");
                        partyMembers.add(killer);
                    }

                    int partySize = partyMembers.size();
                    DebugLogger.getInstance().log(Level.INFO, "Party size: " + partySize);

                    if (partySize > 3) {
                        partySize = 3;
                        partyMembers = partyMembers.subList(0, 3);
                    }

                    int xpPerPlayer = (partySize > 0) ? (baseXp / partySize) : baseXp;

                    for (Player member : partyMembers) {
                        DebugLogger.getInstance().log(Level.INFO, "Adding XP to member: " + member.getName());
                        int memberBaseXp = xpPerPlayer;
                        int memberBaseGold = 0;
                        if (member.equals(killer)) {
                            memberBaseGold = baseGold;
                        }

                        int[] finalRewards = applyEffectsToRewards(member, memberBaseXp, memberBaseGold);
                        int finalXp = finalRewards[0];
                        int finalGold = finalRewards[1];

                        levelModule.addXp(member, finalXp);
                        member.sendMessage(Utils.getInstance().$("You received " + finalXp + " XP from killing " + mobInstance.getCustomMob().getName() + "." +
                                (finalXp > memberBaseXp ? " (+" + (finalXp - memberBaseXp) + " XP from effects)" : "")));

                        if (member.equals(killer) && finalGold > 0 && economyHelper != null) {
                            killer.sendMessage(Utils.getInstance().$("You have received $" + finalGold + " for killing " + mobInstance.getCustomMob().getName() + "." +
                                    (finalGold > memberBaseGold ? " (+" + (finalGold - memberBaseGold) + " gold from effects)" : "")));
                            economyHelper.addHeldGold(killer, finalGold);
                        }
                    }
                } else {
                    killer.sendMessage(Utils.getInstance().$("Unable to add XP. LevelModule is not available."));
                    DebugLogger.getInstance().severe("LevelModule is not available. Cannot add XP to player.");
                }

                // Update quest progress (only for killer)
                QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
                if (questModule != null) {
                    // questModule.updateQuestProgressOnMobDeath(killer, mobInstance.getCustomMob());
                } else {
                    DebugLogger.getInstance().warning("QuestModule not available. Cannot update quest progress.");
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

        // Destroy the modeled entity if exists
        if (mobInstance.getModeledEntity() != null) {
            mobInstance.getModeledEntity().destroy();
        }
    }

    /**
     * Applies effects from the player's weapon to XP and Gold rewards.
     * Delegates to applyXpEffects and applyGoldEffects.
     */
    private int[] applyEffectsToRewards(Player player, int baseXp, int baseGold) {
        int finalXp = applyXpEffects(player, baseXp);
        int finalGold = applyGoldEffects(player, baseGold);
        return new int[]{finalXp, finalGold};
    }

    /**
     * Applies XP-related effects by retrieving xp multipliers from effects.
     * Also sends a message to the player if any effect increased XP.
     */
    private int applyXpEffects(Player player, int baseXp) {
        int finalXp = baseXp;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon != null && weapon.hasItemMeta()) {
            ItemMeta meta = weapon.getItemMeta();
            if (meta != null) {
                NamespacedKey key = new NamespacedKey(plugin, "applied_power_stones");
                String appliedStones = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (appliedStones != null && !appliedStones.isEmpty()) {
                    PowerStoneManager psm = ModuleManager.getInstance().getModuleInstance(PowerStoneModule.class).getPowerStoneManager();
                    Set<String> powerStoneIds = new HashSet<>(Arrays.asList(appliedStones.split(",")));
                    for (String psId : powerStoneIds) {
                        PowerStone ps = psm.getPowerStone(psId);
                        if (ps != null) {
                            Effect effect = psm.getEffect(ps.getEffect());
                            if (effect != null) {
                                double multiplier = effect.getXpMultiplier();
                                if (multiplier > 1.0) {
                                    int oldXp = finalXp;
                                    finalXp = (int)(finalXp * multiplier);
                                    int added = finalXp - oldXp;
                                    if (added > 0) {
                                        player.sendMessage(Utils.getInstance().$("Effect " + effect.getName() + " granted you +" + added + " XP."));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return finalXp;
    }

    /**
     * Applies Gold-related effects by retrieving gold multipliers from effects.
     * Also sends a message to the player if any effect increased Gold.
     */
    private int applyGoldEffects(Player player, int baseGold) {
        int finalGold = baseGold;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon != null && weapon.hasItemMeta()) {
            ItemMeta meta = weapon.getItemMeta();
            if (meta != null) {
                NamespacedKey key = new NamespacedKey(plugin, "applied_power_stones");
                String appliedStones = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (appliedStones != null && !appliedStones.isEmpty()) {
                    PowerStoneManager psm = ModuleManager.getInstance().getModuleInstance(PowerStoneModule.class).getPowerStoneManager();
                    Set<String> powerStoneIds = new HashSet<>(Arrays.asList(appliedStones.split(",")));
                    for (String psId : powerStoneIds) {
                        PowerStone ps = psm.getPowerStone(psId);
                        if (ps != null) {
                            Effect effect = psm.getEffect(ps.getEffect());
                            if (effect != null) {
                                double multiplier = effect.getGoldMultiplier();
                                if (multiplier > 1.0) {
                                    int oldGold = finalGold;
                                    finalGold = (int)(finalGold * multiplier);
                                    int added = finalGold - oldGold;
                                    if (added > 0) {
                                        player.sendMessage(Utils.getInstance().$("Effect " + effect.getName() + " granted you +" + added + " gold."));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return finalGold;
    }

    private void executeActions(CustomMobInstance mobInstance, String trigger) {
        List<Action> actions = mobInstance.getCustomMob().getActions().get(trigger);
        if (actions != null) {
            for (Action action : actions) {
                long currentTime = System.currentTimeMillis();
                long cooldownMillis = (long) (action.getCooldown() * 1000);
                if (currentTime - action.getLastExecutionTime() >= cooldownMillis) {
                    if (mobInstance.isPerformingAction()) {
                        DebugLogger.getInstance().log("Mob is already performing an action, skipping.");
                        continue;
                    }
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

        if (mobInstance.isPerformingAction()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        DebugLogger.getInstance().log("Mob " + mobInstance.getCustomMob().getName() + " attempted to attack " + event.getEntity().getName());

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
        double detectionRange = 20.0;
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
        boolean isMoving = entity.getVelocity().lengthSquared() > 0.02;
        AnimationConfig animationConfig = mobInstance.getCustomMob().getAnimationConfig();

        if (mobInstance.isPerformingAction()) {
            return;
        }

        if (isMoving) {
            animationName = animationConfig.getWalkAnimation();
        } else {
            animationName = animationConfig.getIdleAnimation();
        }

        if (animationName != null && !animationName.equals(mobInstance.getCurrentAnimation())) {
            mobInstance.setCurrentAnimation(animationName);
            String modelId = mobInstance.getCustomMob().getModelId();
            if (modelId != null && !modelId.isEmpty()) {
                ModelHandler.playAnimation(entity, modelId, animationName, 0.0, 0.0, 1.0, true);
            }
        }
    }

    private void updateBossBars() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            MobBossBarHandler bossBarHandler = mobInstance.getBossBarHandler();
            if (bossBarHandler != null) {
                bossBarHandler.updateBossBar();

                double rangeSquared = bossBarHandler.getRange() * bossBarHandler.getRange();
                Set<Player> playersInRange = new HashSet<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(mobInstance.getEntity().getWorld())) {
                        if (player.getLocation().distanceSquared(mobInstance.getEntity().getLocation()) <= rangeSquared) {
                            playersInRange.add(player);
                        }
                    }
                }

                for (Player player : playersInRange) {
                    if (!bossBarHandler.getPlayersInRange().contains(player)) {
                        bossBarHandler.addPlayer(player);
                    }
                }

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
