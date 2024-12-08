// File: eu/xaru/mysticrpg/gui/components/EventDebouncer.java
package eu.xaru.mysticrpg.guis.components;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to debounce GUI click events.
 */
public class EventDebouncer {
    private static final Map<UUID, Long> lastClickTimes = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 200; // 500ms cooldown

    /**
     * Checks if the player can perform an action based on the cooldown.
     *
     * @param player The player to check.
     * @return True if the player can perform the action, false otherwise.
     */
    public boolean canPerform(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastClickTime = lastClickTimes.getOrDefault(playerId, 0L);
        if (currentTime - lastClickTime >= COOLDOWN_MILLIS) {
            lastClickTimes.put(playerId, currentTime);
            return true;
        }
        return false;
    }
}
