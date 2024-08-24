package eu.xaru.mysticrpg.social.friends;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendsHelper {

    private final PlayerDataCache playerDataCache;
    private final DebugLoggerModule logger;
    private final FriendsInventory friendsInventory;

    public FriendsHelper(PlayerDataCache playerDataCache, DebugLoggerModule logger) {
        this.playerDataCache = playerDataCache;
        this.logger = logger;
        this.friendsInventory = new FriendsInventory(playerDataCache);  // Correct instantiation with PlayerDataCache
        registerFriendsCommand();
    }

    public void sendFriendRequest(Player sender, Player receiver) {
        if (!receiver.isOnline()) {
            sender.sendMessage("The player " + receiver.getName() + " is not online.");
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();

        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);
        if (receiverData != null) {
            if (receiverData.blockedPlayers.contains(senderUUID.toString())) {
                sender.sendMessage("You cannot send a friend request to " + receiver.getName() + " as you are blocked.");
                return;
            }

            if (receiverData.friendRequests.contains(senderUUID.toString())) {
                sender.sendMessage("Friend request already sent to " + receiver.getName());
                return;
            }

            // Add friend request
            playerDataCache.addFriendRequest(receiverUUID, senderUUID);
            sender.sendMessage("Friend request sent to " + receiver.getName());
            receiver.sendMessage("You have received a friend request from " + sender.getName());
            logger.log("Friend request sent from " + sender.getName() + " to " + receiver.getName());
        } else {
            sender.sendMessage("Failed to send friend request. Please try again later.");
            logger.error("Failed to retrieve player data for " + receiver.getName());
        }
    }

    public void acceptFriendRequest(Player receiver, Player sender) {
        UUID senderUUID = sender.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();

        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);
        if (receiverData != null) {
            if (!receiverData.friendRequests.contains(senderUUID.toString())) {
                receiver.sendMessage("No friend request from " + sender.getName());
                return;
            }

            // Remove friend request, add to friends list
            playerDataCache.removeFriendRequest(receiverUUID, senderUUID);
            playerDataCache.addFriend(receiverUUID, senderUUID);
            playerDataCache.addFriend(senderUUID, receiverUUID);

            receiver.sendMessage("You are now friends with " + sender.getName());
            sender.sendMessage("You are now friends with " + receiver.getName());
            logger.log("Friend request accepted between " + receiver.getName() + " and " + sender.getName());
        } else {
            receiver.sendMessage("Failed to load your data. Please try again later.");
            logger.error("Failed to load player data for " + receiver.getName());
        }
    }

    public void declineFriendRequest(Player receiver, Player sender) {
        UUID senderUUID = sender.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();

        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiverUUID);
        if (receiverData != null) {
            if (!receiverData.friendRequests.contains(senderUUID.toString())) {
                receiver.sendMessage("No friend request from " + sender.getName());
                return;
            }

            // Remove friend request
            playerDataCache.removeFriendRequest(receiverUUID, senderUUID);
            receiver.sendMessage("Friend request from " + sender.getName() + " declined.");
            sender.sendMessage("Your friend request to " + receiver.getName() + " was declined.");
            logger.log("Friend request declined by " + receiver.getName() + " from " + sender.getName());
        } else {
            receiver.sendMessage("Failed to decline friend request. Please try again later.");
            logger.error("Failed to load player data for " + receiver.getName());
        }
    }

    public void blockUser(Player blocker, Player toBlock) {
        UUID blockerUUID = blocker.getUniqueId();
        UUID toBlockUUID = toBlock.getUniqueId();

        playerDataCache.blockPlayer(blockerUUID, toBlockUUID);
        blocker.sendMessage("You have blocked " + toBlock.getName());
        toBlock.sendMessage("You have been blocked by " + blocker.getName());
        logger.log(blocker.getName() + " blocked " + toBlock.getName());
    }

    public void unblockUser(Player blocker, Player toUnblock) {
        UUID blockerUUID = blocker.getUniqueId();
        UUID toUnblockUUID = toUnblock.getUniqueId();

        playerDataCache.unblockPlayer(blockerUUID, toUnblockUUID);
        blocker.sendMessage("You have unblocked " + toUnblock.getName());
        toUnblock.sendMessage("You have been unblocked by " + blocker.getName());
        logger.log(blocker.getName() + " unblocked " + toUnblock.getName());
    }

    private void registerFriendsCommand() {
        new CommandAPICommand("friends")
                .withAliases("friend")
                .withPermission("mysticrpg.friends")
                .executesPlayer((player, args) -> {
                    friendsInventory.openFriendsMenu(player);
                })
                .register();
    }

    private List<String> getOnlinePlayerNames(Player player) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getFriendRequestNames(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        if (playerData != null) {
            return playerData.friendRequests.stream()
                    .map(uuid -> {
                        Player friend = Bukkit.getPlayer(UUID.fromString(uuid));
                        return friend != null ? friend.getName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    private List<String> getBlockedPlayerNames(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        if (playerData != null) {
            return playerData.blockedPlayers.stream()
                    .map(uuid -> {
                        Player blocked = Bukkit.getPlayer(UUID.fromString(uuid));
                        return blocked != null ? blocked.getName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public void openFriendsMenu(Player player) {
        friendsInventory.openFriendsMenu(player);
    }

    public void openFriendRequestsMenu(Player player) {
        player.sendMessage("Opening friend requests menu...");
    }
}
