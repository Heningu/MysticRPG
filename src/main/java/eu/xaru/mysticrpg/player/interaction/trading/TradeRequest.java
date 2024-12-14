package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a trade request between two players.
 */
public class TradeRequest {

    private final Player initiator;
    private final Player target;
    private final TradeRequestManager manager;
    private boolean isHandled = false;

    public TradeRequest(Player initiator, Player target, TradeRequestManager manager) {
        this.initiator = initiator;
        this.target = target;
        this.manager = manager;
    }

    public Player getInitiator() {
        return initiator;
    }

    public Player getTarget() {
        return target;
    }

    /**
     * Schedules a task to handle the timeout after 30 seconds.
     */
    public void scheduleTimeout() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isHandled) {
                    manager.handleTimeout(TradeRequest.this);
                }
            }
        }.runTaskLater(MysticCore.getPlugin(MysticCore.class), 600L); // 30 seconds (20 ticks per second)
    }

    /**
     * Marks the request as handled to prevent timeout handling.
     */
    public void markHandled() {
        isHandled = true;
    }
}
