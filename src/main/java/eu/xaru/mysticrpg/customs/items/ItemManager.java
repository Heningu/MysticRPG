package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ItemManager {

    private final JavaPlugin plugin;
    private final Map<String, CustomItem> customItems = new HashMap<>();

    public ItemManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        loadCustomItems();
    }

    void loadCustomItems() {
        File itemsFolder = new File(plugin.getDataFolder(), "custom/items");
        if (!itemsFolder.exists() && !itemsFolder.mkdirs()) {
            Bukkit.getLogger().severe("Failed to create items folder.");
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        String id;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    id = config.getString("id");
                    if (id == null || id.isEmpty()) {
                        Bukkit.getLogger().severe("Item ID is missing in file: " + file.getName());
                        continue;
                    }

                    String name = config.getString("name", "Custom Item");
                    String materialName = config.getString("material", "STONE");
                    Material material = Material.matchMaterial(materialName.toUpperCase());
                    if (material == null) {
                        Bukkit.getLogger().severe("Invalid material for item " + id + " in file: " + file.getName());
                        continue;
                    }

                    String rarityName = config.getString("rarity", "COMMON");
                    Rarity rarity;
                    try {
                        rarity = Rarity.valueOf(rarityName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Invalid rarity '" + rarityName + "' for item " + id + " in file: " + file.getName() + ". Defaulting to COMMON.");
                        rarity = Rarity.COMMON;
                    }

                    // Read category
                    String categoryName = config.getString("category", "TOOL").toUpperCase();
                    Category category;
                    try {
                        category = Category.valueOf(categoryName);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Invalid category '" + categoryName + "' for item " + id + " in file: " + file.getName() + ". Defaulting to TOOL.");
                        category = Category.TOOL; // Default category
                    }

                    int customModelData = config.getInt("custom_model_data", 0);

                    List<String> lore = config.getStringList("lore");

                    Map<String, Integer> enchantments = new HashMap<>();
                    if (config.contains("enchantments")) {
                        ConfigurationSection enchantmentsSection = config.getConfigurationSection("enchantments");
                        if (enchantmentsSection != null) {
                            for (String enchantKey : enchantmentsSection.getKeys(false)) {
                                int level = enchantmentsSection.getInt(enchantKey, 1);
                                enchantments.put(enchantKey, level);
                            }
                        }
                    }

                    Map<String, AttributeData> attributes = new HashMap<>();
                    if (config.contains("attributes")) {
                        ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
                        if (attributesSection != null) {
                            for (String attrKey : attributesSection.getKeys(false)) {
                                ConfigurationSection attrConfig = attributesSection.getConfigurationSection(attrKey);
                                if (attrConfig != null) {
                                    double value = attrConfig.getDouble("value", 0);
                                    String operationStr = attrConfig.getString("operation", "ADD_NUMBER").toUpperCase();
                                    AttributeModifier.Operation operation;
                                    try {
                                        operation = AttributeModifier.Operation.valueOf(operationStr);
                                    } catch (IllegalArgumentException e) {
                                        Bukkit.getLogger().warning("Invalid operation '" + operationStr + "' for attribute '" + attrKey + "' in item '" + id + "'. Defaulting to ADD_NUMBER.");
                                        operation = AttributeModifier.Operation.ADD_NUMBER;
                                    }
                                    AttributeData attributeData = new AttributeData(value, operation);
                                    attributes.put(attrKey, attributeData);
                                } else {
                                    // If only a value is provided
                                    double value = attributesSection.getDouble(attrKey, 0);
                                    AttributeData attributeData = new AttributeData(value, AttributeModifier.Operation.ADD_NUMBER);
                                    attributes.put(attrKey, attributeData);
                                }
                            }
                        }
                    }

                    boolean enchantedEffect = config.getBoolean("enchanted_effect", false);

                    // Read use_power_stones and power_stone_slots
                    boolean usePowerStones = config.getBoolean("use_power_stones", false);
                    int powerStoneSlots = config.getInt("power_stone_slots", 0);

                    // Read use_tier_system
                    boolean useTierSystem = config.getBoolean("use_tier_system", false);

                    int itemLevel = 1;
                    int itemMaxLevel = 1;
                    Map<Integer, Map<String, AttributeData>> tierAttributes = new HashMap<>();

                    if (useTierSystem) {
                        // Load item_level and item_max_level
                        itemLevel = config.getInt("item_level", 1);
                        itemMaxLevel = config.getInt("item_max_level", 1);

                        // Load tier_attributes
                        if (config.contains("tier_attributes")) {
                            ConfigurationSection tierAttributesSection = config.getConfigurationSection("tier_attributes");
                            if (tierAttributesSection != null) {
                                for (String tierKey : tierAttributesSection.getKeys(false)) {
                                    int tierNumber;
                                    try {
                                        tierNumber = Integer.parseInt(tierKey);
                                    } catch (NumberFormatException e) {
                                        Bukkit.getLogger().warning("Invalid tier number '" + tierKey + "' in item '" + id + "'. Skipping this tier.");
                                        continue;
                                    }
                                    Map<String, AttributeData> tierAttrs = new HashMap<>();
                                    ConfigurationSection attributesSection = tierAttributesSection.getConfigurationSection(tierKey);
                                    if (attributesSection != null) {
                                        for (String attrKey : attributesSection.getKeys(false)) {
                                            ConfigurationSection attrConfig = attributesSection.getConfigurationSection(attrKey);
                                            if (attrConfig != null) {
                                                double value = attrConfig.getDouble("value", 0);
                                                String operationStr = attrConfig.getString("operation", "ADD_NUMBER").toUpperCase();
                                                AttributeModifier.Operation operation;
                                                try {
                                                    operation = AttributeModifier.Operation.valueOf(operationStr);
                                                } catch (IllegalArgumentException e) {
                                                    Bukkit.getLogger().warning("Invalid operation '" + operationStr + "' for attribute '" + attrKey + "' in item '" + id + "'. Defaulting to ADD_NUMBER.");
                                                    operation = AttributeModifier.Operation.ADD_NUMBER;
                                                }
                                                AttributeData attributeData = new AttributeData(value, operation);
                                                tierAttrs.put(attrKey, attributeData);
                                            } else {
                                                // If only a value is provided
                                                double value = attributesSection.getDouble(attrKey, 0);
                                                AttributeData attributeData = new AttributeData(value, AttributeModifier.Operation.ADD_NUMBER);
                                                tierAttrs.put(attrKey, attributeData);
                                            }
                                        }
                                    }
                                    tierAttributes.put(tierNumber, tierAttrs);
                                }
                            }
                        }
                    } else {
                        // If tier system is not used, use default attributes
                        itemLevel = 1;
                        itemMaxLevel = 1;
                    }

                    // Read armor_type
                    String armorType = config.getString("armor_type", null);

                    CustomItem customItem = new CustomItem(id, name, material, rarity, category, customModelData, lore,
                            attributes, enchantments, enchantedEffect, useTierSystem, itemLevel, itemMaxLevel, tierAttributes,
                            usePowerStones, powerStoneSlots, armorType);

                    customItems.put(id, customItem);
                    Bukkit.getLogger().log(Level.INFO, "Loaded custom item: " + id);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to load item configuration from file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

        /**
         * Retrieves a custom item by its ID.
         *
         * @param id The ID of the custom item.
         * @return The CustomItem object, or null if not found.
         */
        public CustomItem getCustomItem (String id){
            return customItems.get(id);
        }

        /**
         * Retrieves all custom items.
         *
         * @return A collection of all CustomItem objects.
         */
        public Collection<CustomItem> getAllCustomItems () {
            return customItems.values();
        }

        /**
         * Retrieves all custom items belonging to a specific category.
         *
         * @param category The category to filter by.
         * @return A list of custom items in the specified category.
         */
        public List<CustomItem> getItemsByCategory (Category category){
            return customItems.values().stream()
                    .filter(item -> item.getCategory() == category)
                    .collect(Collectors.toList());
        }

        /**
         * Retrieves all categories available in the system.
         *
         * @return An array of all categories.
         */
        public Category[] getAllCategories () {
            return Category.values();
        }
    }
