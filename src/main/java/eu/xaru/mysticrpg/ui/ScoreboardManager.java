package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.DungeonModule;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
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

public class ScoreboardManager {

    private final JavaPlugin plugin;
    private final LevelModule levelModule;
    private final EconomyHelper economyHelper;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private ChatFormatter chatFormatter;
    private DungeonManager dungeonManager;

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Set<UUID> onlinePlayerUUIDs = new HashSet<>();
    private final Map<UUID, Set<String>> playerEntries = new HashMap<>();
    private final AtomicBoolean isScheduled = new AtomicBoolean(false);

    public ScoreboardManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        DungeonModule dungeonModule = ModuleManager.getInstance().getModuleInstance(DungeonModule.class);
        if (dungeonModule != null) {
            this.dungeonManager = dungeonModule.getDungeonManager();
        }
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
            this.playerDataCache = PlayerDataCache.getInstance();
        } else {
            this.playerDataCache = null;
            DebugLogger.getInstance().warning("[MysticRPG] SaveModule not found. Player data will not be loaded.");
        }
        this.chatFormatter = new ChatFormatter();

        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayerUUIDs.add(player.getUniqueId());
            createScoreboardForPlayer(player);
            createOrUpdateTeam(player);
            updatePlayerScoreboard(player);
        }

        startScoreboardUpdater();
    }

    private void startScoreboardUpdater() {
        if (isScheduled.compareAndSet(false, true)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        createOrUpdateTeam(player);
                        updatePlayerScoreboard(player);
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
            DebugLogger.getInstance().log("[MysticRPG] Scoreboard updater started, updating scoreboards every second.");
        }
    }

    public void createScoreboardForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerScoreboards.containsKey(uuid)) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(uuid, scoreboard);
            DebugLogger.getInstance().log("[MysticRPG] Created side scoreboard for player " + player.getName());

            setupPerPlayerScoreboard(scoreboard, player);

            for (UUID otherUUID : onlinePlayerUUIDs) {
                Player otherPlayer = Bukkit.getPlayer(otherUUID);
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    addPlayerToScoreboard(scoreboard, otherPlayer);
                }
            }

            player.setScoreboard(scoreboard);
        }
    }

    private void setupPerPlayerScoreboard(Scoreboard scoreboard, Player player) {
        Objective objective = scoreboard.getObjective("mysticSidebar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("mysticSidebar", "dummy",
                    ChatColor.translateAlternateColorCodes('&', "け"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        playerEntries.put(player.getUniqueId(), new HashSet<>());
    }

    private void addPlayerToScoreboard(Scoreboard scoreboard, Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid.toString();

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            DebugLogger.getInstance().log("[MysticRPG] Created new team '" + teamName + "' in scoreboard.");
        }

        String prefix = getPrefixFromLuckPerms(player);
        String suffix = ChatColor.BLACK + " [" + ChatColor.GREEN + "LVL" + ChatColor.RED + getPlayerLevel(uuid) + ChatColor.BLACK + "]";

        team.setPrefix(prefix + " ");
        team.setSuffix(suffix);

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
            DebugLogger.getInstance().log("[MysticRPG] Added player " + player.getName() + " to team '" + teamName + "' in scoreboard.");
        }
    }

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

    public void updatePlayerScoreboard(Player player) {
        if (playerDataCache == null) return;

        // Check if player is inside a dungeon instance
        DungeonInstance instance = null;
        if (dungeonManager != null) {
            instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        }

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            createScoreboardForPlayer(player);
            scoreboard = playerScoreboards.get(player.getUniqueId());
            if (scoreboard == null) return;
        }

        Objective objective = scoreboard.getObjective("mysticSidebar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("mysticSidebar", "dummy",
                    ChatColor.translateAlternateColorCodes('&', "け"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Set<String> entries = playerEntries.get(player.getUniqueId());
        if (entries != null) {
            for (String entry : entries) {
                scoreboard.resetScores(entry);
            }
            entries.clear();
        }

        Set<String> newEntries = new HashSet<>();

        if (instance != null) {
            // Player is in a dungeon, show dungeon scoreboard lines
            String dungeonName = instance.getConfig().getName();
            int monstersKilled = instance.getMonstersKilledByPlayer(player);
            int timeSpent = instance.getTimeSpent();
            List<Player> partyMembers = instance.getPartyMembers();

            // Format time: mm:ss
            int minutes = timeSpent / 60;
            int seconds = timeSpent % 60;
            String formattedTime = String.format("%02d:%02d", minutes, seconds);

            objective.setDisplayName(ChatColor.GOLD + dungeonName);

            int score = 10;

            String monsterEntry = ChatColor.GREEN + "Monsters: " + ChatColor.WHITE + monstersKilled;
            objective.getScore(monsterEntry).setScore(score--);
            newEntries.add(monsterEntry);

            String timeEntry = ChatColor.YELLOW + "Time: " + ChatColor.WHITE + formattedTime;
            objective.getScore(timeEntry).setScore(score--);
            newEntries.add(timeEntry);

            String partyLabel = ChatColor.AQUA + "Party Members:";
            objective.getScore(partyLabel).setScore(score--);
            newEntries.add(partyLabel);

            for (Player pm : partyMembers) {
                String memberLine = ChatColor.GRAY + " - " + pm.getName();
                objective.getScore(memberLine).setScore(score--);
                newEntries.add(memberLine);
            }

            String emptyLine = ChatColor.BLACK.toString() + ChatColor.RESET;
            objective.getScore(emptyLine).setScore(score--);
            newEntries.add(emptyLine);

            playerEntries.put(player.getUniqueId(), newEntries);

            // In a dungeon, always set scoreboard
            player.setScoreboard(scoreboard);

        } else {
            // Player is not in a dungeon, show normal scoreboard lines
            PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (playerData == null) return;

            objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', "け"));

            String separatorLine1 = "げげ";
            objective.getScore(separatorLine1).setScore(15);
            newEntries.add(separatorLine1);

            String firstemptyline = "  ";
            objective.getScore(firstemptyline).setScore(14);
            newEntries.add(firstemptyline);

            String yourStats = "ぁ YOUR STATS";
            objective.getScore(yourStats).setScore(13);
            newEntries.add(yourStats);

            int level = playerData.getLevel();

            String levelEntry;
            if( level <= 10){
                levelEntry = "   あ LEVEL " + ChatColor.WHITE + level;
            } else if(level > 10 && level <= 20){
                levelEntry = "   あ LEVEL " + ChatColor.GREEN + level;
            } else if(level > 20 && level < 25){
                levelEntry = "   あ LEVEL " + ChatColor.GOLD + level;
            } else {
                levelEntry = "   あ LEVEL " + ChatColor.RED + "MAXEDぅ";
            }
            objective.getScore(levelEntry).setScore(12);
            newEntries.add(levelEntry);

            int currentXp = playerData.getXp();
            int xpNeeded = levelModule != null ? levelModule.getLevelThreshold(level + 1) : 100;
            String xpEntry = ChatColor.RED + "   あ " + ChatColor.WHITE + "XP " + ChatColor.GREEN + currentXp + ChatColor.WHITE + "/" + ChatColor.GOLD + xpNeeded;
            objective.getScore(xpEntry).setScore(11);
            newEntries.add(xpEntry);

            int balance = economyHelper != null ? economyHelper.getHeldGold(player) : 0;
            String balanceEntry = ChatColor.YELLOW + "   い " + ChatColor.WHITE + balance + ChatColor.GOLD + " held Gold";
            objective.getScore(balanceEntry).setScore(10);
            newEntries.add(balanceEntry);

            String secondemptyline = "   ";
            objective.getScore(secondemptyline).setScore(9);
            newEntries.add(secondemptyline);

            String information = "ぇ INFORMATION";
            objective.getScore(information).setScore(8);
            newEntries.add(information);

            String yourRank = "   え " + chatFormatter.getPrefixFromLuckPerms(player);
            objective.getScore(yourRank).setScore(7);
            newEntries.add(yourRank);

            String petID = playerData.getEquippedPet();
            if (petID != null) {
                String formattedName = formatPetName(petID);
                String yourpet = "   シ "+ formattedName;
                objective.getScore(yourpet).setScore(5);
                newEntries.add(yourpet);
            } else {
                String yourpet = "   シ " + ChatColor.RED + "No Pet";
                objective.getScore(yourpet).setScore(5);
                newEntries.add(yourpet);
            }

            String pinnedQuestId = playerData.getPinnedQuest();
            UUID playerUUID = player.getUniqueId();
            String currentObjective = questManager != null ? questManager.getCurrentObjective(playerUUID) : "No Objective";

            if (pinnedQuestId == null) {
                String noPinnedQuestEntry = "   う " + ChatColor.RED + "No Pinned Quest";
                objective.getScore(noPinnedQuestEntry).setScore(3);
                newEntries.add(noPinnedQuestEntry);
            } else {
                String questID = "   う " + ChatColor.GREEN + currentObjective;
                objective.getScore(questID).setScore(3);
                newEntries.add(questID);
            }

            String thirdemptyline = "    ";
            objective.getScore(thirdemptyline).setScore(2);
            newEntries.add(thirdemptyline);

            String separatorLine2 ="げげ ";
            objective.getScore(separatorLine2).setScore(1);
            newEntries.add(separatorLine2);

            String websiteEntry = ChatColor.DARK_GRAY + "さ";
            objective.getScore(websiteEntry).setScore(0);
            newEntries.add(websiteEntry);

            playerEntries.put(player.getUniqueId(), newEntries);

            // Only assign scoreboard if player in "world"
            if (player.getWorld().getName().equals("world")) {
                player.setScoreboard(scoreboard);
            } else {
                // If not in "world", revert to main scoreboard (no side)
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

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
        return capitalizeWords(objectiveKey.replace("_", " "));
    }

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

    private String getPrefixFromLuckPerms(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            DebugLogger.getInstance().warning("[MysticRPG] LuckPerms not found. Unable to retrieve prefix for " + player.getName());
            return "";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
                DebugLogger.getInstance().warning("[MysticRPG] Could not load LuckPerms user data for " + player.getName());
                return "";
            }
        }

        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public int getPlayerLevel(UUID uuid) {
        if (playerDataCache == null) return 1;
        PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
        if (playerData == null) return 1;
        return playerData.getLevel();
    }

    public String formatPetName(String petId) {
        if (petId == null || petId.isEmpty()) {
            return "Unknown Pet";
        }
        String[] words = petId.split("_");
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formattedName.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return formattedName.toString().trim();
    }

    public void cleanup() {
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

    public void addNewPlayer(Player newPlayer) {
        UUID newUUID = newPlayer.getUniqueId();
        onlinePlayerUUIDs.add(newUUID);
        createScoreboardForPlayer(newPlayer);
        createOrUpdateTeam(newPlayer);

        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();

            if (targetUUID.equals(newUUID)) continue;

            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                addPlayerToScoreboard(scoreboard, newPlayer);
            }
        }
    }

    public void removePlayer(Player departingPlayer) {
        UUID departingUUID = departingPlayer.getUniqueId();
        onlinePlayerUUIDs.remove(departingUUID);

        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();

            if (targetUUID.equals(departingUUID)) continue;

            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                removePlayerFromScoreboard(scoreboard, departingPlayer);
            }
        }

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

        playerEntries.remove(departingUUID);
    }

    public void createOrUpdateTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid.toString();

        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            UUID targetUUID = entry.getKey();
            Scoreboard scoreboard = entry.getValue();
            Player targetPlayer = Bukkit.getPlayer(targetUUID);

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                DebugLogger.getInstance().log("[MysticRPG] Created new team '" + teamName + "' in scoreboard.");
            }

            String prefix = getPrefixFromLuckPerms(player);
            String suffix = ChatColor.BLACK + " [" + ChatColor.GREEN + "LVL" + ChatColor.RED + getPlayerLevel(uuid) + ChatColor.BLACK + "]";

            team.setPrefix(prefix + " ");
            team.setSuffix(suffix);

            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
                DebugLogger.getInstance().log("[MysticRPG] Added player " + player.getName() + " to team '" + teamName + "' in scoreboard.");
            }

            if (entry.getKey().equals(uuid)) {
                player.setScoreboard(scoreboard);
            }
        }
    }
}
