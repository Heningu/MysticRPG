package eu.xaru.mysticrpg.guis.manager;

import eu.xaru.mysticrpg.guis.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/**
 * Singleton class to manage all active GUIs.
 */
public class GUIManager implements Listener {
    private static GUIManager instance;
    private final Map<UUID, Stack<BaseGUI>> playerGUIs = new HashMap<>();

    private GUIManager() {
        // Private constructor for Singleton
    }

    public static GUIManager getInstance() {
        if (instance == null) {
            synchronized (GUIManager.class) {
                if (instance == null) {
                    instance = new GUIManager();
                }
            }
        }
        return instance;
    }

    /**
     * Registers a GUI for a player.
     *
     * @param gui The GUI to register.
     */
    public void registerGUI(BaseGUI gui) {
        UUID playerId = gui.getPlayer().getUniqueId();
        playerGUIs.putIfAbsent(playerId, new Stack<>());
        playerGUIs.get(playerId).push(gui);
    }

    /**
     * Unregisters a GUI for a player.
     *
     * @param gui The GUI to unregister.
     */
    public void unregisterGUI(BaseGUI gui) {
        UUID playerId = gui.getPlayer().getUniqueId();
        Stack<BaseGUI> stack = playerGUIs.get(playerId);
        if (stack != null) {
            stack.remove(gui);
            if (stack.isEmpty()) {
                playerGUIs.remove(playerId);
            }
        }
    }

    /**
     * Gets the currently active GUI for a player.
     *
     * @param player The player to check.
     * @return The active GUI or null if none exists.
     */
    public BaseGUI getActiveGUI(Player player) {
        Stack<BaseGUI> stack = playerGUIs.get(player.getUniqueId());
        return (stack != null && !stack.isEmpty()) ? stack.peek() : null;
    }

    /**
     * Handles InventoryClickEvents and delegates to the active GUI.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        BaseGUI gui = getActiveGUI(player);
        if (gui != null) {
            gui.handleClick(event);
        }
    }

    /**
     * Cleans up GUIs when a player quits.
     *
     * @param event The PlayerQuitEvent to handle.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Stack<BaseGUI> stack = playerGUIs.remove(playerId);
        if (stack != null) {
            stack.forEach(BaseGUI::dispose);
        }
    }
}
