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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Custom ScoreboardManager to handle shared and per-player scoreboards.
 */
public class ScoreboardManager {

    private final JavaPlugin plugin;
    private final LevelModule levelModule;
    private final EconomyHelper economyHelper;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;

    // Map of player UUID to their per-player scoreboard
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    // Set to track all online players for team management
    private final Set<UUID> onlinePlayerUUIDs = new HashSet<>();

    // Map of player UUID to their own side data entries
    private final Map<UUID, Set<String>> playerEntries = new HashMap<>();

    // To prevent multiple scheduled tasks
    private final AtomicBoolean isScheduled = new AtomicBoolean(false);

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

        // Initialize for existing online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayerUUIDs.add(player.getUniqueId());
            createScoreboardForPlayer(player);
            createOrUpdateTeam(player);
            updatePlayerScoreboard(player);
        }

        // Start the scoreboard updater
        startScoreboardUpdater();
    }

    /**
     * Starts a scheduled task to update all players' scoreboards periodically.
     */
    private void startScoreboardUpdater() {
        if (isScheduled.compareAndSet(false, true)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        createOrUpdateTeam(player); // Update team data first
                        updatePlayerScoreboard(player); // Then update scoreboard
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // Update every second (20 ticks)
            DebugLogger.getInstance().log("[MysticRPG] Scoreboard updater started, updating scoreboards every second.");
        }
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
            DebugLogger.getInstance().log("[MysticRPG] Created side scoreboard for player " + player.getName());

            // Create and assign side data to the scoreboard
            setupPerPlayerScoreboard(scoreboard, player);

            // Add all existing players to the new player's scoreboard
            for (UUID otherUUID : onlinePlayerUUIDs) {
                Player otherPlayer = Bukkit.getPlayer(otherUUID);
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    addPlayerToScoreboard(scoreboard, otherPlayer);
                }
            }

            // Assign the scoreboard to the player
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Sets up the per-player scoreboard with side data.
     *
     * @param scoreboard The player's scoreboard.
     * @param player     The player.
     */
    private void setupPerPlayerScoreboard(Scoreboard scoreboard, Player player) {
        // Ensure the main sidebar objective is set up
        Objective objective = scoreboard.getObjective("mysticSidebar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("mysticSidebar", "dummy",
                    ChatColor.translateAlternateColorCodes('&', "け"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Initialize entries set
        playerEntries.put(player.getUniqueId(), new HashSet<>());
    }

    /**
     * Adds a player to a specific scoreboard's teams.
     *
     * @param scoreboard The scoreboard to modify.
     * @param player     The player whose team is to be added.
     */
    private void addPlayerToScoreboard(Scoreboard scoreboard, Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid.toString();

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            DebugLogger.getInstance().log("[MysticRPG] Created new team '" + teamName + "' in scoreboard.");
        }

        // Set prefix and suffix
        String prefix = getPrefixFromLuckPerms(player);
        String suffix = ChatColor.BLACK + " [" + ChatColor.GREEN + "LVL" + ChatColor.RED + getPlayerLevel(uuid) + ChatColor.BLACK + "]";

        team.setPrefix(prefix + " ");
        team.setSuffix(suffix);

        // Add the player's name to their team in this scoreboard
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
            DebugLogger.getInstance().log("[MysticRPG] Added player " + player.getName() + " to team '" + teamName + "' in scoreboard.");
        }
    }

    /**
     * Removes a player from a specific scoreboard's teams.
     *
     * @param scoreboard The scoreboard to modify.
     * @param player     The player whose team is to be removed.
     */
    private void removePlayerFromScoreboard(Scoreboard scoreboard, Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid.toString();

        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
            DebugLogger.getInstance().log("[MysticRPG] Removed player " + player.getName() + " from team '" + teamName + "' in scoreboard.");
        }
    }

    /**
     * Updates the scoreboard for a specific player with their own side data.
     *
     * @param player The player whose scoreboard to update.
     */
    public void updatePlayerScoreboard(Player player) {
        if (playerDataCache == null) return;

        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            createScoreboardForPlayer(player);
            scoreboard = playerScoreboards.get(player.getUniqueId());
            if (scoreboard == null) return; // Failed to create
        }

        // Get the sidebar objective
        Objective objective = scoreboard.getObjective("mysticSidebar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("mysticSidebar", "dummy",
                    ChatColor.translateAlternateColorCodes('&', "け"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Reset previous entries
        Set<String> entries = playerEntries.get(player.getUniqueId());
        if (entries != null) {
            for (String entry : entries) {
                scoreboard.resetScores(entry);
            }
            entries.clear();
        }

        Set<String> newEntries = new HashSet<>();

        // Populate scoreboard with player stats
        // Unique separator lines
        String separatorLine1 = "げげ";
        objective.getScore(separatorLine1).setScore(14);
        newEntries.add(separatorLine1);

        // Your Stats:
        String yourStats = "く";
        objective.getScore(yourStats).setScore(13);
        newEntries.add(yourStats);

        // Level
        int level = playerData.getLevel();
        String levelEntry = "か" + ChatColor.WHITE + level;
        objective.getScore(levelEntry).setScore(12);
        newEntries.add(levelEntry);

        // XP Needed
        int currentXp = playerData.getXp();
        int xpNeeded = levelModule != null ? levelModule.getLevelThreshold(level + 1) : 100;
        String xpEntry = ChatColor.YELLOW + "XP: " + ChatColor.WHITE + currentXp + "/" + xpNeeded;
        objective.getScore(xpEntry).setScore(11);
        newEntries.add(xpEntry);

        // Balance
        double balance = economyHelper != null ? economyHelper.getBalance(player) : 0.0;
        String formattedBalance = economyHelper != null ? economyHelper.formatBalance(balance) : "0.00";
        String balanceEntry = ChatColor.YELLOW + "Balance: " + ChatColor.WHITE + "$" + formattedBalance;
        objective.getScore(balanceEntry).setScore(10);
        newEntries.add(balanceEntry);

        // Empty line
        String emptyLine = " ";
        objective.getScore(emptyLine).setScore(9);
        newEntries.add(emptyLine);

        // Your Pinned Quest:
        String pinnedQuestTitle = ChatColor.GREEN + "Pinned Quest:";
        objective.getScore(pinnedQuestTitle).setScore(8);
        newEntries.add(pinnedQuestTitle);

        String pinnedQuestId = playerData.getPinnedQuest();
        if (pinnedQuestId != null && questManager != null) {
            Quest pinnedQuest = questManager.getQuest(pinnedQuestId);
            if (pinnedQuest != null) {
                // Quest Name
                String questNameEntry = ChatColor.AQUA + pinnedQuest.getName();
                objective.getScore(questNameEntry).setScore(7);
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
                    String objectiveDisplay = ChatColor.GRAY + formattedObjective + ": " + ChatColor.WHITE + currentProgress + "/" + required;
                    objective.getScore(objectiveDisplay).setScore(scoreIndex);
                    newEntries.add(objectiveDisplay);
                    scoreIndex--;
                    if (scoreIndex < 2) break; // Adjusted to fit in the scoreboard
                }
            } else {
                String noPinnedQuestEntry = ChatColor.RED + "No Pinned Quest";
                objective.getScore(noPinnedQuestEntry).setScore(7);
                newEntries.add(noPinnedQuestEntry);
            }
        } else {
            String noPinnedQuestEntry = ChatColor.RED + "No Pinned Quest";
            objective.getScore(noPinnedQuestEntry).setScore(7);
            newEntries.add(noPinnedQuestEntry);
        }

        // Second unique separator line
        String separatorLine2 = ChatColor.GRAY + "げげ";
        objective.getScore(separatorLine2).setScore(1);
        newEntries.add(separatorLine2);

        // Website
        String websiteEntry = ChatColor.DARK_GRAY + "さ";
        objective.getScore(websiteEntry).setScore(0);
        newEntries.add(websiteEntry);

        playerEntries.put(player.getUniqueId(), newEntries);

        // Assign the scoreboard to the player
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
     * Retrieves the prefix from LuckPerms for the specified player.
     *
     * @param player The player whose prefix is to be retrieved.
     * @return The prefix string, or an empty string if not set.
     */
    private String getPrefixFromLuckPerms(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            DebugLogger.getInstance().warning("[MysticRPG] LuckPerms not found. Unable to retrieve prefix for " + player.getName());
            return "";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            // User might not be cached; attempt to load
            user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
                DebugLogger.getInstance().warning("[MysticRPG] Could not load LuckPerms user data for " + player.getName());
                return "";
            }
        }

        // Retrieve the highest priority prefix
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix == null) {
            return "";
        }

        // Translate color codes and return
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     * Retrieves a player's current level.
     *
     * @param uuid The UUID of the player.
     * @return The player's level, or 1 if not found.
     */
    public int getPlayerLevel(UUID uuid) {
        if (playerDataCache == null) return 1;
        PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
        if (playerData == null) return 1;
        return playerData.getLevel();
    }

    /**
     * Cleans up all Scoreboard Teams and objectives managed by this ScoreboardManager.
     * This should be called during module unload to prevent memory leaks.
     */
    public void cleanup() {
        // Clean up all teams and objectives from all per-player scoreboards
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            Scoreboard scoreboard = entry.getValue();
            if (scoreboard != null) {
                for (Team team : scoreboard.getTeams()) {
                    team.unregister();
                }

                for (Objective objective : scoreboard.getObjectives()) {
                    objective.unregister();
                }
            }
        }
        playerScoreboards.clear();
        playerEntries.clear();
        onlinePlayerUUIDs.clear();
        DebugLogger.getInstance().log("[MysticRPG] ScoreboardManager cleanup called. Cleared all scoreboards.");
    }

    /**
     * Adds a new player to all existing per-player scoreboards' teams.
     *
     * @param newPlayer The new player to add.
     */
    public void addNewPlayer(Player newPlayer) {
        UUID newUUID = newPlayer.getUniqueId();
        onlinePlayerUUIDs.add(newUUID);
        createScoreboardForPlayer(newPlayer);
        createOrUpdateTeam(newPlayer);

        // Add the new player's team to all existing players' scoreboards
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();

            if (targetUUID.equals(newUUID)) continue; // Already handled

            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                addPlayerToScoreboard(scoreboard, newPlayer);
            }
        }
    }

    /**
     * Removes a player from all existing per-player scoreboards' teams and cleans up their scoreboard.
     *
     * @param departingPlayer The player to remove.
     */
    public void removePlayer(Player departingPlayer) {
        UUID departingUUID = departingPlayer.getUniqueId();
        onlinePlayerUUIDs.remove(departingUUID);

        // Remove the player's team from all existing scoreboards
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();

            if (targetUUID.equals(departingUUID)) continue; // Will unregister their own scoreboard later

            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                removePlayerFromScoreboard(scoreboard, departingPlayer);
            }
        }

        // Clean up their own scoreboard
        Scoreboard departingScoreboard = playerScoreboards.get(departingUUID);
        if (departingScoreboard != null) {
            for (Team team : departingScoreboard.getTeams()) {
                team.unregister();
            }

            for (Objective objective : departingScoreboard.getObjectives()) {
                objective.unregister();
            }

            playerScoreboards.remove(departingUUID);
            DebugLogger.getInstance().log("[MysticRPG] Cleaned up scoreboard for departing player " + departingPlayer.getName());
        }

        // Remove side data entries
        playerEntries.remove(departingUUID);
    }

    /**
     * Creates or updates a team for a player on the shared scoreboard.
     *
     * @param player The player whose team is to be created or updated.
     */
    public void createOrUpdateTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid.toString();

        // Iterate through all player scoreboards and update teams
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();
            Player targetPlayer = Bukkit.getPlayer(targetUUID);

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                DebugLogger.getInstance().log("[MysticRPG] Created new team '" + teamName + "' in scoreboard.");
            }

            // Set prefix and suffix
            String prefix = getPrefixFromLuckPerms(player);
            String suffix = ChatColor.BLACK + " [" + ChatColor.GREEN + "LVL" + ChatColor.RED + getPlayerLevel(uuid) + ChatColor.BLACK + "]";

            team.setPrefix(prefix + " ");
            team.setSuffix(suffix);

            // Add the player's name to their team in this scoreboard
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
                DebugLogger.getInstance().log("[MysticRPG] Added player " + player.getName() + " to team '" + teamName + "' in scoreboard.");
            }

            // If this scoreboard belongs to the player, re-assign it to force display updates
            if (entry.getKey().equals(uuid)) {
                player.setScoreboard(scoreboard);
                DebugLogger.getInstance().log("[MysticRPG] Reassigned scoreboard to player " + player.getName() + " after team update.");
            }
        }
    }
}
