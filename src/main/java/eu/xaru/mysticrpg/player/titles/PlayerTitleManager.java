package eu.xaru.mysticrpg.player.titles;

import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

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

    /**
     * Loads all titles from YML files in the /plugins/MysticRPG/titles folder.
     * Each file should have the structure:
     * id: <string>
     * name: <string>
     * attributes:
     *   <STAT_NAME>: <int>
     */
    private void loadTitlesFromDisk() {
        File titlesFolder = new File(JavaPlugin.getProvidingPlugin(getClass()).getDataFolder(), "titles");
        if (!titlesFolder.exists()) {
            titlesFolder.mkdirs();
        }

        File[] files = titlesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            if (id == null || name == null) {
                DebugLogger.getInstance().error("Title file " + file.getName() + " missing 'id' or 'name'");
                continue;
            }

            // Load attributes
            Map<StatType, Integer> bonuses = new EnumMap<>(StatType.class);
            if (config.isConfigurationSection("attributes")) {
                for (String key : config.getConfigurationSection("attributes").getKeys(false)) {
                    try {
                        StatType stat = StatType.valueOf(key.toUpperCase());
                        int val = config.getInt("attributes." + key, 0);
                        bonuses.put(stat, val);
                    } catch (IllegalArgumentException e) {
                        DebugLogger.getInstance().error("Invalid stat " + key + " in title " + id);
                    }
                }
            }

            PlayerTitle title = new PlayerTitle(name, bonuses);
            availableTitles.put(id.toLowerCase(), title);
            DebugLogger.getInstance().log(Level.INFO, "Loaded title: " + id + " (" + name + ")", 0);
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
        PlayerTitle title = availableTitles.get(current.toLowerCase());
        return title;
    }

    public boolean equipTitle(Player player, String titleId) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No player data found!");
            return false;
        }

        titleId = titleId.toLowerCase();
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
        PlayerTitle title = availableTitles.get(titleId.toLowerCase());
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
            PlayerTitle pt = availableTitles.get(t.toLowerCase());
            String titleName = (pt != null) ? pt.getName() : t;
            player.sendMessage(ChatColor.AQUA + "- " + titleName + " (id: " + t + ")");
        }
    }

    /**
     * Unlock a title for a player.
     * @param player The player
     * @param titleId The title id to unlock
     */
    public boolean unlockTitle(Player player, String titleId) {
        PlayerData data = dataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "No player data found!");
            return false;
        }
        titleId = titleId.toLowerCase();
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

        // If player has no titles at all, automatically unlock and equip "adventurer" if it exists
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
        return current != null && availableTitles.containsKey(current.toLowerCase());
    }

    public String getEquippedTitleName(Player player) {
        PlayerTitle title = getEquippedTitle(player);
        return (title != null) ? title.getName() : null;
    }
}
