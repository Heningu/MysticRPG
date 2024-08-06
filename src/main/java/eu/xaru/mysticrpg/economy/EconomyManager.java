package eu.xaru.mysticrpg.economy;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager {
    private final PlayerDataManager playerDataManager;

    public EconomyManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public double getBalance(Player player) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        return playerData.getBalance();
    }

    public void setBalance(Player player, double amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        playerData.setBalance(amount);
        playerDataManager.save(player);
    }

    public void addBalance(Player player, double amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        playerData.setBalance(playerData.getBalance() + amount);
        playerDataManager.save(player);
    }

    public void sendMoney(Player sender, Player receiver, double amount) {
        PlayerData senderData = playerDataManager.getPlayerData(sender);
        PlayerData receiverData = playerDataManager.getPlayerData(receiver);

        if (senderData.getBalance() >= amount) {
            senderData.setBalance(senderData.getBalance() - amount);
            receiverData.setBalance(receiverData.getBalance() + amount);

            playerDataManager.save(sender);
            playerDataManager.save(receiver);
        } else {
            sender.sendMessage("Insufficient funds.");
        }
    }

    public List<Map.Entry<Player, Double>> getTopRichestPlayers() {
        Map<Player, Double> playerBalances = new HashMap<>();
        for (Player player : playerDataManager.getAllPlayers()) {
            playerBalances.put(player, getBalance(player));
        }

        return playerBalances.entrySet().stream()
                .sorted(Map.Entry.<Player, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    // Future-proof method for displaying player's money on a scoreboard
    public String getMoneyDisplay(Player player) {
        return "Balance: $" + getBalance(player);
    }
}
