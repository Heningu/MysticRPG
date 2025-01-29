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

    private enum ScoreboardLine {
        SEPARATOR_1, EMPTY_1, YOUR_STATS, LEVEL, XP, BALANCE, EMPTY_2, INFORMATION, RANK, PET, QUEST, EMPTY_3, SEPARATOR_2, WEBSITE,
        DUNGEON_NAME, MONSTERS, TIME, PARTY_LABEL, PARTY_MEMBER_1, PARTY_MEMBER_2, PARTY_MEMBER_3, PARTY_MEMBER_4, PARTY_MEMBER_5
    }

    private final JavaPlugin plugin;
    private final LevelModule levelModule;
    private final EconomyHelper economyHelper;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private ChatFormatter chatFormatter;
    private DungeonManager dungeonManager;

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Set<UUID> onlinePlayerUUIDs = new HashSet<>();
    private final Map<UUID, Map<ScoreboardLine, Team>> playerLineTeams = new HashMap<>();
    private final Map<UUID, Map<ScoreboardLine, String>> playerLineCache = new HashMap<>();
    private final AtomicBoolean isScheduled = new AtomicBoolean(false);
    private static final ChatColor[] ENTRY_COLORS = ChatColor.values();

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
            initializePlayerTeams(scoreboard, player);

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
    }

    private void initializePlayerTeams(Scoreboard scoreboard, Player player) {
        try {
            Map<ScoreboardLine, Team> lineTeams = new EnumMap<>(ScoreboardLine.class);
            Map<ScoreboardLine, String> lineCache = new EnumMap<>(ScoreboardLine.class);

            Objective objective = scoreboard.getObjective("mysticSidebar");
            int scoreValue = 15;

            for (ScoreboardLine line : ScoreboardLine.values()) {
                String entryKey = getEntryKey(line);
                String teamName = player.getUniqueId() + "_" + line.name();

                Team team = scoreboard.registerNewTeam(teamName);
                team.addEntry(entryKey);
                objective.getScore(entryKey).setScore(scoreValue--);

                lineTeams.put(line, team);
                lineCache.put(line, "");
            }

            playerLineTeams.put(player.getUniqueId(), lineTeams);
            playerLineCache.put(player.getUniqueId(), lineCache);
        } catch (Exception e) {
            DebugLogger.getInstance().log(Level.SEVERE, "Failed to initialize teams for " + player.getName() + ": " + e.getMessage());
        }
    }

    private String getEntryKey(ScoreboardLine line) {
        int ordinal = line.ordinal();
        int color1 = ordinal % ENTRY_COLORS.length;
        int color2 = (ordinal / ENTRY_COLORS.length) % ENTRY_COLORS.length;
        return ENTRY_COLORS[color1].toString() + ENTRY_COLORS[color2].toString() + ChatColor.RESET;
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

        if (instance != null) {
            updateDungeonScoreboard(player, instance, scoreboard);
        } else {
            updateRegularScoreboard(player, scoreboard);
        }

        if (player.getWorld().getName().equals("world")) {
            player.setScoreboard(scoreboard);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void updateRegularScoreboard(Player player, Scoreboard scoreboard) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
        if (playerData == null) return;

        Map<ScoreboardLine, String> cache = playerLineCache.get(uuid);
        Map<ScoreboardLine, Team> teams = playerLineTeams.get(uuid);

        if (cache == null || teams == null) {
            DebugLogger.getInstance().warning("Scoreboard not initialized properly for " + player.getName());
            return;
        }

        updateLine(teams, cache, ScoreboardLine.SEPARATOR_1, "げげ");
        updateLine(teams, cache, ScoreboardLine.EMPTY_1, "  ");
        updateLine(teams, cache, ScoreboardLine.YOUR_STATS, "ぁ YOUR STATS");

        int level = playerData.getLevel();
        updateLine(teams, cache, ScoreboardLine.LEVEL, generateLevelText(level));

        int currentXp = playerData.getXp();
        int xpNeeded = levelModule != null ? levelModule.getLevelThreshold(level + 1) : 100;
        updateLine(teams, cache, ScoreboardLine.XP,
                ChatColor.RED + "   あ " + ChatColor.WHITE + "XP " +
                        ChatColor.GREEN + currentXp + ChatColor.WHITE + "/" + ChatColor.GOLD + xpNeeded);

        int balance = economyHelper != null ? economyHelper.getHeldGold(player) : 0;
        updateLine(teams, cache, ScoreboardLine.BALANCE,
                ChatColor.YELLOW + "   い " + ChatColor.WHITE + balance + ChatColor.GOLD + " held Gold");

        updateLine(teams, cache, ScoreboardLine.EMPTY_2, "   ");
        updateLine(teams, cache, ScoreboardLine.INFORMATION, "ぇ INFORMATION");
        updateLine(teams, cache, ScoreboardLine.RANK, "   え " + chatFormatter.getPrefixFromLuckPerms(player));

        String petText = "   シ " + (playerData.getEquippedPet() != null ?
                formatPetName(playerData.getEquippedPet()) : ChatColor.RED + "No Pet");
        updateLine(teams, cache, ScoreboardLine.PET, petText);

        String questText = "   う " + (playerData.getPinnedQuest() != null ?
                ChatColor.GREEN + getCurrentObjective(player) : ChatColor.RED + "No Pinned Quest");
        updateLine(teams, cache, ScoreboardLine.QUEST, questText);

        updateLine(teams, cache, ScoreboardLine.EMPTY_3, "    ");
        updateLine(teams, cache, ScoreboardLine.SEPARATOR_2, "げげ ");
        updateLine(teams, cache, ScoreboardLine.WEBSITE, ChatColor.DARK_GRAY + "さ");
    }

    private void updateDungeonScoreboard(Player player, DungeonInstance instance, Scoreboard scoreboard) {
        UUID uuid = player.getUniqueId();
        Map<ScoreboardLine, String> cache = playerLineCache.get(uuid);
        Map<ScoreboardLine, Team> teams = playerLineTeams.get(uuid);

        if (cache == null || teams == null) {
            DebugLogger.getInstance().warning("Scoreboard not initialized properly for " + player.getName());
            return;
        }

        int minutes = instance.getTimeSpent() / 60;
        int seconds = instance.getTimeSpent() % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        updateLine(teams, cache, ScoreboardLine.DUNGEON_NAME, ChatColor.GOLD + instance.getConfig().getName());
        updateLine(teams, cache, ScoreboardLine.MONSTERS,
                ChatColor.GREEN + "Monsters: " + ChatColor.WHITE + instance.getMonstersKilledByPlayer(player));
        updateLine(teams, cache, ScoreboardLine.TIME,
                ChatColor.YELLOW + "Time: " + ChatColor.WHITE + formattedTime);
        updateLine(teams, cache, ScoreboardLine.PARTY_LABEL, ChatColor.AQUA + "Party Members:");

        List<Player> partyMembers = instance.getPartyMembers();
        for (int i = 0; i < 5; i++) {
            ScoreboardLine line = ScoreboardLine.values()[ScoreboardLine.PARTY_MEMBER_1.ordinal() + i];
            if (i < partyMembers.size()) {
                updateLine(teams, cache, line, ChatColor.GRAY + " - " + partyMembers.get(i).getName());
            } else {
                updateLine(teams, cache, line, "");
            }
        }
    }

    private void updateLine(Map<ScoreboardLine, Team> teams, Map<ScoreboardLine, String> cache,
                            ScoreboardLine line, String newValue) {
        String current = cache.get(line);
        if (current == null || !current.equals(newValue)) {
            Team team = teams.get(line);
            if (team != null) {
                team.setPrefix(newValue);
                cache.put(line, newValue);
            }
        }
    }

    private String generateLevelText(int level) {
        if (level >= 25) {
            return "   あ LEVEL " + ChatColor.RED + "MAXEDぅ";
        }

        ChatColor color = ChatColor.WHITE;
        if (level > 20) {
            color = ChatColor.GOLD;
        } else if (level > 10) {
            color = ChatColor.GREEN;
        }
        return "   あ LEVEL " + color + level;
    }

    private String getCurrentObjective(Player player) {
        if (questManager == null) return "No Objective";
        return questManager.getCurrentObjective(player.getUniqueId());
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
        return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "";
    }

    public int getPlayerLevel(UUID uuid) {
        if (playerDataCache == null) return 1;
        PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
        return playerData != null ? playerData.getLevel() : 1;
    }

    public String formatPetName(String petId) {
        if (petId == null || petId.isEmpty()) {
            return "Unknown Pet";
        }
        String[] words = petId.split("_");
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return formattedName.toString().trim();
    }

    public void cleanup() {
        for (Scoreboard scoreboard : playerScoreboards.values()) {
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
            for (Objective objective : scoreboard.getObjectives()) {
                objective.unregister();
            }
        }
        playerScoreboards.clear();
        playerLineTeams.clear();
        playerLineCache.clear();
        onlinePlayerUUIDs.clear();
        DebugLogger.getInstance().log("[MysticRPG] ScoreboardManager cleanup completed.");
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
        }
        playerLineTeams.remove(departingUUID);
        playerLineCache.remove(departingUUID);
    }

    public void createOrUpdateTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = "team_" + uuid;
        String prefix = getPrefixFromLuckPerms(player);
        String suffix = ChatColor.BLACK + " [LVL" + getPlayerLevel(uuid) + "]";

        for (Scoreboard scoreboard : playerScoreboards.values()) {
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            if (!team.getPrefix().equals(prefix) || !team.getSuffix().equals(suffix)) {
                team.setPrefix(prefix);
                team.setSuffix(suffix);
            }
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }
    }
}