package eu.xaru.mysticrpg.player.titles;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the acquisition, listing, loading and equipping of player titles.
 * Titles are loaded from YML files in the /plugins/MysticRPG/titles folder.
 */
public class PlayerTitleManager {

    private final PlayerDataCache dataCache;
    private final PlayerStatsManager statsManager;
    private final Map<String, PlayerTitle> availableTitles = new HashMap<>();

    public PlayerTitleManager(PlayerDataCache dataCache, PlayerStatsManager statsManager) {
        this.dataCache = dataCache;
        this.statsManager = statsManager;
        loadTitlesFromDisk();
    }

    private void loadTitlesFromDisk() {
        File titlesFolder = new File("plugins/MysticRPG/titles");
        if (!titlesFolder.exists()) {
            titlesFolder.mkdirs();
        }

        File[] files = titlesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String userFileName = "titles/" + file.getName();
                DynamicConfigManager.loadConfig(userFileName);
                DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
                if (config == null) {
                    DebugLogger.getInstance().error("Failed to retrieve DynamicConfig for file " + file.getName());
                    continue;
                }

                String id = config.getString("id", null);
                String name = config.getString("name", null);
                if (id == null || name == null) {
                    DebugLogger.getInstance().error("Title file " + file.getName() + " missing 'id' or 'name'");
                    continue;
                }

                Map<StatType, Integer> bonuses = new EnumMap<>(StatType.class);

                Object attrObj = config.get("attributes");
                if (attrObj instanceof Map<?,?> attrsMap) {
                    for (Map.Entry<?,?> e : attrsMap.entrySet()) {
                        String statKey = e.getKey().toString();
                        StatType statType;
                        try {
                            statType = StatType.valueOf(statKey.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            DebugLogger.getInstance().error("Invalid stat " + statKey + " in title " + id);
                            continue;
                        }
                        int val = parseInt(e.getValue(), 0);
                        bonuses.put(statType, val);
                    }
                }

                PlayerTitle title = new PlayerTitle(name, bonuses);
                availableTitles.put(id.toLowerCase(Locale.ROOT), title);
                DebugLogger.getInstance().log(Level.INFO, "Loaded title: " + id + " (" + name + ")", 0);

            } catch (Exception ex) {
                DebugLogger.getInstance().error("Error loading title from file " + file.getName() + ":", ex);
            }
        }
    }

    public Map<String, PlayerTitle> getAvailableTitles() {
        return availableTitles;
    }

    public PlayerTitle getEquippedTitle(Player player) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return null;
        String current = data.getCurrentTitle();
        if (current == null) return null;
        return availableTitles.get(current.toLowerCase(Locale.ROOT));
    }

    public boolean equipTitle(Player player, String titleId) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No player data found!");
            return false;
        }
        titleId = titleId.toLowerCase(Locale.ROOT);
        if (!data.getUnlockedTitles().contains(titleId)) {
            player.sendMessage(ChatColor.RED + "You have not unlocked this title!");
            return false;
        }
        if (!availableTitles.containsKey(titleId)) {
            player.sendMessage(ChatColor.RED + "This title does not exist!");
            return false;
        }
        data.setCurrentTitle(titleId);
        player.sendMessage(ChatColor.GREEN + "You have equipped the title: " + availableTitles.get(titleId).getName());

        applyTitleBonuses(player, titleId);
        return true;
    }

    public void applyTitleBonuses(Player player, String titleId) {
        PlayerTitle title = availableTitles.get(titleId.toLowerCase(Locale.ROOT));
        if (title == null) return;

        for (Map.Entry<StatType, Integer> bonus : title.getStatBonuses().entrySet()) {
            statsManager.increaseBaseStat(player, bonus.getKey(), bonus.getValue());
        }
    }

    public void listPlayerTitles(Player player) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No player data found!");
            return;
        }

        List<String> unlocked = data.getUnlockedTitles();
        if (unlocked.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no titles unlocked.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "=== Your Unlocked Titles ===");
        for (String t : unlocked) {
            PlayerTitle pt = availableTitles.get(t.toLowerCase(Locale.ROOT));
            String titleName = (pt != null) ? pt.getName() : t;
            player.sendMessage(ChatColor.AQUA + "- " + titleName + " (id: " + t + ")");
        }
    }

    public boolean unlockTitle(Player player, String titleId) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No player data found!");
            return false;
        }
        titleId = titleId.toLowerCase(Locale.ROOT);
        if (!availableTitles.containsKey(titleId)) {
            player.sendMessage(ChatColor.RED + "This title does not exist!");
            return false;
        }

        List<String> unlocked = data.getUnlockedTitles();
        if (!unlocked.contains(titleId)) {
            unlocked.add(titleId);
            player.sendMessage(ChatColor.GREEN + "You have unlocked the title: " + availableTitles.get(titleId).getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "You already unlocked this title.");
        }
        return true;
    }

    public void ensureDefaultTitle(Player player) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getUnlockedTitles().isEmpty()) {
            String defaultTitleId = "adventurer";
            if (availableTitles.containsKey(defaultTitleId)) {
                data.getUnlockedTitles().add(defaultTitleId);
                data.setCurrentTitle(defaultTitleId);
            }
        }
    }

    public boolean hasEquippedTitle(Player player) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return false;
        String current = data.getCurrentTitle();
        return current != null && availableTitles.containsKey(current.toLowerCase(Locale.ROOT));
    }

    public String getEquippedTitleName(Player player) {
        PlayerTitle title = getEquippedTitle(player);
        return (title != null) ? title.getName() : null;
    }

    private int parseInt(Object val, int fallback) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }
}
