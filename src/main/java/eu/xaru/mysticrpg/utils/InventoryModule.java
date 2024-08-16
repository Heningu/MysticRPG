package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryModule implements IBaseModule {

    // Use WeakReferences to ensure that memory is not leaked
    private final Map<UUID, WeakReference<Inventory>> playerInventories = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        // Initialization logic if needed
    }

    @Override
    public void start() {
        // Start logic if needed
    }

    @Override
    public void stop() {
        // Clear all stored inventories safely
        playerInventories.clear();
    }

    @Override
    public void unload() {
        // Unload logic if needed
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();  // No dependencies
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    // Inventory Management

    /**
     * Stores a player's inventory for later retrieval using a WeakReference to prevent memory leaks.
     *
     * @param player The player whose inventory is to be stored.
     */
    public void storePlayerInventory(Player player) {
        playerInventories.put(player.getUniqueId(), new WeakReference<>(player.getInventory()));
    }

    /**
     * Retrieves a stored inventory for a player.
     *
     * @param player The player whose inventory is to be retrieved.
     * @return The stored inventory, or null if no inventory is stored or if it has been garbage collected.
     */
    public Inventory retrievePlayerInventory(Player player) {
        WeakReference<Inventory> inventoryRef = playerInventories.get(player.getUniqueId());
        return (inventoryRef != null) ? inventoryRef.get() : null;
    }

    /**
     * Removes a stored inventory for a player, releasing the weak reference.
     *
     * @param player The player whose inventory is to be removed.
     */
    public void removeStoredInventory(Player player) {
        playerInventories.remove(player.getUniqueId());
    }

    // Inventory Builder

    /**
     * Starts the building process for a new inventory.
     *
     * @param name The name/title of the inventory.
     * @param size The size of the inventory (must be a multiple of 9).
     * @return An InventoryBuilder instance.
     */
    public InventoryBuilder createInventory(String name, int size) {
        return new InventoryBuilder(name, size);
    }

    public class InventoryBuilder {
        private final Inventory inventory;

        public InventoryBuilder(String name, int size) {
            this.inventory = Bukkit.createInventory(null, size, name);
        }

        public InventoryBuilder addItem(Material material, String displayName, String... lore) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(Arrays.asList(lore));
                item.setItemMeta(meta);
            }
            inventory.addItem(item);
            return this;
        }

        public InventoryBuilder setItem(int slot, Material material, String displayName, String... lore) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(Arrays.asList(lore));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            return this;
        }

        public InventoryBuilder fill(Material material, String displayName, String... lore) {
            for (int i = 0; i < inventory.getSize(); i++) {
                setItem(i, material, displayName, lore);
            }
            return this;
        }

        public Inventory build() {
            return inventory;
        }
    }

    // Utility Functions

    /**
     * Clears all items from the specified inventory.
     *
     * @param inventory The inventory to clear.
     */
    public void clearInventory(Inventory inventory) {
        inventory.clear();
    }

    /**
     * Fills an inventory with a specified item.
     *
     * @param inventory The inventory to fill.
     * @param material  The material of the item.
     * @param displayName The display name of the item.
     * @param lore The lore of the item.
     */
    public void fillInventory(Inventory inventory, Material material, String displayName, String... lore) {
        for (int i = 0; i < inventory.getSize(); i++) {
            setItem(inventory, i, material, displayName, lore);
        }
    }

    /**
     * Sets an item in a specific slot of an inventory.
     *
     * @param inventory The inventory.
     * @param slot      The slot index.
     * @param material  The material of the item.
     * @param displayName The display name of the item.
     * @param lore The lore of the item.
     */
    public void setItem(Inventory inventory, int slot, Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    /**
     * Counts the number of similar items in an inventory.
     *
     * @param inventory The inventory.
     * @param item      The item to compare.
     * @return The count of similar items.
     */
    public int countSimilarItems(Inventory inventory, ItemStack item) {
        int count = 0;
        for (ItemStack i : inventory.getContents()) {
            if (i != null && i.isSimilar(item)) {
                count += i.getAmount();
            }
        }
        return count;
    }

    /**
     * Retrieves the slots containing a specific item.
     *
     * @param inventory The inventory.
     * @param item      The item to search for.
     * @return An array of slot indices containing the item.
     */
    public int[] getItemSlots(Inventory inventory, ItemStack item) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slotItem = inventory.getItem(i);
            if (slotItem != null && slotItem.isSimilar(item)) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
