package eu.xaru.mysticrpg.player.interaction.trading;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all active trade sessions.
 */
public class TradeManager {
    private static TradeManager instance;
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    private TradeManager() {}

    /**
     * Retrieves the TradeManager instance.
     *
     * @return The TradeManager instance.
     */
    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }

    /**
     * Starts a new trade between two players.
     *
     * @param player1 Initiator of the trade.
     * @param player2 Target of the trade.
     */
    public void startTrade(Player player1, Player player2) {
        TradeSession session = new TradeSession(player1, player2);
        activeTrades.put(player1.getUniqueId(), session);
        activeTrades.put(player2.getUniqueId(), session);
    }

    /**
     * Retrieves the trade session a player is involved in.
     *
     * @param player The player to check.
     * @return The TradeSession if the player is trading, otherwise null.
     */
    public TradeSession getTradeSession(Player player) {
        return activeTrades.get(player.getUniqueId());
    }

    /**
     * Ends an active trade session.
     *
     * @param session The trade session to end.
     */
    public void endTrade(TradeSession session) {
        activeTrades.remove(session.getPlayer1().getUniqueId());
        activeTrades.remove(session.getPlayer2().getUniqueId());
    }

    /**
     * Checks if a player is currently involved in a trade.
     *
     * @param player The player to check.
     * @return True if the player is trading, otherwise false.
     */
    public boolean isTrading(Player player) {
        return activeTrades.containsKey(player.getUniqueId());
    }
}
