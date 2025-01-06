// File: eu/xaru/mysticrpg/customs/mobs/MobManager.java

package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.items.effects.Effect;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStone;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneModule;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.DungeonModule;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.party.Party;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
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
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import java.util.Random;

/**
 * Manages spawning, tracking, and custom logic for all active custom mobs.
 */
public class MobManager implements Listener {

    private final JavaPlugin plugin;
    /**
     * This map holds all loaded custom mob configurations (from .yml).
     */
    private final Map<String, CustomMob> mobConfigurations;

    /**
     * Tracks each active mob in the world by its entity UUID -> CustomMobInstance
     */
    private final Map<UUID, CustomMobInstance> activeMobs = new HashMap<>();

    private final Random random = new Random();
    private final ItemManager itemManager;
    private final EconomyHelper economyHelper;
    private final PartyHelper partyHelper;

    /**
     * Constructs a new MobManager.
     *
     * @param plugin           The main plugin instance
     * @param mobConfigurations The map of all loaded custom mob definitions
     * @param economyHelper    For awarding currency
     */
    public MobManager(JavaPlugin plugin,
                      Map<String, CustomMob> mobConfigurations,
                      EconomyHelper economyHelper) {

        this.plugin = plugin;
        this.mobConfigurations = mobConfigurations;  // <--- We store the reference here
        this.economyHelper = economyHelper;

        // Grab the ItemManager from the CustomItemModule
        this.itemManager = ModuleManager.getInstance()
                .getModuleInstance(CustomItemModule.class)
                .getItemManager();

        // For party logic
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule != null) {
            this.partyHelper = partyModule.getPartyHelper();
        } else {
            this.partyHelper = null;
            DebugLogger.getInstance().warning("PartyModule is not available. Party features disabled.");
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Repeating tasks for animations, checks, etc.
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkCombatStatus, 0L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, 0L, 10L);
    }

    /**
     * Exposes the loaded custom mob definitions.
     * This is what your GUI code calls to list all the known mob IDs.
     */
    public Map<String, CustomMob> getMobConfigurations() {
        return mobConfigurations;
    }

    /**
     * Spawns the given CustomMob in the world at the specified location,
     * creates a CustomMobInstance, and tracks it in activeMobs.
     */
    public CustomMobInstance spawnMobAtLocation(CustomMob customMob, Location location) {
        if (location.getWorld() == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Cannot spawn mob '" + customMob + "' because world is null at location " + location, 0);
            return null;
        }
        if (customMob == null) {
            DebugLogger.getInstance().log(Level.WARNING,
                    "CustomMob is null. Aborting spawn at " + location, 0);
            return null;
        }

        // Spawn the actual LivingEntity
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, customMob.getEntityType());

        // Adjust base attributes like speed, armor, etc
        applyCustomAttributes(mob, customMob);

        // Name tag & health
        mob.setCustomName(createMobNameTag(customMob, customMob.getHealth()));
        mob.setCustomNameVisible(true);

        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customMob.getHealth());
        }
        mob.setHealth(customMob.getHealth());

        // If there's a ModelEngine model ID
        String modelId = customMob.getModelId();
        ModeledEntity modeledEntity = null;
        if (modelId != null && !modelId.isEmpty()) {
            modeledEntity = ModelHandler.applyModel(mob, modelId);
            if (modeledEntity != null) {
                // Hide the base entity
                modeledEntity.setBaseEntityVisible(false);
            }
        } else {
            // no model ID -> just a normal visible entity
            mob.setInvisible(false);
        }

        // Create an instance to track
        CustomMobInstance mobInstance =
                new CustomMobInstance(customMob, location, mob, modeledEntity);

        // Put in active map
        activeMobs.put(mob.getUniqueId(), mobInstance);

        // Optionally run spawn-trigger actions (if you keep those)
        // executeActions(mobInstance, ActionTriggers.ON_SPAWN);

        DebugLogger.getInstance().log(Level.INFO,
                "Spawned mob: " + customMob.getName() + " at " + location, 0);

        return mobInstance;
    }

    private void applyCustomAttributes(LivingEntity mob, CustomMob customMob) {
        AttributeInstance speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(customMob.getMovementSpeed());
        }
        AttributeInstance armorAttr = mob.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(customMob.getBaseArmor());
        }
        AttributeInstance damageAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            // We might keep base at 0 and handle damage logic ourselves
            damageAttr.setBaseValue(0.0);
        }

        // Equipment if needed
        EntityEquipment eq = mob.getEquipment();
        if (eq != null && customMob.getEquipment() != null) {
            if (customMob.getEquipment().getHelmet() != null) {
                eq.setHelmet(getCustomItemStack(customMob.getEquipment().getHelmet()));
            }
            if (customMob.getEquipment().getChestplate() != null) {
                eq.setChestplate(getCustomItemStack(customMob.getEquipment().getChestplate()));
            }
            if (customMob.getEquipment().getLeggings() != null) {
                eq.setLeggings(getCustomItemStack(customMob.getEquipment().getLeggings()));
            }
            if (customMob.getEquipment().getBoots() != null) {
                eq.setBoots(getCustomItemStack(customMob.getEquipment().getBoots()));
            }
            if (customMob.getEquipment().getWeapon() != null) {
                eq.setItemInMainHand(getCustomItemStack(customMob.getEquipment().getWeapon()));
            }
        }
    }

    private ItemStack getCustomItemStack(String customItemId) {
        if (customItemId == null) return null;
        CustomItem customItem = itemManager.getCustomItem(customItemId);
        if (customItem != null) {
            return customItem.toItemStack();
        }
        return null;
    }

    private String createMobNameTag(CustomMob mob, double hp) {
        return Utils.getInstance().$(String.format("[LVL%d] %s [%.1f❤]",
                mob.getLevel(), mob.getName(), hp));
    }

    /**
     * Locate the CustomMobInstance from a living entity, if any.
     */
    public CustomMobInstance findMobInstance(LivingEntity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    /**
     * Called when the mob takes damage (EntityDamageEvent).
     * We do custom HP handling, remove red damage effect, etc.
     */
    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        // See if it's one of our custom mobs
        CustomMobInstance mobInstance = findMobInstance(livingEntity);
        if (mobInstance == null) return; // not ours

        double damage = event.getFinalDamage();

        // Identify the attacker if it's a Player or projectile
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager instanceof Player p) {
                mobInstance.setLastDamager(p.getUniqueId());
                mobInstance.setTarget(p);
                mobInstance.setInCombat(true);
            } else if (damager instanceof Projectile proj) {
                ProjectileSource shooter = proj.getShooter();
                if (shooter instanceof Player p2) {
                    mobInstance.setLastDamager(p2.getUniqueId());
                    mobInstance.setTarget(p2);
                    mobInstance.setInCombat(true);
                }
            }
        }

        // Decrease our tracked HP
        double newHp = mobInstance.getCurrentHp() - damage;
        mobInstance.setCurrentHp(newHp);

        // Update the name tag with new HP
        livingEntity.setCustomName(createMobNameTag(mobInstance.getCustomMob(), newHp));

        // If it has a boss bar, update it
        if (mobInstance.getBossBarHandler() != null) {
            mobInstance.getBossBarHandler().updateBossBar();
        }

        // If lethal
        if (newHp <= 0) {
            // Remove from active map
            activeMobs.remove(livingEntity.getUniqueId());
            handleMobDeath(mobInstance);
            // remove entity
            livingEntity.remove();
        }

        // Cancel normal damage so we don't do the red flash
        event.setDamage(0);

        // If a player physically attacked, maybe do small knockback
        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player attacker) {
                Vector knockback = livingEntity.getLocation().toVector()
                        .subtract(attacker.getLocation().toVector()).normalize().multiply(0.4);
                livingEntity.setVelocity(knockback);
            }
        }
    }

    /**
     * Handles awarding XP/gold to the killer, dropping items, etc.
     */
    public void handleMobDeath(CustomMobInstance mobInstance) {
        DebugLogger.getInstance().log(Level.INFO,
                "Mob died: " + mobInstance.getCustomMob().getName());

        UUID killerUUID = mobInstance.getLastDamager();
        if (killerUUID != null) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && killer.isOnline()) {
                // If in a dungeon, increment kill count, etc
                DungeonModule dModule = ModuleManager.getInstance().getModuleInstance(DungeonModule.class);
                if (dModule != null) {
                    DungeonManager dMan = dModule.getDungeonManager();
                    DungeonInstance instance = dMan.getInstanceByPlayer(killer.getUniqueId());
                    if (instance != null) {
                        instance.incrementMonsterKill(killer);
                    }
                }

                // Reward XP/gold
                int xp = mobInstance.getCustomMob().getExperienceReward();
                int gold = mobInstance.getCustomMob().getCurrencyReward();

                LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
                if (levelModule != null) {
                    // Possibly handle party splitting
                    List<Player> partyMembers = getPartyMembers(killer);

                    int partySize = partyMembers.size();
                    if (partySize > 3) {
                        partySize = 3;
                        partyMembers = partyMembers.subList(0, 3);
                    }
                    int xpPerPlayer = (partySize > 0) ? xp / partySize : xp;

                    for (Player member : partyMembers) {
                        int baseXp = xpPerPlayer;
                        int baseGold = (member.equals(killer)) ? gold : 0;

                        int[] finalRewards = applyEffectsToRewards(member, baseXp, baseGold);
                        int finalXp = finalRewards[0];
                        int finalGold = finalRewards[1];

                        // add xp
                        levelModule.addXp(member, finalXp);

                        member.sendMessage(Utils.getInstance().$(
                                "You received " + finalXp + " XP from killing " +
                                        mobInstance.getCustomMob().getName() + "."));

                        // only the actual killer gets gold
                        if (member.equals(killer) && finalGold > 0 && economyHelper != null) {
                            killer.sendMessage(Utils.getInstance().$(
                                    "You received " + finalGold + " gold from killing " +
                                            mobInstance.getCustomMob().getName() + "."));
                            economyHelper.addHeldGold(killer, finalGold);
                        }
                    }
                }

                // If using quests
                QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
                if (questModule != null) {
                    // questModule.updateQuestProgressOnMobDeath(killer, mobInstance.getCustomMob());
                }
            }
        }

        // Drop items
        for (CustomMob.DropItem dropItem : mobInstance.getCustomMob().getDrops()) {
            if (random.nextDouble() <= dropItem.getChance()) {
                ItemStack drop = null;
                if ("custom_item".equalsIgnoreCase(dropItem.getType())) {
                    CustomItem cItem = itemManager.getCustomItem(dropItem.getId());
                    if (cItem != null) {
                        drop = cItem.toItemStack();
                    }
                } else if ("material".equalsIgnoreCase(dropItem.getType())) {
                    Material mat = Material.matchMaterial(dropItem.getMaterial().toUpperCase());
                    if (mat != null) {
                        drop = new ItemStack(mat, dropItem.getAmount());
                    }
                }
                if (drop != null) {
                    mobInstance.getEntity().getWorld().dropItemNaturally(
                            mobInstance.getEntity().getLocation(), drop);
                }
            }
        }

        // Cleanup bossbar
        if (mobInstance.getBossBarHandler() != null) {
            mobInstance.getBossBarHandler().removeAllPlayers();
        }

        // Destroy model
        if (mobInstance.getModeledEntity() != null) {
            mobInstance.getModeledEntity().destroy();
        }
    }

    /**
     * If in a party, returns all party members. Otherwise returns [killer].
     */
    private List<Player> getPartyMembers(Player killer) {
        if (partyHelper == null) {
            return List.of(killer);
        }
        Party party = partyHelper.getParty(killer.getUniqueId());
        if (party == null) {
            return List.of(killer);
        }
        List<Player> result = new ArrayList<>();
        for (UUID m : party.getMembers()) {
            Player pm = Bukkit.getPlayer(m);
            if (pm != null && pm.isOnline()) {
                result.add(pm);
            }
        }
        return result;
    }

    /**
     * Applies item-based multipliers for XP or gold if the player has e.g. PowerStones.
     */
    private int[] applyEffectsToRewards(Player player, int baseXp, int baseGold) {
        int finalXp = applyXpEffects(player, baseXp);
        int finalGold = applyGoldEffects(player, baseGold);
        return new int[]{finalXp, finalGold};
    }

    private int applyXpEffects(Player p, int baseXp) {
        int finalXp = baseXp;
        ItemStack wep = p.getInventory().getItemInMainHand();
        if (wep != null && wep.hasItemMeta()) {
            ItemMeta meta = wep.getItemMeta();
            if (meta != null) {
                NamespacedKey key = new NamespacedKey(plugin, "applied_power_stones");
                String stoneStr = meta.getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                if (stoneStr != null && !stoneStr.isEmpty()) {
                    PowerStoneManager psm = ModuleManager.getInstance()
                            .getModuleInstance(PowerStoneModule.class)
                            .getPowerStoneManager();
                    Set<String> stoneIds = new HashSet<>(Arrays.asList(stoneStr.split(",")));
                    for (String sId : stoneIds) {
                        PowerStone ps = psm.getPowerStone(sId);
                        if (ps != null) {
                            Effect eff = psm.getEffect(ps.getEffect());
                            if (eff != null) {
                                double mult = eff.getXpMultiplier();
                                if (mult > 1.0) {
                                    int old = finalXp;
                                    finalXp = (int) (finalXp * mult);
                                    int added = finalXp - old;
                                    if (added > 0) {
                                        p.sendMessage(Utils.getInstance().$(
                                                "Effect " + eff.getName() +
                                                        " granted you +" + added + " XP."));
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

    private int applyGoldEffects(Player p, int baseGold) {
        int finalGold = baseGold;
        ItemStack wep = p.getInventory().getItemInMainHand();
        if (wep != null && wep.hasItemMeta()) {
            ItemMeta meta = wep.getItemMeta();
            if (meta != null) {
                NamespacedKey key = new NamespacedKey(plugin, "applied_power_stones");
                String stoneStr = meta.getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                if (stoneStr != null && !stoneStr.isEmpty()) {
                    PowerStoneManager psm = ModuleManager.getInstance()
                            .getModuleInstance(PowerStoneModule.class)
                            .getPowerStoneManager();
                    Set<String> stoneIds = new HashSet<>(Arrays.asList(stoneStr.split(",")));
                    for (String sId : stoneIds) {
                        PowerStone ps = psm.getPowerStone(sId);
                        if (ps != null) {
                            Effect eff = psm.getEffect(ps.getEffect());
                            if (eff != null) {
                                double mult = eff.getGoldMultiplier();
                                if (mult > 1.0) {
                                    int old = finalGold;
                                    finalGold = (int) (finalGold * mult);
                                    int added = finalGold - old;
                                    if (added > 0) {
                                        p.sendMessage(Utils.getInstance().$(
                                                "Effect " + eff.getName() +
                                                        " granted you +" + added + " gold."));
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

    /**
     * Periodically checks if mobs are in combat range or not.
     */
    private void checkCombatStatus() {
        for (CustomMobInstance mobInst : activeMobs.values()) {
            boolean hasNearbyPlayer = hasNearbyPlayer(mobInst);
            if (mobInst.isInCombat() && !hasNearbyPlayer) {
                mobInst.setInCombat(false);
                mobInst.setTarget(null);
                DebugLogger.getInstance().log("Mob " + mobInst.getCustomMob().getName() + " left combat.");
            }
        }
    }

    private boolean hasNearbyPlayer(CustomMobInstance mobInst) {
        double rangeSq = 20.0 * 20.0;
        Location mobLoc = mobInst.getEntity().getLocation();
        for (Player pl : mobLoc.getWorld().getPlayers()) {
            if (pl.getLocation().distanceSquared(mobLoc) < rangeSq) {
                return true;
            }
        }
        return false;
    }

    /**
     * If using a bossbar, update its health % and which players can see it.
     */
    private void updateBossBars() {
        for (CustomMobInstance mobInst : activeMobs.values()) {
            MobBossBarHandler bbh = mobInst.getBossBarHandler();
            if (bbh != null) {
                bbh.updateBossBar();

                double rangeSq = bbh.getRange() * bbh.getRange();
                Location mobLoc = mobInst.getEntity().getLocation();

                Set<Player> inRangePlayers = new HashSet<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(mobLoc.getWorld())) {
                        if (p.getLocation().distanceSquared(mobLoc) <= rangeSq) {
                            inRangePlayers.add(p);
                        }
                    }
                }

                // Add any new players
                for (Player pl : inRangePlayers) {
                    if (!bbh.getPlayersInRange().contains(pl)) {
                        bbh.addPlayer(pl);
                    }
                }
                // Remove those who left range
                for (Player oldP : new HashSet<>(bbh.getPlayersInRange())) {
                    if (!inRangePlayers.contains(oldP)) {
                        bbh.removePlayer(oldP);
                    }
                }
            }
        }
    }
}
