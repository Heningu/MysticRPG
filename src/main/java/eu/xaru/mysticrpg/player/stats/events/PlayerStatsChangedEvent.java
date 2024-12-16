package eu.xaru.mysticrpg.player.stats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever a player's stats or HP changes, prompting UI updates.
 */
public class PlayerStatsChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public PlayerStatsChangedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
