package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.guis.player.TradeGUI;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages all pending trade requests.
 */
public class TradeRequestManager {

    private final TradeManager tradeManager;

    // Map of TradeRequestKey -> TradeRequest
    private final Map<TradeRequestKey, TradeRequest> pendingRequests = new HashMap<>();

    /**
     * Constructor to initialize TradeRequestManager.
     *
     * @param tradeManager The TradeManager instance.
     */
    public TradeRequestManager(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    /**
     * Creates a new trade request and schedules a timeout.
     *
     * @param initiator The player initiating the trade.
     * @param target    The target player to trade with.
     */
    public void createTradeRequest(Player initiator, Player target) {
        // Prevent trade requests if either player is already trading
        if (tradeManager.isTrading(initiator)) {
            initiator.sendMessage(ChatColor.RED + "You are already in a trade.");
            return;
        }

        if (tradeManager.isTrading(target)) {
            initiator.sendMessage(ChatColor.RED + target.getName() + " is already in a trade.");
            return;
        }

        // Check if the target already has any pending trade request
        for (TradeRequestKey key : pendingRequests.keySet()) {
            if (key.getTargetUUID().equals(target.getUniqueId())) {
                initiator.sendMessage(ChatColor.RED + "There is already a pending trade request to " + target.getName() + ".");
                return;
            }
        }

        TradeRequestKey key = new TradeRequestKey(initiator.getUniqueId(), target.getUniqueId());
        if (pendingRequests.containsKey(key)) {
            initiator.sendMessage(ChatColor.RED + "You already have a pending trade request to " + target.getName() + ".");
            return;
        }

        TradeRequest tradeRequest = new TradeRequest(initiator, target, this);
        pendingRequests.put(key, tradeRequest);
        tradeRequest.scheduleTimeout();
    }

    /**
     * Handles accepting a trade request.
     *
     * @param initiatorName The name of the initiator.
     * @param target        The player accepting the trade.
     */
    public void acceptTradeRequest(String initiatorName, Player target) {
        Player initiator = Bukkit.getPlayerExact(initiatorName);
        if (initiator == null) {
            target.sendMessage(ChatColor.RED + "The initiator is no longer online.");
            return;
        }

        TradeRequestKey key = new TradeRequestKey(initiator.getUniqueId(), target.getUniqueId());
        TradeRequest tradeRequest = pendingRequests.get(key);
        if (tradeRequest == null) {
            target.sendMessage(ChatColor.RED + "No pending trade request from " + initiatorName + ".");
            return;
        }

        // Mark the request as handled to prevent timeout
        tradeRequest.markHandled();

        // Remove the request to prevent duplication
        pendingRequests.remove(key);

        // Start the trade
        tradeManager.startTrade(initiator, target);

        // Notify both players
        initiator.sendMessage(ChatColor.GREEN + target.getName() + " has accepted your trade request.");
        target.sendMessage(ChatColor.GREEN + "You have accepted the trade request from " + initiator.getName() + ".");

        // Open TradeGUI for both players
        TradeSession session = tradeManager.getTradeSession(initiator);
        if (session != null) {
            try {
                TradeGUI tradeGUI1 = new TradeGUI(session, initiator);
                tradeGUI1.open();
                DebugLogger.getInstance().log(Level.INFO, "TradeGUI opened for " + initiator.getName(), 0);
            } catch (Exception e) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to open TradeGUI for " + initiator.getName() + ": " + e.getMessage(), 0);
                initiator.sendMessage(ChatColor.RED + "An error occurred while opening the trade interface.");
            }

            try {
                TradeGUI tradeGUI2 = new TradeGUI(session, target);
                tradeGUI2.open();
                DebugLogger.getInstance().log(Level.INFO, "TradeGUI opened for " + target.getName(), 0);
            } catch (Exception e) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to open TradeGUI for " + target.getName() + ": " + e.getMessage(), 0);
                target.sendMessage(ChatColor.RED + "An error occurred while opening the trade interface.");
            }
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "TradeSession not found for " + initiator.getName() + " and " + target.getName(), 0);
        }
    }

    /**
     * Handles declining a trade request.
     *
     * @param initiatorName The name of the initiator.
     * @param target        The player declining the trade.
     */
    public void declineTradeRequest(String initiatorName, Player target) {
        Player initiator = Bukkit.getPlayerExact(initiatorName);
        if (initiator == null) {
            target.sendMessage(ChatColor.RED + "The initiator is no longer online.");
            return;
        }

        TradeRequestKey key = new TradeRequestKey(initiator.getUniqueId(), target.getUniqueId());
        TradeRequest tradeRequest = pendingRequests.get(key);
        if (tradeRequest == null) {
            target.sendMessage(ChatColor.RED + "No pending trade request from " + initiatorName + ".");
            return;
        }

        // Mark the request as handled to prevent timeout
        tradeRequest.markHandled();

        // Remove the request
        pendingRequests.remove(key);

        // Notify both players
        initiator.sendMessage(ChatColor.RED + target.getName() + " has declined your trade request.");
        target.sendMessage(ChatColor.RED + "You have declined the trade request from " + initiator.getName() + ".");
    }

    /**
     * Handles the timeout of a trade request.
     *
     * @param tradeRequest The trade request that timed out.
     */
    public void handleTimeout(TradeRequest tradeRequest) {
        Player initiator = tradeRequest.getInitiator();
        Player target = tradeRequest.getTarget();

        // Remove the request
        TradeRequestKey key = new TradeRequestKey(initiator.getUniqueId(), target.getUniqueId());
        pendingRequests.remove(key);

        // Notify both players
        if (initiator != null && initiator.isOnline()) {
            initiator.sendMessage(ChatColor.RED + "Trade request to " + target.getName() + " has timed out.");
        }
        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.RED + "Trade request from " + initiator.getName() + " has timed out.");
        }
    }
}
