package eu.xaru.mysticrpg.player;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatCalculations;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.player.stats.events.PlayerStatsChangedEvent;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import eu.xaru.mysticrpg.world.WorldModule;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

/**
 * CustomDamageHandler manages custom damage calculations and integrates with stats and world flags.
 * It modifies incoming damage based on player stats and disallows PVP if disabled by world/region flags.
 */
public class CustomDamageHandler implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private PlayerStatsManager statsManager;
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private JavaPlugin plugin;
    private StatsModule statsModule;

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        if (saveModule != null) {
            playerDataCache = PlayerDataCache.getInstance();
        } else {
            DebugLogger.getInstance().error("SaveModule not initialized. CustomDamageHandler cannot function.");
            return;
        }

        if (playerDataCache == null) {
            DebugLogger.getInstance().error("PlayerDataCache not initialized. CustomDamageHandler cannot function.");
            return;
        }

        this.statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        if (statsModule == null) {
            DebugLogger.getInstance().error("StatsModule not found. CustomDamageHandler cannot function properly.");
            return;
        }

        this.statsManager = statsModule.getStatsManager();

        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler initialized", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler started", 0);

        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            if (!(event.getEntity() instanceof Player victim)) return;
            if (event.isCancelled()) return;

            // Check PVP flag
            Entity damager = event.getDamager();
            if (damager instanceof Player attacker) {
                WorldModule wm = ModuleManager.getInstance().getModuleInstance(WorldModule.class);
                if (wm != null && !wm.getWorldManager().isAllowed("pvp", victim.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }

            double rawDamage = event.getFinalDamage();
            double finalDamage = calculateDamage(damager, victim, rawDamage);
            applyDamageAndEffects(victim, finalDamage, damager);
            event.setCancelled(true);
        });

        eventManager.registerEvent(EntityDamageEvent.class, event -> {
            if (event instanceof EntityDamageByEntityEvent) return;
            if (!(event.getEntity() instanceof Player victim)) return;
            if (event.isCancelled()) return;

            EntityDamageEvent.DamageCause cause = event.getCause();
            switch (cause) {
                case FALL, FIRE, FIRE_TICK, LAVA, DROWNING, SUFFOCATION, VOID, LIGHTNING, HOT_FLOOR -> {
                    double finalDamage = event.getFinalDamage();
                    applyDamageAndEffects(victim, finalDamage, null);
                    event.setCancelled(true);
                }
                default -> event.setCancelled(true);
            }
        });

        // Health regeneration task
        new BukkitRunnable() {
            @Override
            public void run() {
                regenerateHealth();
            }
        }.runTaskTimer(plugin, 20, 20); // every second
    }

    /**
     * Applies custom damage to a player.
     *
     * @param player the player receiving damage
     * @param damage the amount of damage
     */
    public void applyCustomDamage(Player player, double damage) {
        applyDamageAndEffects(player, damage, null);
    }

    /**
     * Calculates final damage based on attacker and victim stats.
     *
     * @param damager    the entity dealing damage
     * @param victim     the player receiving damage
     * @param baseDamage initial damage
     * @return final calculated damage
     */
    private double calculateDamage(Entity damager, Player victim, double baseDamage) {
        PlayerStats victimStats = statsModule.recalculatePlayerStatsFor(victim);

        double finalDamage = baseDamage;
        if (damager instanceof Player attacker) {
            PlayerStats attackerStats = statsModule.recalculatePlayerStatsFor(attacker);
            double strength = attackerStats.getEffectiveStat(StatType.STRENGTH);
            double critChance = attackerStats.getEffectiveStat(StatType.CRIT_CHANCE);
            double critDamage = attackerStats.getEffectiveStat(StatType.CRIT_DAMAGE);

            finalDamage = StatCalculations.calculatePhysicalDamage(finalDamage, strength);
            finalDamage = StatCalculations.calculateCritDamage(finalDamage, critChance, critDamage);
        }

        double defense = victimStats.getEffectiveStat(StatType.DEFENSE);
        finalDamage = StatCalculations.calculateDamageTaken(finalDamage, defense);

        return finalDamage;
    }

    /**
     * Applies the final damage and effects to the victim. Handles death and hurt effects.
     *
     * @param victim  the victim player
     * @param damage  the calculated damage
     * @param damager the entity that caused the damage
     */
    private void applyDamageAndEffects(Player victim, double damage, Entity damager) {
        UUID victimUUID = victim.getUniqueId();
        PlayerData victimData = playerDataCache.getCachedPlayerData(victimUUID);
        if (victimData == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + victim.getName());
            return;
        }

        PlayerStats victimStats = statsModule.recalculatePlayerStatsFor(victim);
        int maxHp = (int) victimStats.getEffectiveStat(StatType.HEALTH);

        int currentHp = victimData.getCurrentHp();
        int dealtDamage = (int) Math.round(damage);
        currentHp -= dealtDamage;
        if (currentHp < 0) currentHp = 0;
        if (currentHp > maxHp) currentHp = maxHp;

        victimData.setCurrentHp(currentHp);

        DebugLogger.getInstance().log("Player " + victim.getName() + " took " + damage + " damage. Current HP: " + currentHp);

        if (currentHp <= 0) {
            handleDeath(victim);
        } else {
            triggerHurtEffects(victim, damager);
        }

        lastDamageTime.put(victimUUID, System.currentTimeMillis());

        Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(victim));
    }

    /**
     * Handles player death.
     *
     * @param player the player who died
     */
    private void handleDeath(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        PlayerStats stats = statsModule.recalculatePlayerStatsFor(player);
        int maxHp = (int) stats.getEffectiveStat(StatType.HEALTH);

        Location spawnLocation = player.getWorld().getSpawnLocation();
        player.teleport(spawnLocation);
        player.sendMessage(Utils.getInstance().$("You Died"));
        DebugLogger.getInstance().log("Player " + player.getName() + " died and was teleported to spawn.");

        data.setCurrentHp(maxHp);
        Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
    }

    /**
     * Triggers hurt effects such as sound, particles, and knockback.
     *
     * @param victim  the victim player
     * @param damager the entity that caused the damage
     */
    private void triggerHurtEffects(Player victim, Entity damager) {
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0,1,0), 10);

        if (damager != null && damager != victim) {
            Vector knockback = victim.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize().multiply(0.4);
            victim.setVelocity(knockback);
        }
    }

    /**
     * Regenerates health for players who haven't taken damage recently.
     */
    private void regenerateHealth() {
        Set<UUID> playerUUIDs = playerDataCache.getAllCachedPlayerUUIDs();
        for (UUID playerUUID : playerUUIDs) {
            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
            if (playerData == null) continue;

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) continue;

            PlayerStats stats = statsModule.recalculatePlayerStatsFor(player);
            int maxHp = (int) stats.getEffectiveStat(StatType.HEALTH);

            int currentHp = playerData.getCurrentHp();
            if (currentHp > maxHp) {
                currentHp = maxHp;
                playerData.setCurrentHp(currentHp);
            }

            if (currentHp < maxHp) {
                long lastDamage = lastDamageTime.getOrDefault(playerUUID, 0L);
                if (System.currentTimeMillis() - lastDamage >= 5000) {
                    double regenAmount = stats.getEffectiveStat(StatType.HEALTH_REGEN);
                    int newHp = currentHp + (int) Math.round(regenAmount);
                    if (newHp > maxHp) newHp = maxHp;
                    playerData.setCurrentHp(newHp);

                    DebugLogger.getInstance().log("Regenerated " + regenAmount + " HP for player " + player.getName() + ". Current HP: " + newHp);

                    Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
                }
            }
        }
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }
}
