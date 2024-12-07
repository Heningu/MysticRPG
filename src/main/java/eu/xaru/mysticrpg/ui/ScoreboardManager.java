package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Custom ScoreboardManager to handle player scoreboards.
 */
public class ScoreboardManager {

    private final JavaPlugin plugin;
    private final LevelModule levelModule;
    private final EconomyHelper economyHelper;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Set<String>> playerEntries = new HashMap<>();

    /**
     * Constructs a new ScoreboardManager instance.
     */
    public ScoreboardManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            this.economyHelper = null;
            DebugLogger.getInstance().warning("[MysticRPG] EconomyModule not found. Balance display will be disabled.");
        }
        QuestModule questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        if (questModule != null) {
            this.questManager = questModule.getQuestManager();
        } else {
            this.questManager = null;
            DebugLogger.getInstance().warning("[MysticRPG] QuestModule not found. Quest display will be disabled.");
        }
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            this.playerDataCache = saveModule.getPlayerDataCache();
        } else {
            this.playerDataCache = null;
            DebugLogger.getInstance().warning("[MysticRPG] SaveModule not found. Player data will not be loaded.");
        }

        startScoreboardUpdater();
    }

    /**
     * Starts a repeating task to update all players' scoreboards.
     */
    private void startScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second (20 ticks)
    }

    /**
     * Updates the scoreboard for a specific player.
     *
     * @param player The player whose scoreboard to update.
     */
    public void updatePlayerScoreboard(Player player) {
        if (playerDataCache == null) return;

        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }

        // Ensure the main sidebar objective is set up
        Objective objective = scoreboard.getObjective("mysticSidebar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("mysticSidebar", "dummy",
                     "[" + "Mystic" + "RPG" + "]");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Reset previous entries
        Set<String> entries = playerEntries.get(player.getUniqueId());
        if (entries != null) {
            for (String entry : entries) {
                scoreboard.resetScores(entry);
            }
        }

        Set<String> newEntries = new HashSet<>();

        // Populate scoreboard with player stats
        // Unique separator lines
        String separatorLine1 = "------------------------------" + ChatColor.RESET;
        objective.getScore(separatorLine1).setScore(15);
        newEntries.add(separatorLine1);

        // Your Stats:
        String yourStats = "Your Stats:";
        objective.getScore(yourStats).setScore(14);
        newEntries.add(yourStats);

        // Level
        int level = playerData.getLevel();
        String levelEntry = "» " + "Level: " + + level;
        objective.getScore(levelEntry).setScore(13);
        newEntries.add(levelEntry);

        // XP Needed
        int currentXp = playerData.getXp();
        int xpNeeded = levelModule != null ? levelModule.getLevelThreshold(level + 1) : 0;
        String xpEntry = "» " +  "XP: " + currentXp + "/" + xpNeeded;
        objective.getScore(xpEntry).setScore(12);
        newEntries.add(xpEntry);

        // Balance
        double balance = economyHelper != null ? economyHelper.getBalance(player) : 0.0;
        String formattedBalance = economyHelper != null ? economyHelper.formatBalance(balance) : "0.00";
        String balanceEntry = "» " +  "Balance: $"  + formattedBalance;
        objective.getScore(balanceEntry).setScore(11);
        newEntries.add(balanceEntry);

        // Empty line (make it unique)
        String emptyLine =  " ";
        objective.getScore(emptyLine).setScore(10);
        newEntries.add(emptyLine);

        // Your Pinned Quest:
        String pinnedQuestTitle = "Your Pinned Quest:";
        objective.getScore(pinnedQuestTitle).setScore(9);
        newEntries.add(pinnedQuestTitle);

        String pinnedQuestId = playerData.getPinnedQuest();
        if (pinnedQuestId != null && questManager != null) {
            Quest pinnedQuest = questManager.getQuest(pinnedQuestId);
            if (pinnedQuest != null) {
                // Quest Name
                String questNameEntry =  pinnedQuest.getName();
                objective.getScore(questNameEntry).setScore(8);
                newEntries.add(questNameEntry);

                // Objective and Progress
                Map<String, Integer> objectivesMap = pinnedQuest.getObjectives();
                Map<String, Integer> progressMap = playerData.getQuestProgress().getOrDefault(pinnedQuestId, new HashMap<>());

                int scoreIndex = 7;
                for (Map.Entry<String, Integer> entry : objectivesMap.entrySet()) {
                    String objectiveKey = entry.getKey();
                    int required = entry.getValue();
                    int currentProgress = progressMap.getOrDefault(objectiveKey, 0);

                    // Cap currentProgress at required
                    currentProgress = Math.min(currentProgress, required);

                    // Format the objectiveKey
                    String formattedObjective = formatObjectiveKey(objectiveKey);

                    // Ensure uniqueness by adding color codes
                    String objectiveDisplay =  formattedObjective + ": "  + currentProgress + "/" + required;
                    objective.getScore(objectiveDisplay).setScore(scoreIndex);
                    newEntries.add(objectiveDisplay);
                    scoreIndex--;
                    if (scoreIndex < 2) break; // Adjusted to fit in the scoreboard
                }
            } else {
                String noPinnedQuestEntry =  "No Pinned Quest" + ChatColor.RESET;
                objective.getScore(noPinnedQuestEntry).setScore(8);
                newEntries.add(noPinnedQuestEntry);
            }
        } else {
            String noPinnedQuestEntry =  "No Pinned Quest" + ChatColor.RESET;
            objective.getScore(noPinnedQuestEntry).setScore(8);
            newEntries.add(noPinnedQuestEntry);
        }

        // Second unique separator line
        String separatorLine2 =  "------------------------------" + ChatColor.DARK_GRAY;
        objective.getScore(separatorLine2).setScore(1);
        newEntries.add(separatorLine2);

        // www.xaru.eu
        String websiteEntry =  "www.xaru.eu";
        objective.getScore(websiteEntry).setScore(0);
        newEntries.add(websiteEntry);

        playerEntries.put(player.getUniqueId(), newEntries);

        player.setScoreboard(scoreboard);
    }

    /**
     * Formats the objective key to a readable format.
     *
     * @param objectiveKey The raw objective key.
     * @return The formatted objective key.
     */
    private String formatObjectiveKey(String objectiveKey) {
        if (objectiveKey.startsWith("collect_")) {
            String itemName = objectiveKey.substring("collect_".length());
            itemName = itemName.replace("_", " ");
            return "Collect " + capitalizeWords(itemName);
        } else if (objectiveKey.startsWith("kill_")) {
            String mobName = objectiveKey.substring("kill_".length());
            mobName = mobName.replace("_", " ");
            return "Kill " + capitalizeWords(mobName);
        }
        // Add other cases as needed
        return capitalizeWords(objectiveKey.replace("_", " "));
    }

    /**
     * Capitalizes the first letter of each word.
     *
     * @param str The input string.
     * @return The capitalized string.
     */
    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Retrieves the PlayerDataCache instance from the SaveModule.
     *
     * @return The PlayerDataCache instance, or null if not available.
     */
    private PlayerDataCache getPlayerDataCache() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            return saveModule.getPlayerDataCache();
        }
        return null;
    }

    /**
     * Retrieves a player's specific Scoreboard.
     *
     * @param uuid The UUID of the player.
     * @return The player's Scoreboard, or null if not found.
     */
    public Scoreboard getPlayerScoreboard(UUID uuid) {
        return playerScoreboards.get(uuid);
    }

    /**
     * Retrieves a player's specific Scoreboard.
     *
     * @param player The player.
     * @return The player's Scoreboard, or null if not found.
     */
    public Scoreboard getPlayerScoreboard(Player player) {
        return getPlayerScoreboard(player.getUniqueId());
    }

    /**
     * Cleans up all Scoreboard Teams managed by this ScoreboardManager.
     * This should be called during module unload to prevent memory leaks.
     */
    public void cleanup() {
        for (Scoreboard scoreboard : playerScoreboards.values()) {
            if (scoreboard != null) {
                for (Team team : scoreboard.getTeams()) {
                    team.unregister();
                }
            }
        }
        playerScoreboards.clear();
        playerEntries.clear();
    }

    /**
     * Creates a scoreboard for the player if it doesn't already exist.
     *
     * @param player The player to create a scoreboard for.
     */
    public void createScoreboardForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerScoreboards.containsKey(uuid)) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(uuid, scoreboard);
            DebugLogger.getInstance().log("[MysticRPG] Created scoreboard for player " + player.getName());
        }
    }
}
