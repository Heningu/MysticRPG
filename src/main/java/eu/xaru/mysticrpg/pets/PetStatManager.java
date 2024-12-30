package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages applying or removing stat buffs and effects
 * defined in a Pet's configuration.
 *
 * Example Pet config:
 * stats:
 *   HEALTH: 10
 *   STRENGTH: 5
 * effects:
 *   - FIERY
 */
public class PetStatManager {

    private static PlayerStatsManager statsManager;

    /**
     * Must be called once, e.g. in onEnable or a module init, after StatsModule is ready.
     */
    public static void init(PlayerStatsManager manager) {
        statsManager = manager;
    }

    /**
     * Applies the pet's stats & effects to the player.
     */
    public static void applyPetBonuses(Player player, Pet pet) {
        if (statsManager == null) {
            DebugLogger.getInstance().warning("PetStatManager not initialized with a PlayerStatsManager!");
            return;
        }
        Map<String, Object> configStats = pet.getAdditionalStats();
        if (configStats != null && !configStats.isEmpty()) {
            PlayerStats ps = statsManager.loadStats(player);
            for (Map.Entry<String, Object> entry : configStats.entrySet()) {
                StatType st = parseStatType(entry.getKey());
                double val = parseDouble(entry.getValue(), 0.0);
                if (st != null && val != 0.0) {
                    ps.addTempStat(st, val);
                    DebugLogger.getInstance().log(Level.INFO,
                            "Applied +" + val + " " + st.name() + " from pet " + pet.getName());
                }
            }
            statsManager.saveStats(player, ps);
        }

        List<String> petEffects = pet.getEffects();
        if (petEffects != null && !petEffects.isEmpty()) {
            PetEffectTracker.addEffectsToPlayer(player, petEffects);
            DebugLogger.getInstance().log(Level.INFO,
                    "Applied pet effects " + petEffects + " to " + player.getName());
        }
    }

    /**
     * Removes the pet's stats & effects from the player.
     */
    public static void removePetBonuses(Player player, Pet pet) {
        if (statsManager == null) {
            DebugLogger.getInstance().warning("PetStatManager not initialized yet!");
            return;
        }
        Map<String, Object> configStats = pet.getAdditionalStats();
        if (configStats != null && !configStats.isEmpty()) {
            PlayerStats ps = statsManager.loadStats(player);
            for (Map.Entry<String, Object> entry : configStats.entrySet()) {
                StatType st = parseStatType(entry.getKey());
                double val = parseDouble(entry.getValue(), 0.0);
                if (st != null && val != 0.0) {
                    double current = ps.getTempStat(st);
                    ps.addTempStat(st, -val);
                    DebugLogger.getInstance().log(Level.INFO,
                            "Removed +" + val + " " + st.name() + " from pet " + pet.getName());
                }
            }
            statsManager.saveStats(player, ps);
        }

        List<String> petEffects = pet.getEffects();
        if (petEffects != null && !petEffects.isEmpty()) {
            PetEffectTracker.removeEffectsFromPlayer(player, petEffects);
            DebugLogger.getInstance().log(Level.INFO,
                    "Removed pet effects " + petEffects + " from " + player.getName());
        }
    }

    private static StatType parseStatType(String key) {
        try {
            return StatType.valueOf(key.toUpperCase());
        } catch (Exception e) {
            DebugLogger.getInstance().warning("Invalid stat type '" + key + "' in pet config");
            return null;
        }
    }

    private static double parseDouble(Object val, double fallback) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }
}
