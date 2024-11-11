package eu.xaru.mysticrpg.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for managing custom inventories (GUIs).
 */
public class CustomInventoryManager {

    /**
     * Creates a custom inventory with the specified size and title.
     *
     * @param size  The size of the inventory (must be a multiple of 9, between 9 and 54).
     * @param title The title of the inventory.
     * @return The created Inventory instance.
     * @throws IllegalArgumentException if the size is not a multiple of 9 or out of bounds.
     */
    public static Inventory createInventory(int size, String title) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 and between 9 and 54.");
        }
        return Bukkit.createInventory(null, size, Utils.getInstance().$(title));
    }

    /**
     * Adds an item to a specific slot in the inventory.
     *
     * @param inventory The inventory to add the item to.
     * @param slot      The slot index (0-based).
     * @param item      The ItemStack to add.
     * @return True if the item was added successfully, false otherwise.
     */
    public static boolean addItemToSlot(Inventory inventory, int slot, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return false;
        }
        inventory.setItem(slot, item);
        return true;
    }

    /**
     * Fills all empty slots in the inventory with a placeholder item.
     *
     * @param inventory   The inventory to fill.
     * @param placeholder The placeholder ItemStack to use.
     */
    public static void fillEmptySlots(Inventory inventory, ItemStack placeholder) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Creates a placeholder ItemStack with specified material and name.
     *
     * @param material The material of the placeholder item.
     * @param name     The display name of the placeholder item.
     * @return The created placeholder ItemStack.
     */
    public static ItemStack createPlaceholder(Material material, String name) {
        ItemStack placeholder = new ItemStack(material);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.getInstance().$(name));
            meta.setLore(List.of(" "));
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    /**
     * Sets the display name of an ItemStack.
     *
     * @param item        The ItemStack to modify.
     * @param displayName The new display name.
     */
    public static void setItemDisplayName(ItemStack item, String displayName) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
    }

    /**
     * Sets the lore of an ItemStack.
     *
     * @param item The ItemStack to modify.
     * @param lore The lore to set.
     */
    public static void setItemLore(ItemStack item, List<String> lore) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    /**
     * Opens the specified inventory for the player.
     *
     * @param player    The player to open the inventory for.
     * @param inventory The inventory to open.
     */
    public static void openInventory(Player player, Inventory inventory) {
        player.openInventory(inventory);
    }
}
