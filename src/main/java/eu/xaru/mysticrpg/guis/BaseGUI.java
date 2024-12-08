// File: eu/xaru/mysticrpg/guis/BaseGUI.java
package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.guis.components.Button;
import eu.xaru.mysticrpg.guis.manager.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all GUIs with nested navigation and proper cleanup.
 */
public abstract class BaseGUI {
    protected final Player player;
    protected final Inventory inventory;
    protected final Map<Integer, Button> buttons = new HashMap<>();

    /**
     * Constructs a BaseGUI.
     *
     * @param player The player interacting with the GUI.
     * @param title  The title of the inventory.
     * @param size   The size of the inventory (must be a multiple of 9 between 9 and 54).
     * @throws IllegalArgumentException if size is invalid.
     */
    public BaseGUI(Player player, String title, int size) {
        validateInventorySize(size);
        this.player = player;
        this.inventory = org.bukkit.Bukkit.createInventory(null, size, title);
        initialize();
        GUIManager.getInstance().registerGUI(this);
    }

    /**
     * Validates the inventory size.
     *
     * @param size The size to validate.
     * @throws IllegalArgumentException if size is not a multiple of 9 or not between 9 and 54.
     */
    private void validateInventorySize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 and between 9 and 54.");
        }
    }

    /**
     * Initializes the GUI with items and actions. Must be implemented by subclasses.
     */
    protected abstract void initialize();

    /**
     * Adds a button to a specific slot.
     *
     * @param slot   The slot index (0-based).
     * @param button The Button to add.
     * @throws IllegalArgumentException if slot is out of inventory bounds.
     */
    protected void addButton(int slot, Button button) {
        validateSlot(slot);
        buttons.put(slot, button);
        inventory.setItem(slot, button.getItem());
    }

    /**
     * Validates the slot index.
     *
     * @param slot The slot index to validate.
     * @throws IllegalArgumentException if slot is out of inventory bounds.
     */
    private void validateSlot(int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            throw new IllegalArgumentException("Slot index " + slot + " is out of bounds for inventory size " + inventory.getSize());
        }
    }

    /**
     * Handles click events within the GUI.
     *
     * @param event The InventoryClickEvent to handle.
     */
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Ensure the clicked inventory is the current GUI
        if (!event.getView().getTopInventory().equals(this.inventory)) return;

        event.setCancelled(true); // Prevent default behavior

        int slot = event.getRawSlot();

        // Check if the click is within the GUI's inventory bounds
        if (slot < 0 || slot >= inventory.getSize()) return;

        Button button = buttons.get(slot);
        if (button != null && button.getAction() != null) {
            try {
                button.getAction().execute(event);
            } catch (Exception e) {
                player.sendMessage("An error occurred while processing your action.");
                e.printStackTrace(); // Log the exception for debugging
            }
        }
    }

    /**
     * Opens this GUI for the player.
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Closes the GUI for the player and unregisters it from the manager.
     */
    public void close() {
        if (player.getOpenInventory().getTopInventory().equals(this.inventory)) {
            player.closeInventory();
        }
        dispose();
    }

    /**
     * Properly disposes of the GUI by clearing references and unregistering it.
     */
    public void dispose() {
        buttons.clear();
        GUIManager.getInstance().unregisterGUI(this);
    }

    /**
     * Retrieves the Inventory associated with this GUI.
     *
     * @return The Inventory instance.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Retrieves the Player interacting with this GUI.
     *
     * @return The Player instance.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Ensures that the GUI is properly cleaned up when the object is garbage collected.
     * This is a safeguard and may not always be reliable due to JVM garbage collection timing.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }
}
