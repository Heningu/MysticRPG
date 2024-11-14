// File: eu/xaru/mysticrpg/dungeons/loot/LootTable.java

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

    private String id;
    private final List<LootItem> lootItems;
    private final Random random;
    private final ItemManager itemManager;

    public LootTable(String id) {
        this.id = id;
        this.lootItems = new ArrayList<>();
        this.random = new Random();
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule == null) {
            throw new IllegalStateException("CustomItemModule not loaded. LootTable requires CustomItemModule.");
        }
        this.itemManager = customItemModule.getItemManager();
    }

    public String getId() {
        return id;
    }

    public void addItem(String type, String idOrMaterial, int amount, double chance) {
        LootItem lootItem = new LootItem(type, idOrMaterial, amount, chance);
        lootItems.add(lootItem);
    }

    public List<ItemStack> generateLoot() {
        List<ItemStack> loot = new ArrayList<>();
        for (LootItem lootItem : lootItems) {
            if (random.nextDouble() <= lootItem.getChance()) {
                ItemStack itemStack = null;
                if ("custom_item".equalsIgnoreCase(lootItem.getType())) {
                    CustomItem customItem = itemManager.getCustomItem(lootItem.getIdOrMaterial());
                    if (customItem != null) {
                        itemStack = customItem.toItemStack();
                        itemStack.setAmount(lootItem.getAmount());
                    }
                } else if ("material".equalsIgnoreCase(lootItem.getType())) {
                    Material material = Material.matchMaterial(lootItem.getIdOrMaterial().toUpperCase());
                    if (material != null) {
                        itemStack = new ItemStack(material, lootItem.getAmount());
                    }
                }
                if (itemStack != null) {
                    loot.add(itemStack);
                }
            }
        }
        return loot;
    }

    public List<LootItem> getLootItems() {
        return lootItems;
    }

    public void saveToFile(File file) {
        try {
            FileConfiguration config = new YamlConfiguration();
            config.set("id", id);
            List<Map<String, Object>> lootItemsList = new ArrayList<>();
            for (LootItem lootItem : lootItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("type", lootItem.getType());
                itemMap.put("id_or_material", lootItem.getIdOrMaterial());
                itemMap.put("amount", lootItem.getAmount());
                itemMap.put("chance", lootItem.getChance());
                lootItemsList.add(itemMap);
            }
            config.set("loot_items", lootItemsList);
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LootTable loadFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            LootTable lootTable = new LootTable(id);
            List<Map<?, ?>> lootItemsList = config.getMapList("loot_items");
            for (Map<?, ?> itemMap : lootItemsList) {
                String type = (String) itemMap.get("type");
                String idOrMaterial = (String) itemMap.get("id_or_material");

                int amount = 1; // default amount
                if (itemMap.containsKey("amount")) {
                    Object amountObj = itemMap.get("amount");
                    if (amountObj instanceof Number) {
                        amount = ((Number) amountObj).intValue();
                    }
                }

                double chance = 1.0; // default chance
                if (itemMap.containsKey("chance")) {
                    Object chanceObj = itemMap.get("chance");
                    if (chanceObj instanceof Number) {
                        chance = ((Number) chanceObj).doubleValue();
                    }
                }

                lootTable.addItem(type, idOrMaterial, amount, chance);
            }
            return lootTable;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class LootItem {
        private final String type; // "custom_item" or "material"
        private final String idOrMaterial;
        private final int amount;
        private final double chance;

        public LootItem(String type, String idOrMaterial, int amount, double chance) {
            this.type = type;
            this.idOrMaterial = idOrMaterial;
            this.amount = amount;
            this.chance = chance;
        }

        public String getType() {
            return type;
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
