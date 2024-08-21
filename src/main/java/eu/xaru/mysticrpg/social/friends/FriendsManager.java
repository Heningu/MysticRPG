package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.*;

public class FriendsManager {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;

    public FriendsManager(MysticCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void sendFriendRequest(Player sender, Player receiver) {
        UUID senderUUID = sender.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();

        PlayerData receiverData = playerDataManager.getPlayerData(receiver);

        if (receiverData.getBlockedPlayers().contains(senderUUID)) {
            sender.sendMessage("You cannot send a friend request to " + receiver.getName() + " as you are blocked.");
            return;
        }

        receiverData.addFriendRequest(senderUUID);
        playerDataManager.save(receiver);

        sender.sendMessage("Friend request sent to " + receiver.getName());
        receiver.sendMessage("You have received a friend request from " + sender.getName());
    }

    public void acceptFriendRequest(Player receiver, Player sender) {
        UUID receiverUUID = receiver.getUniqueId();
        UUID senderUUID = sender.getUniqueId();

        PlayerData receiverData = playerDataManager.getPlayerData(receiver);
        PlayerData senderData = playerDataManager.getPlayerData(sender);

        if (receiverData.getFriendRequests().contains(senderUUID)) {
            receiverData.removeFriendRequest(senderUUID);
            receiverData.addFriend(senderUUID);
            senderData.addFriend(receiverUUID);

            playerDataManager.save(receiver);
            playerDataManager.save(sender);

            receiver.sendMessage("You are now friends with " + sender.getName());
            sender.sendMessage("You are now friends with " + receiver.getName());
        } else {
            receiver.sendMessage("No friend request from " + sender.getName());
        }
    }

    public void declineFriendRequest(Player receiver, Player sender) {
        UUID receiverUUID = receiver.getUniqueId();
        UUID senderUUID = sender.getUniqueId();

        PlayerData receiverData = playerDataManager.getPlayerData(receiver);

        if (receiverData.getFriendRequests().contains(senderUUID)) {
            receiverData.removeFriendRequest(senderUUID);
            playerDataManager.save(receiver);

            receiver.sendMessage("Friend request from " + sender.getName() + " declined.");
            sender.sendMessage("Your friend request to " + receiver.getName() + " was declined.");
        } else {
            receiver.sendMessage("No friend request from " + sender.getName());
        }
    }

    public void blockUser(Player blocker, Player toBlock) {
        UUID blockerUUID = blocker.getUniqueId();
        UUID toBlockUUID = toBlock.getUniqueId();

        PlayerData blockerData = playerDataManager.getPlayerData(blocker);

        blockerData.blockPlayer(toBlockUUID);
        playerDataManager.save(blocker);

        blocker.sendMessage("You have blocked " + toBlock.getName());
        toBlock.sendMessage("You have been blocked by " + blocker.getName());
    }

    public void unblockUser(Player blocker, Player toUnblock) {
        UUID blockerUUID = blocker.getUniqueId();
        UUID toUnblockUUID = toUnblock.getUniqueId();

        PlayerData blockerData = playerDataManager.getPlayerData(blocker);

        if (blockerData.getBlockedPlayers().contains(toUnblockUUID)) {
            blockerData.unblockPlayer(toUnblockUUID);
            playerDataManager.save(blocker);

            blocker.sendMessage("You have unblocked " + toUnblock.getName());
            toUnblock.sendMessage("You have been unblocked by " + blocker.getName());
        } else {
            blocker.sendMessage("Player " + toUnblock.getName() + " is not blocked.");
        }
    }

    public Set<UUID> getFriendRequests(Player player) {
        return playerDataManager.getPlayerData(player).getFriendRequests();
    }

    public Set<UUID> getBlockedUsers(Player player) {
        return playerDataManager.getPlayerData(player).getBlockedPlayers();
    }
}
