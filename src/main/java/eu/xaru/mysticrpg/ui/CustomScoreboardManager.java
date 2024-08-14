package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.party.Party;
import eu.xaru.mysticrpg.party.PartyManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomScoreboardManager {
    private final Main plugin;
    private final PartyManager partyManager;
    private final Map<UUID, CachedPlayerData> playerDataCache = new HashMap<>();

    public CustomScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    public void updateScoreboard(Player player, PlayerData playerData) {
        // Retrieve current data
        int currentLevel = playerData.getLevel();
        double currentBalance = playerData.getBalance();
        String currentQuest = "NONE"; // Placeholder as quests are not yet implemented
        Party party = partyManager.getParty(player);
        String partyLeader = (party != null) ? String.valueOf(party.getLeader()) : "NO PARTY";
        List<String> currentPartyMembers = (party != null && !party.getMemberListWithRoles().isEmpty())
                ? party.getMemberListWithRoles()
                : List.of("EMPTY", "EMPTY");

        CachedPlayerData newData = new CachedPlayerData(currentLevel, currentBalance, currentQuest, currentPartyMembers);

        // Retrieve the cached data
        UUID playerUUID = player.getUniqueId();
        CachedPlayerData cachedData = playerDataCache.get(playerUUID);

        if (partyManager == null) {
            plugin.getLogger().severe("PartyManager is not initialized!");
            return;
        }

        // Update scoreboard only if there's a change
        if (cachedData == null || !newData.equals(cachedData)) {
            playerDataCache.put(playerUUID, newData); // Cache the new data

            ScoreboardManager manager = plugin.getServer().getScoreboardManager();
            Scoreboard board = manager.getNewScoreboard();

            // Set the custom title with special characters
            String title = ChatColor.DARK_PURPLE + "ᴍʏꜱᴛɪᴄ" + ChatColor.RED + "ʀᴘɢ";
            Objective objective = board.registerNewObjective("MysticRPG", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            int line = 14;

            // Setting the lines according to the provided layout
            objective.getScore(ChatColor.DARK_GRAY + "◆" + ChatColor.STRIKETHROUGH + "                                  " + ChatColor.RESET + ChatColor.DARK_GRAY + "◆").setScore(line--);
           // objective.getScore(ChatColor.of("#E5FBFD") + ChatColor.BOLD.toString() + "ᴘʟᴀʏᴇʀ").setScore(line--);
            objective.getScore(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "ʟᴇᴠᴇʟ: " + ChatColor.GREEN + currentLevel).setScore(line--);
            objective.getScore(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "ᴍᴏɴᴇʏ: " + ChatColor.GREEN + "$" + currentBalance).setScore(line--);
            objective.getScore(" ").setScore(line--);
         //   objective.getScore(ChatColor.of("#E5FBFD") + "Qᴜᴇꜱᴛꜱ").setScore(line--);
            objective.getScore(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "ᴄᴜʀʀᴇɴᴛ: " + ChatColor.WHITE + currentQuest).setScore(line--);
            objective.getScore("  ").setScore(line--);
            objective.getScore(ChatColor.GRAY + "ᴘᴀʀᴛʏ").setScore(line--);
            objective.getScore(ChatColor.DARK_GRAY + " ▪ " + ChatColor.WHITE + partyLeader).setScore(line--);
            for (String member : currentPartyMembers) {
                objective.getScore(ChatColor.DARK_GRAY + " ▪ " + member).setScore(line--);
            }
            // Ensure to fill in empty lines if there are fewer than 3 party members
            while (line > 2) {
                objective.getScore(ChatColor.DARK_GRAY + " ▪ EMPTY").setScore(line--);
            }
            objective.getScore(ChatColor.DARK_GRAY + "◆" + ChatColor.STRIKETHROUGH + "                                  " + ChatColor.RESET + ChatColor.DARK_GRAY + "◆").setScore(line--);

            // Setting scoreboard to player
            player.setScoreboard(board);
        }
    }
}
