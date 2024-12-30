package eu.xaru.mysticrpg.customs.items.sets;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles loading ItemSets from /plugins/MysticRPG/custom/items/sets/*.yml using DynamicConfig.
 */
public class SetManager {

    private static SetManager instance;
    private final Map<String, ItemSet> sets = new HashMap<>();

    private SetManager() {
        loadSets();
    }

    public static SetManager getInstance() {
        if (instance == null) {
            instance = new SetManager();
        }
        return instance;
    }

    private void loadSets() {

        File setsFolder = new File(MysticCore.getInstance().getDataFolder(), "custom\\items\\sets");
        if (!setsFolder.exists()) {
            if (!setsFolder.mkdirs()) {
                DebugLogger.getInstance().severe("Failed to create sets folder.");
                return;
            }
        }

        File[] files = setsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {

                DynamicConfig config = DynamicConfigManager.loadConfig(file.getPath());

                if (config == null) {
                    DebugLogger.getInstance().severe("Failed to load DynamicConfig for set file: " + file.getName());
                    continue;
                }

                String id = config.getString("id", "");
                if (id.isEmpty()) {
                    DebugLogger.getInstance().severe("Set ID is missing in file: " + file.getName());
                    continue;
                }

                // bonuses -> Map<Integer, Map<StatType, Double>>
                Map<Integer, Map<StatType, Double>> pieceBonuses = new HashMap<>();

                Object bonusesObj = config.get("bonuses");
                if (bonusesObj instanceof Map<?,?> bonusesMap) {
                    // Each key is thresholdKey (string), value is a sub-map of stat->value
                    for (Map.Entry<?,?> e : bonusesMap.entrySet()) {
                        String thresholdKey = String.valueOf(e.getKey());
                        int threshold;
                        try {
                            threshold = Integer.parseInt(thresholdKey);
                        } catch (NumberFormatException ex) {
                            DebugLogger.getInstance().warning("Invalid threshold '" + thresholdKey
                                    + "' in set '" + id + "' (file: " + file.getName() + ")");
                            continue;
                        }

                        if (e.getValue() instanceof Map<?,?> statsMap) {
                            Map<StatType, Double> statsMapConverted = new EnumMap<>(StatType.class);
                            for (Map.Entry<?,?> statEntry : statsMap.entrySet()) {
                                String statKey = String.valueOf(statEntry.getKey()).toUpperCase(Locale.ROOT);
                                StatType statType;
                                try {
                                    statType = StatType.valueOf(statKey);
                                } catch (IllegalArgumentException ex) {
                                    DebugLogger.getInstance().warning("Invalid stat '" + statKey
                                            + "' in set '" + id + "', threshold " + threshold);
                                    continue;
                                }
                                double value = parseDouble(statEntry.getValue(), 0.0);
                                statsMapConverted.put(statType, value);
                            }
                            pieceBonuses.put(threshold, statsMapConverted);
                        }
                    }
                }

                ItemSet itemSet = new ItemSet(id, pieceBonuses);
                sets.put(id, itemSet);
                DebugLogger.getInstance().log(Level.INFO, "Loaded set: " + id);

            } catch (Exception e) {
                DebugLogger.getInstance().severe("Failed to load set configuration from file " + file.getName() + ":", e);
            }
        }
    }

    public ItemSet getSet(String id) {
        return sets.get(id);
    }

    public String formatSetName(String setId) {
        if (setId == null || setId.isEmpty()) return "";
        String[] parts = setId.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private double parseDouble(Object val, double fallback) {
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
