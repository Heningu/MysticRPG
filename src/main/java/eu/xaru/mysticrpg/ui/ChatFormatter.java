package eu.xaru.mysticrpg.ui;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * ChatFormatter handles the formatting of chat messages to include player prefixes and suffixes.
 */
public class ChatFormatter implements Listener {

    private final LuckPerms luckPerms;
    private final PlayerDataCache playerDataCache;

    /**
     * Constructs a new ChatFormatter instance.
     */
    public ChatFormatter() {
        this.luckPerms = LuckPermsProvider.get();
        this.playerDataCache = PlayerDataCache.getInstance();
    }

    /**
     * Handles the AsyncPlayerChatEvent to format chat messages with prefix and suffix.
     *
     * @param event The AsyncPlayerChatEvent.
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Retrieve prefix from LuckPerms
        String prefix = getPrefixFromLuckPerms(player);

        // Retrieve suffix from PlayerDataCache (e.g., level)
        String suffix = getSuffixFromPlayerData(uuid);

        // Construct the chat format: [prefix] username [suffix]: message
        String format = "";
        if (!prefix.isEmpty()) {
            format += prefix + " ";
        }
        format += "%1$s" + " ";
        if (!suffix.isEmpty()) {
            format += suffix + " ";
        }
        format += ": %2$s";

        // Apply color codes
        format = ChatColor.translateAlternateColorCodes('&', format);

        event.setFormat(format);
    }

    /**
     * Retrieves the prefix from LuckPerms for the specified player.
     *
     * @param player The player whose prefix is to be retrieved.
     * @return The prefix string, or an empty string if not set.
     */
    public String getPrefixFromLuckPerms(Player player) {
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
     * Retrieves the suffix from PlayerDataCache (e.g., player level).
     *
     * @param uuid The UUID of the player.
     * @return The suffix string, or an empty string if not set.
     */
    private String getSuffixFromPlayerData(UUID uuid) {
        if (playerDataCache == null) {
            return "";
        }

        PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
        if (playerData == null) {
            return "";
        }

        int level = playerData.getLevel();
        return ChatColor.BLACK + "[" + ChatColor.GREEN + "LVL" + ChatColor.RED + level + ChatColor.BLACK + "]&r";
    }
}
