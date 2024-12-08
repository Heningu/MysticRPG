// File: eu/xaru/mysticrpg/utils/InventoryBuilder.java
package eu.xaru.mysticrpg.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for building custom inventories (GUIs).
 */
public class InventoryBuilder {
    private final Map<Character, ItemStack> materials = new HashMap<>();
    private String[] pattern = new String[0];
    private int size = 54;
    private Inventory inventory;
    private Function<Inventory, Inventory> inventoryContents;

    public InventoryBuilder(InventoryHolder holder) {
        this.inventory = Bukkit.createInventory(holder, size);
    }

    /**
     * Sets the material for a specific character key.
     *
     * @param key      The character key representing the item in the pattern.
     * @param material The XMaterial to associate with the key.
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setMaterial(char key, XMaterial material) {
        this.materials.put(key, material.parseItem());
        return this;
    }

    /**
     * Sets a custom ItemStack for a specific character key.
     *
     * @param key  The character key representing the item in the pattern.
     * @param item The ItemStack to associate with the key.
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setItem(char key, ItemStack item) {
        this.materials.put(key, item);
        return this;
    }

    /**
     * Applies default materials to common keys.
     *
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder withDefaults() {
        this.materials.put('B', new ItemStackBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).withDisplayName(" ").build());
        this.materials.put('X', new ItemStackBuilder(XMaterial.AIR).build());
        return this;
    }

    /**
     * Sets a custom function to modify the inventory contents before building.
     *
     * @param contents A function that takes an Inventory and returns a modified Inventory.
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setInventoryContents(Function<Inventory, Inventory> contents) {
        this.inventoryContents = contents;
        return this;
    }

    /**
     * Counts the number of occurrences of a specific character key in the pattern.
     *
     * @param key The character key to count.
     * @return The count of occurrences.
     */
    public int countSimilar(char key) {
        int count = 0;
        for (String line : pattern) {
            for (char c : line.toCharArray()) {
                if (c == key) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts the number of occurrences of a specific ItemStack in the materials map.
     *
     * @param itemStack The ItemStack to count.
     * @return The count of occurrences.
     */
    public int countSimilar(ItemStack itemStack) {
        int count = 0;
        for (ItemStack item : materials.values()) {
            if (item != null && item.isSimilar(itemStack)) {
                count++;
            }
        }
        return count * countSimilar(itemStack.getType().toString().charAt(0));
    }

    /**
     * Retrieves the slot indices where a specific ItemStack is placed.
     *
     * @param item The ItemStack to search for.
     * @return An array of slot indices.
     */
    public int[] getItemSlots(ItemStack item) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack slotItem = inventory.getItem(i);

            if (slotItem == null) {
                slots.add(i);
            }

            if (slotItem != null && slotItem.isSimilar(item)) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Retrieves the slot indices where a specific character key is placed.
     *
     * @param key The character key to search for.
     * @return An array of slot indices.
     */
    public int[] getItemSlots(char key) {
        ItemStack item = this.materials.get(key);
        if (item == null) {
            return getItemSlots(new ItemStackBuilder(XMaterial.AIR).build());
        }
        return getItemSlots(item);
    }

    /**
     * Sets the pattern for the inventory layout.
     *
     * @param pattern An array of strings representing the inventory rows.
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setPattern(String... pattern) {
        if (pattern.length < 1 || pattern.length > 6)
            throw new IllegalArgumentException("Pattern must have between 1 and 6 lines");
        int numCols = pattern[0].length();
        if (numCols < 1 || numCols > 9)
            throw new IllegalArgumentException("Pattern lines must have between 1 and 9 characters");
        for (String line : pattern)
            if (line.length() != numCols)
                throw new IllegalArgumentException("Pattern lines must have the same length");
        this.pattern = pattern;
        this.size = pattern.length * 9;
        this.inventory = Bukkit.createInventory(inventory.getHolder(), size);

        return this;
    }

    /**
     * Sets the size of the inventory.
     *
     * @param size The size of the inventory (must be a multiple of 9).
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setSize(int size) {
        if (size < 9 || size > 54)
            throw new IllegalArgumentException("Size must be between 9 and 54 and a multiple of 9");
        this.pattern = new String[size / 9];
        for (int i = 0; i < size / 9; i++)
            this.pattern[i] = "XXXXXXXXX"; // Default fill
        this.size = size;
        this.inventory = Bukkit.createInventory(inventory.getHolder(), size);
        return this;
    }

    /**
     * Sets an item in a specific row and column.
     *
     * @param row      The row index (0-based).
     * @param col      The column index (0-based).
     * @param material The material of the item.
     * @param quantity The quantity of the item.
     * @return The InventoryBuilder instance.
     */
    public InventoryBuilder setSlot(int row, int col, Material material, int quantity) {
        if (row < 0 || row >= pattern.length)
            throw new IllegalArgumentException("Row must be between 0 and " + (pattern.length - 1));
        if (col < 0 || col >= pattern[0].length())
            throw new IllegalArgumentException("Column must be between 0 and " + (pattern[0].length() - 1));
        ItemStack item = new ItemStack(material, quantity);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(material.name());
            item.setItemMeta(meta);
        }
        int index = (row * 9) + col;
        if (index >= 0 && index < size)
            inventory.setItem(index, item);
        return this;
    }

    /**
     * Builds and finalizes the inventory.
     *
     * @return The constructed Inventory instance.
     */
    public Inventory build() {
        ItemStack[] contents = new ItemStack[size];
        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[i].length(); j++) {
                char key = pattern[i].charAt(j);
                contents[(i * 9) + j] = materials.getOrDefault(key, new ItemStack(Material.AIR));
            }
        }
        inventory.setContents(contents);
        if (inventoryContents != null) {
            inventory = inventoryContents.apply(inventory);
        }
        return inventory;
    }

    /**
     * Retrieves the constructed inventory.
     *
     * @return The Inventory instance.
     */
    public Inventory getInventory() {
        return inventory;
    }
}
