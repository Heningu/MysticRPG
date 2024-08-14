package eu.xaru.mysticrpg.economy;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import eu.xaru.mysticrpg.ui.CustomScoreboardManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager {
    private final PlayerDataManager playerDataManager;
    private final CustomScoreboardManager scoreboardManager;

    public EconomyManager(PlayerDataManager playerDataManager, CustomScoreboardManager scoreboardManager) {
        this.playerDataManager = playerDataManager;
        this.scoreboardManager = scoreboardManager;
    }

    public double getBalance(Player player) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        return playerData.getBalance();
    }

    public void setBalance(Player player, double amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        playerData.setBalance(amount);
        playerDataManager.save(player);
        scoreboardManager.updateScoreboard(player, playerData); // Update scoreboard after balance change
    }

    public void addBalance(Player player, double amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        playerData.setBalance(playerData.getBalance() + amount);
        playerDataManager.save(player);
        scoreboardManager.updateScoreboard(player, playerData); // Update scoreboard after balance addition
    }

    public void sendMoney(Player sender, Player receiver, double amount) {
        PlayerData senderData = playerDataManager.getPlayerData(sender);
        PlayerData receiverData = playerDataManager.getPlayerData(receiver);

        if (senderData.getBalance() >= amount) {
            senderData.setBalance(senderData.getBalance() - amount);
            receiverData.setBalance(receiverData.getBalance() + amount);

            playerDataManager.save(sender);
            playerDataManager.save(receiver);
            scoreboardManager.updateScoreboard(sender, senderData);  // Update sender's scoreboard
            scoreboardManager.updateScoreboard(receiver, receiverData); // Update receiver's scoreboard
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

    public String getMoneyDisplay(Player player) {
        return "Balance: $" + getBalance(player);
    }
}
