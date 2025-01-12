package eu.xaru.mysticrpg.dungeons.loot;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Represents a loot table with an ID and a list of item entries,
 * saving/loading via YamlConfiguration (no DynamicConfig).
 */
public class LootTable {

    private final String id;
    private final List<LootItem> lootItems;
    private final Random random;
    private final ItemManager itemManager;

    public LootTable(String id) {
        this.id = id;
        this.lootItems = new ArrayList<>();
        this.random = new Random();

        // Acquire the ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance()
                .getModuleInstance(CustomItemModule.class);
        if (customItemModule == null) {
            throw new IllegalStateException("CustomItemModule not loaded. LootTable requires CustomItemModule.");
        }
        this.itemManager = customItemModule.getItemManager();
    }

    public String getId() {
        return id;
    }

    /**
     * Adds a LootItem to this table.
     *
     * @param sourceType   "material" or "custom_item"
     * @param idOrMaterial The custom item ID or the Material name
     * @param amount       The stack size to drop
     * @param chance       The probability (0.0 -> 1.0)
     */
    public void addItem(String sourceType, String idOrMaterial, int amount, double chance) {
        lootItems.add(new LootItem(sourceType, idOrMaterial, amount, chance));
    }

    /**
     * Based on each entry's chance, returns a list of ItemStacks for a "drop roll."
     */
    public List<ItemStack> generateLoot() {
        List<ItemStack> result = new ArrayList<>();
        for (LootItem lootItem : lootItems) {
            if (random.nextDouble() <= lootItem.getChance()) {
                ItemStack item = null;

                if ("custom_item".equalsIgnoreCase(lootItem.getSourceType())) {
                    CustomItem cItem = itemManager.getCustomItem(lootItem.getIdOrMaterial());
                    if (cItem != null) {
                        item = cItem.toItemStack();
                        item.setAmount(lootItem.getAmount());
                    }
                } else if ("material".equalsIgnoreCase(lootItem.getSourceType())) {
                    Material mat = Material.matchMaterial(lootItem.getIdOrMaterial().toUpperCase());
                    if (mat != null) {
                        item = new ItemStack(mat, lootItem.getAmount());
                    }
                }

                if (item != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Saves this LootTable into a YamlConfiguration file (e.g., "dungeons/loottables/goblin_loot.yml").
     */
    public void saveToFile(File file) {
        YamlConfiguration ycfg = new YamlConfiguration();

        // Basic fields
        ycfg.set("id", this.id);

        // Convert loot items to a list of maps
        List<Map<String, Object>> itemsData = new ArrayList<>();
        for (LootItem li : lootItems) {
            Map<String, Object> map = new HashMap<>();
            map.put("sourceType", li.getSourceType());
            map.put("id_or_material", li.getIdOrMaterial());
            map.put("amount", li.getAmount());
            map.put("chance", li.getChance());
            itemsData.add(map);
        }
        ycfg.set("loot_items", itemsData);

        // Write out
        try {
            ycfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a LootTable from the specified file using YamlConfiguration.
     */
    @SuppressWarnings("unchecked")
    public static LootTable loadFromFile(File file) {
        YamlConfiguration ycfg = new YamlConfiguration();
        try {
            ycfg.load(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Attempt to find an "id" in the file, or fallback to the filename minus extension
        String lootId = ycfg.getString("id", stripExtension(file.getName()));
        if (lootId == null || lootId.isEmpty()) {
            return null; // No valid ID found
        }

        LootTable table = new LootTable(lootId);

        // Parse the list of loot items
        List<Map<?, ?>> rawList = (List<Map<?, ?>>) ycfg.getList("loot_items", Collections.emptyList());
        for (Map<?, ?> raw : rawList) {
            if (raw == null) continue;

            String sourceType = parseString(raw.get("sourceType"), "material");
            String idOrMat = parseString(raw.get("id_or_material"), "STONE");

            int amount = parseInt(raw.get("amount"), 1);
            double chance = parseDouble(raw.get("chance"), 1.0);

            table.addItem(sourceType, idOrMat, amount, chance);
        }

        return table;
    }

    /**
     * Helper to strip an extension from a filename.
     * e.g. "goblin_loot.yml" => "goblin_loot"
     */
    private static String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx == -1) return fileName;
        return fileName.substring(0, idx);
    }

    // --------------------------------------------------
    // Private parse helpers
    // --------------------------------------------------
    private static String parseString(Object obj, String fallback) {
        return (obj != null) ? obj.toString() : fallback;
    }

    private static int parseInt(Object obj, int fallback) {
        if (obj instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseDouble(Object obj, double fallback) {
        if (obj instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    // --------------------------------------------------
    // Inner class representing one loot entry
    // --------------------------------------------------
    public static class LootItem {
        private final String sourceType;    // "material" or "custom_item"
        private final String idOrMaterial;  // "DIAMOND_SWORD" or "myCustomItemId"
        private final int amount;
        private final double chance;

        public LootItem(String sourceType, String idOrMaterial, int amount, double chance) {
            this.sourceType = sourceType;
            this.idOrMaterial = idOrMaterial;
            this.amount = amount;
            this.chance = chance;
        }

        public String getSourceType() {
            return sourceType;
        }

        public String getIdOrMaterial() {
            return idOrMaterial;
        }

        public int getAmount() {
            return amount;
        }

        public double getChance() {
            return chance;
        }
    }
}
