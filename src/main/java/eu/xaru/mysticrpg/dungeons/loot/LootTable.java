package eu.xaru.mysticrpg.dungeons.loot;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Represents a loot table with an ID and a list of item entries.
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
     * @param sourceType     "material" or "custom_item"
     * @param idOrMaterial   The custom item ID or the Material name
     * @param amount         The stack size to drop
     * @param chance         The probability (0.0 -> 1.0)
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
     * Saves this loot table into a DynamicConfig whose resourceName == userFileName.
     */
    public void saveToFile(File file) {
        // e.g. "dungeons/loottables/goblin_loot.yml"
        String userFileName = "dungeons/loottables/" + file.getName();
        // Use the same string for resourceName to fulfill "identical" usage
        DynamicConfig config = DynamicConfigManager.loadConfig(userFileName, userFileName);

        config.set("id", id);

        // Build a list of item entries
        List<Map<String, Object>> itemsData = new ArrayList<>();
        for (LootItem li : lootItems) {
            Map<String, Object> map = new HashMap<>();
            map.put("sourceType", li.getSourceType());
            map.put("id_or_material", li.getIdOrMaterial());
            map.put("amount", li.getAmount());
            map.put("chance", li.getChance());
            itemsData.add(map);
        }
        config.set("loot_items", itemsData);

        // Attempt to save if needed
        config.saveIfNeeded();
    }

    /**
     * Loads a LootTable from the specified 'file' via resourceName == userFileName as well.
     */
    @SuppressWarnings("unchecked")
    public static LootTable loadFromFile(File file) {
        try {
            // e.g. "dungeons/loottables/goblin_loot.yml"
            String userFileName = "dungeons/loottables/" + file.getName();

            // Attempt to get existing config or load anew
            DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
            if (config == null) {
                config = DynamicConfigManager.loadConfig(userFileName, userFileName);
            }

            // read ID from config (fallback to the file name if missing)
            String lootId = config.getString("id", stripExtension(file.getName()));
            if (lootId == null || lootId.isEmpty()) {
                // If no ID is found in config or fallback, we can't proceed
                return null;
            }

            LootTable table = new LootTable(lootId);

            // parse item entries from "loot_items"
            List<Map<?, ?>> rawList = config.getMapList("loot_items", new ArrayList<>());
            for (Map<?, ?> raw : rawList) {
                String sourceType = (String) raw.get("sourceType");
                String idOrMat = (String) raw.get("id_or_material");

                int amount = 1;
                if (raw.containsKey("amount")) {
                    Object amtObj = raw.get("amount");
                    if (amtObj instanceof Number) {
                        amount = ((Number) amtObj).intValue();
                    }
                }

                double chance = 1.0;
                if (raw.containsKey("chance")) {
                    Object chObj = raw.get("chance");
                    if (chObj instanceof Number) {
                        chance = ((Number) chObj).doubleValue();
                    }
                }

                table.addItem(sourceType, idOrMat, amount, chance);
            }
            return table;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper to strip an extension from a filename.
     * e.g. "goblin_loot.yml" => "goblin_loot"
     */
    private static String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf(".");
        if (idx == -1) return fileName;
        return fileName.substring(0, idx);
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
