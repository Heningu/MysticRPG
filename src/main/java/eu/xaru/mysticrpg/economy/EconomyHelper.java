package eu.xaru.mysticrpg.economy;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import org.bukkit.entity.Player;

public class EconomyHelper {

    private final PlayerDataCache playerDataCache;

    public EconomyHelper(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
    }

    public double getBalance(Player player) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0;
        }
        return playerData.getBalance();
    }

    public void setBalance(Player player, double amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        playerData.setBalance(amount);
    }

    public void addBalance(Player player, double amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        playerData.setBalance(playerData.getBalance() + amount);
    }

    public void sendMoney(Player sender, Player receiver, double amount) {
        PlayerData senderData = playerDataCache.getCachedPlayerData(sender.getUniqueId());
        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiver.getUniqueId());

        if (senderData == null || receiverData == null) {
            sender.sendMessage("Failed to find data for either sender or receiver.");
            return;
        }

        if (senderData.getBalance() >= amount) {
            senderData.setBalance(senderData.getBalance() - amount);
            receiverData.setBalance(receiverData.getBalance() + amount);
            sender.sendMessage("You sent $" + formatBalance(amount) + " to " + receiver.getName());
            receiver.sendMessage("You received $" + formatBalance(amount) + " from " + sender.getName());
        } else {
            sender.sendMessage("Insufficient funds.");
        }
    }

    public String formatBalance(double balance) {
        return String.format("%,.2f", balance);
    }
}
