package eu.xaru.mysticrpg.dungeons.loot;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class LootTable {

    private final String id;
    private final List<LootItem> lootItems;
    private final Random random;
    private final ItemManager itemManager;

    public LootTable(String id) {
        this.id = id;
        this.lootItems = new ArrayList<>();
        this.random = new Random();

        // Ensure we can get the itemManager
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

    public void addItem(String sourceType, String idOrMaterial, int amount, double chance) {
        lootItems.add(new LootItem(sourceType, idOrMaterial, amount, chance));
    }

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

    public void saveToFile(File file) {
        try {
            FileConfiguration config = new YamlConfiguration();
            config.set("id", id);

            List<Map<String, Object>> items = new ArrayList<>();
            for (LootItem li : lootItems) {
                Map<String, Object> map = new HashMap<>();
                map.put("sourceType", li.getSourceType());
                map.put("id_or_material", li.getIdOrMaterial());
                map.put("amount", li.getAmount());
                map.put("chance", li.getChance());
                items.add(map);
            }
            config.set("loot_items", items);

            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LootTable loadFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            LootTable table = new LootTable(id);

            List<Map<?, ?>> list = config.getMapList("loot_items");
            for (Map<?, ?> raw : list) {
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

    public static class LootItem {
        private final String sourceType; // "material" or "custom_item"
        private final String idOrMaterial;
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
