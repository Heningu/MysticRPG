package eu.xaru.mysticrpg.customs.items.sets;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

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
        JavaPlugin plugin = JavaPlugin.getPlugin(MysticCore.class);
        File setsFolder = new File(plugin.getDataFolder(), "custom/items/sets");
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
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");
                if (id == null || id.isEmpty()) {
                    DebugLogger.getInstance().severe("Set ID is missing in file: " + file.getName());
                    continue;
                }

                Map<Integer, Map<StatType, Double>> pieceBonuses = new HashMap<>();
                if (config.contains("bonuses")) {
                    ConfigurationSection bonusesSection = config.getConfigurationSection("bonuses");
                    if (bonusesSection != null) {
                        for (String thresholdKey : bonusesSection.getKeys(false)) {
                            int threshold;
                            try {
                                threshold = Integer.parseInt(thresholdKey);
                            } catch (NumberFormatException e) {
                                DebugLogger.getInstance().warning("Invalid threshold '" + thresholdKey + "' in set '" + id + "' file: " + file.getName());
                                continue;
                            }

                            ConfigurationSection statsSection = bonusesSection.getConfigurationSection(thresholdKey);
                            if (statsSection == null) continue;

                            Map<StatType, Double> statsMap = new HashMap<>();
                            for (String statKey : statsSection.getKeys(false)) {
                                String statName = statKey.toUpperCase();
                                StatType statType;
                                try {
                                    statType = StatType.valueOf(statName);
                                } catch (IllegalArgumentException e) {
                                    DebugLogger.getInstance().warning("Invalid stat '" + statName + "' in set '" + id + "' threshold " + threshold + " in file: " + file.getName());
                                    continue;
                                }

                                double value = statsSection.getDouble(statKey, 0.0);
                                statsMap.put(statType, value);
                            }

                            pieceBonuses.put(threshold, statsMap);
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

    /**
     * Formats a set ID like "LEGENDARY_SET" into "Legendary Set".
     * Splits by underscore, capitalizes each word, and joins with a space.
     */
    public String formatSetName(String setId) {
        if (setId == null || setId.isEmpty()) return "";
        String[] parts = setId.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
