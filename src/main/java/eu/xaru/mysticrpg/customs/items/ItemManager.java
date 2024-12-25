package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages loading and storing all CustomItems from the /custom/items folder,
 * reading each .yml via DynamicConfig (Maps, no ConfigurationSections).
 */
public class ItemManager {

    private final JavaPlugin plugin;
    private final Map<String, CustomItem> customItems = new HashMap<>();

    public ItemManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        loadCustomItems();
    }

    /**
     * Loads all custom item YAML files from plugins/MysticRPG/custom/items/.
     * Uses the DynamicConfig system for reading each file. No direct usage
     * of ConfigurationSection remains.
     */
    void loadCustomItems() {
        File itemsFolder = new File(plugin.getDataFolder(), "custom/items");
        if (!itemsFolder.exists() && !itemsFolder.mkdirs()) {
            DebugLogger.getInstance().severe("Failed to create items folder.");
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String userFileName = "custom/items/" + file.getName();
            try {
                // 1) Load (or reload) the config
                DynamicConfigManager.loadConfig(userFileName, userFileName);

                // 2) Retrieve the DynamicConfig
                DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
                if (config == null) {
                    DebugLogger.getInstance().severe("Failed to load config for file: " + file.getName());
                    continue;
                }

                // 3) Start extracting data from the config
                String id = config.getString("id", "");
                if (id.isEmpty()) {
                    DebugLogger.getInstance().severe("Item ID is missing in file: " + file.getName());
                    continue;
                }

                String name = config.getString("name", "Custom Item");
                String materialName = config.getString("material", "STONE");
                Material material = Material.matchMaterial(materialName.toUpperCase());
                if (material == null) {
                    DebugLogger.getInstance().severe("Invalid material '" + materialName + "' for item " + id + " in " + file.getName());
                    continue;
                }

                // Read rarity
                String rarityName = config.getString("rarity", "COMMON");
                Rarity rarity = parseRarity(rarityName, id, file.getName());

                // Read category
                String categoryName = config.getString("category", "TOOL").toUpperCase(Locale.ROOT);
                Category category = parseCategory(categoryName, id, file.getName());

                int customModelData = config.getInt("custom_model_data", 0);
                List<String> lore = config.getStringList("lore", new ArrayList<>());

                // Build enchantments
                Map<String, Integer> enchantments = new HashMap<>();
                Object enchObj = config.get("enchantments");
                if (enchObj instanceof Map<?,?> enchMap) {
                    for (Map.Entry<?,?> e : enchMap.entrySet()) {
                        String enchKey = e.getKey().toString();
                        int level = parseInt(e.getValue(), 1);
                        enchantments.put(enchKey, level);
                    }
                }

                // Build attributes
                Map<String, AttributeData> attributes = new HashMap<>();
                Object attrsObj = config.get("attributes");
                if (attrsObj instanceof Map<?,?> attrsMap) {
                    for (Map.Entry<?,?> e : attrsMap.entrySet()) {
                        String attrKey = e.getKey().toString();
                        Object singleAttrObj = e.getValue();
                        double value = 0.0;
                        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;

                        // If the attribute is a map with {value, operation}, parse them
                        if (singleAttrObj instanceof Map<?,?> singleMap) {
                            value = parseDouble(singleMap.get("value"), 0.0);
                            String operationStr = parseString(singleMap.get("operation"), "ADD_NUMBER").toUpperCase(Locale.ROOT);
                            op = parseOperation(operationStr, attrKey, id, file.getName());
                        } else {
                            // If it's just a raw numeric value
                            value = parseDouble(singleAttrObj, 0.0);
                        }
                        attributes.put(attrKey, new AttributeData(value, op));
                    }
                }

                boolean enchantedEffect = config.getBoolean("enchanted_effect", false);

                // Power stones
                boolean usePowerStones = config.getBoolean("use_power_stones", false);
                int powerStoneSlots = config.getInt("power_stone_slots", 0);

                // Tier system
                boolean useTierSystem = config.getBoolean("use_tier_system", false);
                int itemLevel = 1;
                int itemMaxLevel = 1;
                Map<Integer, Map<String, AttributeData>> tierAttributes = new HashMap<>();

                if (useTierSystem) {
                    itemLevel = config.getInt("item_level", 1);
                    itemMaxLevel = config.getInt("item_max_level", 1);

                    Object tierObj = config.get("tier_attributes");
                    if (tierObj instanceof Map<?,?> tierMap) {
                        // e2 -> tierKey -> Map of attributes
                        for (Map.Entry<?,?> e2 : tierMap.entrySet()) {
                            String tierKey = e2.getKey().toString();
                            int tierNumber;
                            try {
                                tierNumber = Integer.parseInt(tierKey);
                            } catch (NumberFormatException ex) {
                                DebugLogger.getInstance().warning("Invalid tier '" + tierKey + "' for item '" + id
                                        + "' in " + file.getName() + ". Skipping this tier.");
                                continue;
                            }
                            Map<String, AttributeData> levelAttrs = new HashMap<>();
                            Object levelAttrObj = e2.getValue();
                            if (levelAttrObj instanceof Map<?,?> levelMap) {
                                for (Map.Entry<?,?> sub : levelMap.entrySet()) {
                                    String subAttr = sub.getKey().toString();
                                    Object subVal = sub.getValue();
                                    double val = 0.0;
                                    AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;

                                    if (subVal instanceof Map<?,?> subAttrMap) {
                                        val = parseDouble(subAttrMap.get("value"), 0.0);
                                        String opStr = parseString(subAttrMap.get("operation"), "ADD_NUMBER").toUpperCase(Locale.ROOT);
                                        operation = parseOperation(opStr, subAttr, id, file.getName());
                                    } else {
                                        // Just numeric
                                        val = parseDouble(subVal, 0.0);
                                    }
                                    levelAttrs.put(subAttr, new AttributeData(val, operation));
                                }
                            }
                            tierAttributes.put(tierNumber, levelAttrs);
                        }
                    }
                }

                // Armor type
                String armorType = config.getString("armor_type", null);

                // Set ID (only relevant if category=ARMOR)
                String setId = config.getString("set", null);
                if (setId != null && !category.equals(Category.ARMOR)) {
                    DebugLogger.getInstance().warning("Set defined for non-armor item '" + id
                            + "' in file: " + file.getName() + ". Ignoring set.");
                    setId = null;
                }

                // Finally, build the CustomItem
                CustomItem customItem = new CustomItem(
                        id, name, material, rarity, category,
                        customModelData, lore, attributes, enchantments,
                        enchantedEffect, useTierSystem, itemLevel, itemMaxLevel,
                        tierAttributes, usePowerStones, powerStoneSlots,
                        armorType, setId
                );

                customItems.put(id, customItem);
                DebugLogger.getInstance().log(Level.INFO, "Loaded custom item: " + id);

            } catch (Exception e) {
                DebugLogger.getInstance().severe("Failed to load item config from " + file.getName() + ":", e);
            }
        }
    }

    /**
     * Get a CustomItem by ID, or null if it doesn't exist.
     */
    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }

    /**
     * @return All loaded custom items.
     */
    public Collection<CustomItem> getAllCustomItems() {
        return customItems.values();
    }

    /**
     * @return All items matching a certain Category.
     */
    public List<CustomItem> getItemsByCategory(Category category) {
        return customItems.values().stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * @return All possible item Categories.
     */
    public Category[] getAllCategories() {
        return Category.values();
    }

    // ---------- Private parse helpers ----------

    private Rarity parseRarity(String rarityName, String itemId, String fileName) {
        try {
            return Rarity.valueOf(rarityName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            DebugLogger.getInstance().warning("Invalid rarity '" + rarityName + "' for item " + itemId
                    + " in file: " + fileName + ". Defaulting to COMMON.");
            return Rarity.COMMON;
        }
    }

    private Category parseCategory(String categoryName, String itemId, String fileName) {
        try {
            return Category.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            DebugLogger.getInstance().warning("Invalid category '" + categoryName + "' for item " + itemId
                    + " in file: " + fileName + ". Defaulting to TOOL.");
            return Category.TOOL;
        }
    }

    private int parseInt(Object val, int fallback) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String parseString(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }

    private AttributeModifier.Operation parseOperation(String operationStr, String attrKey, String itemId, String fileName) {
        try {
            return AttributeModifier.Operation.valueOf(operationStr);
        } catch (IllegalArgumentException ex) {
            DebugLogger.getInstance().warning("Invalid operation '" + operationStr
                    + "' for attribute '" + attrKey + "' in item '" + itemId
                    + "' from file: " + fileName + ". Defaulting to ADD_NUMBER.");
            return AttributeModifier.Operation.ADD_NUMBER;
        }
    }
}
