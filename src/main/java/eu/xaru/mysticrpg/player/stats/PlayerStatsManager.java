package eu.xaru.mysticrpg.player.stats;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerStatsManager {
    private final PlayerDataCache playerDataCache;

    public PlayerStatsManager(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
    }

    public PlayerStats loadStats(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return new PlayerStats();
        }

        PlayerStats stats = new PlayerStats();
        EnumMap<StatType, Double> defaults = getDefaultStats();

        for (StatType type : StatType.values()) {
            double val = data.getAttributes().getOrDefault(type.name(), defaults.get(type).intValue());
            stats.setBaseStat(type, val);
        }

        return stats;
    }

    public void saveStats(Player player, PlayerStats stats) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return;
        }

        for (StatType type : StatType.values()) {
            data.getAttributes().put(type.name(), (int) stats.getBaseStat(type));
        }
    }

    public void applyTempModifier(Player player, StatType stat, double amount) {
        PlayerStats stats = loadStats(player);
        stats.addTempStat(stat, amount);
    }

    public void increaseBaseStat(Player player, StatType stat, double amount) {
        PlayerStats stats = loadStats(player);
        stats.addToBaseStat(stat, amount);
        saveStats(player, stats);
        DebugLogger.getInstance().log(Level.INFO, "Increased " + stat.name() + " by " + amount + " for " + player.getName(), 0);
    }

    public double getEffectiveStat(Player player, StatType stat) {
        PlayerStats stats = loadStats(player);
        return stats.getEffectiveStat(stat);
    }

    /**
     * Increases an attribute by 1 and decreases the player's attribute points by 1.
     * attributeName should match a StatType name (e.g. "HEALTH", "DEFENSE", "STRENGTH", "INTELLIGENCE").
     */
    public void increaseBaseAttribute(Player player, String attributeName) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No data found!");
            return;
        }

        int points = data.getAttributePoints();
        if (points <= 0) {
            player.sendMessage(ChatColor.RED + "You have no attribute points!");
            return;
        }

        // Check if attributeName corresponds to a known StatType
        StatType statType;
        try {
            statType = StatType.valueOf(attributeName);
        } catch (IllegalArgumentException e) {
            // If attributeName doesn't match any StatType
            // If you handle custom attributes differently, add logic here.
            player.sendMessage(ChatColor.RED + "Invalid attribute: " + attributeName);
            return;
        }

        // Increase the attribute in PlayerData directly
        Map<String, Integer> attrs = data.getAttributes();
        int newVal = attrs.getOrDefault(attributeName, 0) + 1;
        attrs.put(attributeName, newVal);
        data.setAttributePoints(points - 1);

        // If we increased HEALTH, ensure currentHp <= new HEALTH
        if (statType == StatType.HEALTH) {
            int maxHp = attrs.get("HEALTH");
            int currentHp = data.getCurrentHp();
            if (currentHp > maxHp) {
                data.setCurrentHp(maxHp);
            }
        }

        player.sendMessage(ChatColor.GREEN + "You increased " + attributeName + " by 1!");

        // If you want to fire a PlayerStatsChangedEvent or save data here, you can:
        // Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
        // playerDataCache.savePlayerData(player.getUniqueId(), null);
    }

    private EnumMap<StatType, Double> getDefaultStats() {
        EnumMap<StatType, Double> map = new EnumMap<>(StatType.class);
        map.put(StatType.HEALTH, 20.0);
        map.put(StatType.DEFENSE, 0.0);
        map.put(StatType.STRENGTH, 1.0);
        map.put(StatType.INTELLIGENCE, 1.0);
        map.put(StatType.CRIT_CHANCE, 5.0);
        map.put(StatType.CRIT_DAMAGE, 10.0);
        map.put(StatType.ATTACK_SPEED, 0.0);
        map.put(StatType.HEALTH_REGEN, 1.0);
        map.put(StatType.MOVEMENT_SPEED, 0.0);
        map.put(StatType.MANA, 10.0);
        return map;
    }
}
