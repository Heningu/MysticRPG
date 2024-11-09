package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Helper class that manages friend operations such as sending, accepting, denying, and removing friends.
 */
public class FriendsHelper {

    private final PlayerDataCache playerDataCache;
    private final DebugLoggerModule logger;

    /**
     * Constructor to initialize FriendsHelper with required dependencies.
     *
     * @param playerDataCache The cache containing player data.
     * @param logger          The debug logger for logging purposes.
     */
    public FriendsHelper(PlayerDataCache playerDataCache, DebugLoggerModule logger) {
        this.playerDataCache = playerDataCache;
        this.logger = logger;
    }

    /**
     * Sends a friend request from the sender to the receiver.
     *
     * @param sender   The player sending the friend request.
     * @param receiver The player receiving the friend request.
     */
    public void sendFriendRequest(Player sender, Player receiver) {
        UUID senderUUID = sender.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();

        // Prevent players from sending friend requests to themselves
        if (senderUUID.equals(receiverUUID)) {
            sender.sendMessage(ChatColor.RED + "You cannot add yourself as a friend.");
            return;
        }

        PlayerData senderData = playerDataCache.getCachedPlayerData(senderUUID);
        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);

        // Check if player data is accessible
        if (senderData == null || receiverData == null) {
            sender.sendMessage(ChatColor.RED + "An error occurred while accessing player data.");
            return;
        }

        // Check if the receiver has blocked the sender
        if (receiverData.getBlockedPlayers().contains(senderUUID.toString())) {
            sender.sendMessage(ChatColor.RED + "You cannot send a friend request to this player.");
            return;
        }

        // Check if the players are already friends
        if (receiverData.getFriends().contains(senderUUID.toString())) {
            sender.sendMessage(ChatColor.RED + "You are already friends with this player.");
            return;
        }

        // Check if a friend request has already been sent
        if (receiverData.getFriendRequests().contains(senderUUID.toString())) {
            sender.sendMessage(ChatColor.RED + "You have already sent a friend request to this player.");
            return;
        }

        // Add the friend request
        receiverData.getFriendRequests().add(senderUUID.toString());
        sender.sendMessage(ChatColor.GREEN + "Friend request sent to " + receiver.getName() + ".");

        // Notify the receiver of the incoming friend request
        receiver.sendMessage(ChatColor.YELLOW + sender.getName() + " has sent you a friend request.");

        // Create clickable [Accept] and [Deny] buttons using TextComponent
        TextComponent acceptButton = new TextComponent("[Accept]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends accept " + sender.getName()));

        TextComponent denyButton = new TextComponent("[Deny]");
        denyButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends deny " + sender.getName()));

        TextComponent separator = new TextComponent(" ");

        TextComponent message = new TextComponent();
        message.addExtra(acceptButton);
        message.addExtra(separator);
        message.addExtra(denyButton);

        // Send the clickable message to the receiver
        receiver.spigot().sendMessage(message);

        logger.log(sender.getName() + " sent a friend request to " + receiver.getName());
    }

    /**
     * Accepts a friend request from the specified sender.
     *
     * @param receiver   The player accepting the friend request.
     * @param senderName The name of the player who sent the friend request.
     */
    public void acceptFriendRequest(Player receiver, String senderName) {
        OfflinePlayer sender = Bukkit.getOfflinePlayer(senderName);
        if (sender == null || sender.getUniqueId() == null) {
            receiver.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        UUID receiverUUID = receiver.getUniqueId();
        UUID senderUUID = sender.getUniqueId();

        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);
        PlayerData senderData = playerDataCache.getCachedPlayerData(senderUUID);

        // Check if player data is accessible
        if (receiverData == null || senderData == null) {
            receiver.sendMessage(ChatColor.RED + "An error occurred while accessing player data.");
            return;
        }

        // Check if there is a pending friend request from the sender
        if (!receiverData.getFriendRequests().contains(senderUUID.toString())) {
            receiver.sendMessage(ChatColor.RED + "You have no friend request from this player.");
            return;
        }

        // Remove the friend request
        receiverData.getFriendRequests().remove(senderUUID.toString());

        // Add each other as friends
        receiverData.getFriends().add(senderUUID.toString());
        senderData.getFriends().add(receiverUUID.toString());

        receiver.sendMessage(ChatColor.GREEN + "You are now friends with " + sender.getName() + ".");

        // Notify the sender if they are online
        if (sender.isOnline()) {
            Player senderPlayer = sender.getPlayer();
            if (senderPlayer != null) {
                senderPlayer.sendMessage(ChatColor.GREEN + receiver.getName() + " has accepted your friend request.");
            }
        }

        logger.log(receiver.getName() + " accepted the friend request from " + sender.getName());
    }

    /**
     * Denies a friend request from the specified sender.
     *
     * @param receiver   The player denying the friend request.
     * @param senderName The name of the player who sent the friend request.
     */
    public void denyFriendRequest(Player receiver, String senderName) {
        OfflinePlayer sender = Bukkit.getOfflinePlayer(senderName);
        if (sender == null || sender.getUniqueId() == null) {
            receiver.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        UUID receiverUUID = receiver.getUniqueId();
        UUID senderUUID = sender.getUniqueId();

        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);

        // Check if player data is accessible
        if (receiverData == null) {
            receiver.sendMessage(ChatColor.RED + "An error occurred while accessing player data.");
            return;
        }

        // Check if there is a pending friend request from the sender
        if (!receiverData.getFriendRequests().contains(senderUUID.toString())) {
            receiver.sendMessage(ChatColor.RED + "You have no friend request from this player.");
            return;
        }

        // Remove the friend request
        receiverData.getFriendRequests().remove(senderUUID.toString());

        receiver.sendMessage(ChatColor.YELLOW + "You have denied the friend request from " + sender.getName() + ".");

        // Notify the sender if they are online
        if (sender.isOnline()) {
            Player senderPlayer = sender.getPlayer();
            if (senderPlayer != null) {
                senderPlayer.sendMessage(ChatColor.RED + receiver.getName() + " has denied your friend request.");
            }
        }

        logger.log(receiver.getName() + " denied the friend request from " + sender.getName());
    }

    /**
     * Removes a friend from the player's friends list.
     *
     * @param player  The player removing the friend.
     * @param target The friend to be removed.
     */
    public void removeFriend(Player player, OfflinePlayer target) {
        UUID playerUUID = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        PlayerData targetData = playerDataCache.getCachedPlayerData(targetUUID);

        // Check if player data is accessible
        if (playerData == null || targetData == null) {
            player.sendMessage(ChatColor.RED + "An error occurred while accessing player data.");
            return;
        }

        // Check if the target is actually a friend
        if (!playerData.getFriends().contains(targetUUID.toString())) {
            player.sendMessage(ChatColor.RED + "This player is not in your friends list.");
            return;
        }

        // Remove each other from friends lists
        playerData.getFriends().remove(targetUUID.toString());
        targetData.getFriends().remove(playerUUID.toString());

        player.sendMessage(ChatColor.YELLOW + "You have removed " + target.getName() + " from your friends list.");

        // Notify the target player if they are online
        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has removed you from their friends list.");
            }
        }

        logger.log(player.getName() + " removed " + target.getName() + " from their friends list.");
    }
}
