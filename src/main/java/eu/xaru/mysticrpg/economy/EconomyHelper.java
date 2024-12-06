package eu.xaru.mysticrpg.economy;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyHelper {

    private final PlayerDataCache playerDataCache;
    private static final Logger logger = Logger.getLogger(EconomyHelper.class.getName());

    public EconomyHelper(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
    }

    /**
     * Retrieves the current balance of a player.
     *
     * @param player The player whose balance is to be retrieved.
     * @return The current balance.
     */
    public double getBalance(Player player) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            logger.log(Level.WARNING, "PlayerData not found for player: {0}", player.getName());
            return 0;
        }
        return playerData.getBalance();
    }

    /**
     * Sets the balance of a player to a specific amount.
     *
     * @param player The player whose balance is to be set.
     * @param amount The new balance amount.
     */
    public void setBalance(Player player, double amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            logger.log(Level.WARNING, "PlayerData not found for player: {0}", player.getName());
            return;
        }
        playerData.setBalance(amount);
        logger.log(Level.INFO, "Set balance for player {0} to ${1}", new Object[]{player.getName(), amount});
    }

    /**
     * Deposits a specified amount to a player's balance.
     *
     * @param player The player to deposit funds to.
     * @param amount The amount to deposit.
     * @return True if the deposit was successful, false otherwise.
     */
    public boolean depositBalance(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(Utils.getInstance().$("Deposit amount must be positive."));
            logger.log(Level.WARNING, "Attempted to deposit a non-positive amount: ${0} to player: {1}", new Object[]{amount, player.getName()});
            return false;
        }

        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Player data not found."));
            logger.log(Level.SEVERE, "PlayerData not found for player: {0}", player.getName());
            return false;
        }

        double newBalance = playerData.getBalance() + amount;
        playerData.setBalance(newBalance);

        player.sendMessage(Utils.getInstance().$("Your balance has been increased by $" + formatBalance(amount) + ". New balance: $" + formatBalance(newBalance)));
        logger.log(Level.INFO, "Deposited ${0} to player {1}. New balance: ${2}", new Object[]{amount, player.getName(), newBalance});
        return true;
    }

    /**
     * Withdraws a specified amount from a player's balance.
     *
     * @param player The player to withdraw funds from.
     * @param amount The amount to withdraw.
     * @return True if the withdrawal was successful, false otherwise.
     */
    public boolean withdrawBalance(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(Utils.getInstance().$("Withdrawal amount must be positive."));
            logger.log(Level.WARNING, "Attempted to withdraw a non-positive amount: ${0} from player: {1}", new Object[]{amount, player.getName()});
            return false;
        }

        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Player data not found."));
            logger.log(Level.SEVERE, "PlayerData not found for player: {0}", player.getName());
            return false;
        }

        double currentBalance = playerData.getBalance();
        if (currentBalance < amount) {
            player.sendMessage(Utils.getInstance().$("Insufficient funds."));
            logger.log(Level.WARNING, "Player {0} has insufficient funds. Balance: ${1}, Attempted withdrawal: ${2}",
                    new Object[]{player.getName(), currentBalance, amount});
            return false;
        }

        double newBalance = currentBalance - amount;
        playerData.setBalance(newBalance);

        player.sendMessage(Utils.getInstance().$("Your balance has been decreased by $" + formatBalance(amount) + ". New balance: $" + formatBalance(newBalance)));
        logger.log(Level.INFO, "Withdrew ${0} from player {1}. New balance: ${2}", new Object[]{amount, player.getName(), newBalance});
        return true;
    }

    /**
     * Adds (or subtracts) balance for a player.
     * This method is deprecated and should be replaced with depositBalance and withdrawBalance.
     *
     * @param player The player whose balance is to be adjusted.
     * @param amount The amount to add (positive) or subtract (negative).
     */
    @Deprecated
    public void addBalance(Player player, double amount) {
        if (amount > 0) {
            depositBalance(player, amount);
        } else if (amount < 0) {
            withdrawBalance(player, -amount);
        }
        // If amount == 0, do nothing
    }

    /**
     * Sends money from one player to another.
     *
     * @param sender    The player sending money.
     * @param receiver  The player receiving money.
     * @param amount    The amount to send.
     */
    public void sendMoney(Player sender, Player receiver, double amount) {
        if (amount <= 0) {
            sender.sendMessage(Utils.getInstance().$("Amount must be positive."));
            logger.log(Level.WARNING, "Player {0} attempted to send a non-positive amount: ${1} to {2}",
                    new Object[]{sender.getName(), amount, receiver.getName()});
            return;
        }

        PlayerData senderData = playerDataCache.getCachedPlayerData(sender.getUniqueId());
        PlayerData receiverData = playerDataCache.getCachedPlayerData(receiver.getUniqueId());

        if (senderData == null || receiverData == null) {
            sender.sendMessage(Utils.getInstance().$("Failed to find data for either sender or receiver."));
            logger.log(Level.SEVERE, "PlayerData not found for sender: {0} or receiver: {1}", new Object[]{sender.getName(), receiver.getName()});
            return;
        }

        if (senderData.getBalance() >= amount) {
            boolean withdrawn = withdrawBalance(sender, amount);
            if (!withdrawn) {
                sender.sendMessage(Utils.getInstance().$("Transaction failed: Unable to deduct money."));
                logger.log(Level.SEVERE, "Failed to withdraw ${0} from sender {1}", new Object[]{amount, sender.getName()});
                return;
            }

            boolean deposited = depositBalance(receiver, amount);
            if (!deposited) {
                // Refund the sender if depositing fails
                depositBalance(sender, amount);
                sender.sendMessage(Utils.getInstance().$("Transaction failed: Unable to credit receiver."));
                logger.log(Level.SEVERE, "Failed to deposit ${0} to receiver {1}. Refunded ${0} to sender {2}",
                        new Object[]{amount, receiver.getName(), sender.getName()});
                return;
            }

            sender.sendMessage(Utils.getInstance().$("You sent $" + formatBalance(amount) + " to " + receiver.getName()));
            receiver.sendMessage(Utils.getInstance().$("You received $" + formatBalance(amount) + " from " + sender.getName()));
            logger.log(Level.INFO, "Player {0} sent ${1} to player {2}", new Object[]{sender.getName(), amount, receiver.getName()});
        } else {
            sender.sendMessage(Utils.getInstance().$("Insufficient funds."));
            logger.log(Level.WARNING, "Player {0} has insufficient funds. Balance: ${1}, Attempted to send: ${2}",
                    new Object[]{sender.getName(), senderData.getBalance(), amount});
        }
    }

    /**
     * Formats the balance into a readable string.
     *
     * @param balance The balance amount.
     * @return A formatted string representing the balance.
     */
    public String formatBalance(double balance) {
        return String.format("%,.2f", balance);
    }
}
