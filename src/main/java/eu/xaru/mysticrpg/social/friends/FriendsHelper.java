package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FriendsHelper {

    private final SaveModule saveModule;
    private final DebugLoggerModule logger;

    public FriendsHelper(SaveModule saveModule, DebugLoggerModule logger) {
        this.saveModule = saveModule;
        this.logger = logger;
    }

    public void sendFriendRequest(Player sender, Player receiver) {
        PlayerData receiverData = saveModule.getPlayerData(receiver);

        if (receiverData.blockedPlayers().contains(sender.getUniqueId())) {
            sender.sendMessage("You cannot send a friend request to " + receiver.getName() + " as you are blocked.");
            return;
        }

        receiverData.friendRequests().add(sender.getUniqueId());
        saveModule.savePlayerData(receiverData);

        sender.sendMessage("Friend request sent to " + receiver.getName());
        receiver.sendMessage("You have received a friend request from " + sender.getName());
        logger.log("Friend request sent from " + sender.getName() + " to " + receiver.getName());
    }

    public void acceptFriendRequest(Player receiver, Player sender) {
        PlayerData receiverData = saveModule.getPlayerData(receiver);
        PlayerData senderData = saveModule.getPlayerData(sender);

        UUID senderUUID = sender.getUniqueId();

        if (receiverData.friendRequests().contains(senderUUID)) {
            receiverData.friendRequests().remove(senderUUID);
            receiverData.friends().add(senderUUID);
            senderData.friends().add(receiver.getUniqueId());

            saveModule.savePlayerData(receiverData);
            saveModule.savePlayerData(senderData);

            receiver.sendMessage("You are now friends with " + sender.getName());
            sender.sendMessage("You are now friends with " + receiver.getName());
            logger.log("Friend request accepted between " + receiver.getName() + " and " + sender.getName());
        } else {
            receiver.sendMessage("No friend request from " + sender.getName());
        }
    }

    public void declineFriendRequest(Player receiver, Player sender) {
        PlayerData receiverData = saveModule.getPlayerData(receiver);
        UUID senderUUID = sender.getUniqueId();

        if (receiverData.friendRequests().contains(senderUUID)) {
            receiverData.friendRequests().remove(senderUUID);
            saveModule.savePlayerData(receiverData);

            receiver.sendMessage("Friend request from " + sender.getName() + " declined.");
            sender.sendMessage("Your friend request to " + receiver.getName() + " was declined.");
            logger.log("Friend request declined by " + receiver.getName() + " from " + sender.getName());
        } else {
            receiver.sendMessage("No friend request from " + sender.getName());
        }
    }

    public void blockUser(Player blocker, Player toBlock) {
        PlayerData blockerData = saveModule.getPlayerData(blocker);
        blockerData.blockedPlayers().add(toBlock.getUniqueId());
        saveModule.savePlayerData(blockerData);

        blocker.sendMessage("You have blocked " + toBlock.getName());
        toBlock.sendMessage("You have been blocked by " + blocker.getName());
        logger.log(blocker.getName() + " blocked " + toBlock.getName());
    }

    public void unblockUser(Player blocker, Player toUnblock) {
        PlayerData blockerData = saveModule.getPlayerData(blocker);
        UUID toUnblockUUID = toUnblock.getUniqueId();

        if (blockerData.blockedPlayers().contains(toUnblockUUID)) {
            blockerData.blockedPlayers().remove(toUnblockUUID);
            saveModule.savePlayerData(blockerData);

            blocker.sendMessage("You have unblocked " + toUnblock.getName());
            toUnblock.sendMessage("You have been unblocked by " + blocker.getName());
            logger.log(blocker.getName() + " unblocked " + toUnblock.getName());
        } else {
            blocker.sendMessage("Player " + toUnblock.getName() + " is not blocked.");
        }
    }
}
