package eu.xaru.mysticrpg.player.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Holds permanent and temporary stats for a player.
 * Permanent stats come from level-ups, gear, etc.
 * Temporary stats can come from buffs, potions, etc.
 */
public class PlayerStats {
    private final EnumMap<StatType, Double> baseStats; // permanent stats
    private final EnumMap<StatType, Double> tempStats; // temporary modifiers (additive)

    public PlayerStats() {
        baseStats = new EnumMap<>(StatType.class);
        tempStats = new EnumMap<>(StatType.class);
        // Initialize all stats to 0
        for (StatType type : StatType.values()) {
            baseStats.put(type, 0.0);
            tempStats.put(type, 0.0);
        }
    }

    public double getBaseStat(StatType stat) {
        return baseStats.getOrDefault(stat, 0.0);
    }

    public void setBaseStat(StatType stat, double value) {
        baseStats.put(stat, value);
    }

    public void addToBaseStat(StatType stat, double value) {
        baseStats.put(stat, getBaseStat(stat) + value);
    }

    public double getTempStat(StatType stat) {
        return tempStats.getOrDefault(stat, 0.0);
    }

    public void addTempStat(StatType stat, double value) {
        tempStats.put(stat, getTempStat(stat) + value);
    }

    public void clearTempStats() {
        for (StatType stat : StatType.values()) {
            tempStats.put(stat, 0.0);
        }
    }

    /**
     * Effective stat = base stat + temp stat
     */
    public double getEffectiveStat(StatType stat) {
        return getBaseStat(stat) + getTempStat(stat);
    }

    public Map<StatType, Double> getAllEffectiveStats() {
        EnumMap<StatType, Double> result = new EnumMap<>(StatType.class);
        for (StatType stat : StatType.values()) {
            result.put(stat, getEffectiveStat(stat));
        }
        return Collections.unmodifiableMap(result);
    }
}
