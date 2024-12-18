package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public class CustomItem {

    private final String id;
    private final String name;
    private final Material material;
    private final Rarity rarity;
    private final Category category;
    private final int customModelData;
    private final List<String> lore;
    private final Map<String, AttributeData> attributes;
    private final Map<String, Integer> enchantments;
    private final boolean enchantedEffect;
    private final String armorType;
    private final String setId; // null if not part of a set

    // Fields for tier system
    private final boolean useTierSystem;
    private final int itemLevel;
    private final int itemMaxLevel;
    private final Map<Integer, Map<String, AttributeData>> tierAttributes;

    // Fields for power stones
    private final boolean usePowerStones;
    private final int powerStoneSlots;

    public CustomItem(String id, String name, Material material, Rarity rarity, Category category, int customModelData,
                      List<String> lore, Map<String, AttributeData> attributes, Map<String, Integer> enchantments,
                      boolean enchantedEffect, boolean useTierSystem, int itemLevel, int itemMaxLevel,
                      Map<Integer, Map<String, AttributeData>> tierAttributes,
                      boolean usePowerStones, int powerStoneSlots, String armorType, String setId) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.rarity = rarity;
        this.category = category;
        this.customModelData = customModelData;
        this.lore = lore != null ? lore : Collections.emptyList();
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
        this.enchantments = enchantments != null ? enchantments : Collections.emptyMap();
        this.enchantedEffect = enchantedEffect;
        this.useTierSystem = useTierSystem;
        this.itemLevel = itemLevel;
        this.itemMaxLevel = itemMaxLevel;
        this.tierAttributes = tierAttributes != null ? tierAttributes : Collections.emptyMap();
        this.usePowerStones = usePowerStones;
        this.powerStoneSlots = powerStoneSlots;
        this.armorType = armorType;
        this.setId = setId; // Set from constructor
    }

    public String getId() {
        return id;
    }

    public boolean isUsePowerStones() {
        return usePowerStones;
    }

    public int getPowerStoneSlots() {
        return powerStoneSlots;
    }

    public boolean isUseTierSystem() {
        return useTierSystem;
    }

    public int getItemLevel() {
        return itemLevel;
    }

    public int getItemMaxLevel() {
        return itemMaxLevel;
    }

    public String getArmorType() { return armorType; }

    public Map<Integer, Map<String, AttributeData>> getTierAttributes() {
        return tierAttributes;
    }

    public Map<String, AttributeData> getAttributes() {
        return attributes;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getSetId() {
        return setId;
    }

    /**
     * Attempts to convert an attribute name (like "HEALTH" or "DEFENSE") to a Bukkit Attribute.
     * If "GENERIC_HEALTH" fails, tries just "HEALTH".
     */
    public Attribute getAttributeByName(String name) {
        try {
            return Attribute.valueOf("GENERIC_" + name);
        } catch (IllegalArgumentException e) {
            // Try without GENERIC_ prefix
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ex) {
                System.err.println("Invalid attribute name: " + name + " for item: " + id);
                return null;
            }
        }
    }

    public EquipmentSlot getEquipmentSlot() {
        if (material.name().endsWith("_HELMET")) {
            return EquipmentSlot.HEAD;
        } else if (material.name().endsWith("_CHESTPLATE")) {
            return EquipmentSlot.CHEST;
        } else if (material.name().endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        } else if (material.name().endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        } else {
            return EquipmentSlot.HAND;
        }
    }

    // Method to create ItemStack with specified tier
    public ItemStack toItemStack(int currentTier) {
        DebugLogger.getInstance().log(Level.INFO, "Creating ItemStack for custom item " + id + " at tier " + currentTier);

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Utils.getInstance().$(name));

            // Set CustomModelData
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            NamespacedKey idKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_id");
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

            NamespacedKey categoryKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_category");
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category.name());

            if (useTierSystem) {
                NamespacedKey tierKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_tier");
                meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, currentTier);

                NamespacedKey maxTierKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_max_tier");
                meta.getPersistentDataContainer().set(maxTierKey, PersistentDataType.INTEGER, itemMaxLevel);
            }

            if (usePowerStones) {
                NamespacedKey powerStoneSlotsKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "power_stone_slots");
                meta.getPersistentDataContainer().set(powerStoneSlotsKey, PersistentDataType.INTEGER, powerStoneSlots);
            }

            if (setId != null && !setId.isEmpty()) {
                NamespacedKey setKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_set");
                meta.getPersistentDataContainer().set(setKey, PersistentDataType.STRING, setId);
            }

            Map<String, AttributeData> finalAttributes = useTierSystem ?
                    tierAttributes.getOrDefault(currentTier, attributes) : attributes;

            StringBuilder sb = new StringBuilder();
            DebugLogger.getInstance().log(Level.INFO, "Final attributes for item " + id + " at tier " + currentTier + ":");
            for (Map.Entry<String, AttributeData> attr : finalAttributes.entrySet()) {
                double value = attr.getValue().getValue();
                DebugLogger.getInstance().log(Level.INFO, " - " + attr.getKey().toUpperCase() + ": " + value);
                if (sb.length() > 0) sb.append(";");
                sb.append(attr.getKey().toUpperCase()).append(":").append(value);
            }

            NamespacedKey statsKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_attributes");
            meta.getPersistentDataContainer().set(statsKey, PersistentDataType.STRING, sb.toString());

            if (enchantedEffect) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(meta);
                itemStack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            } else {
                itemStack.setItemMeta(meta);
            }

            if (!enchantments.isEmpty()) {
                for (Map.Entry<String, Integer> enchantEntry : enchantments.entrySet()) {
                    org.bukkit.enchantments.Enchantment enchantment =
                            org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantEntry.getKey().toLowerCase()));
                    if (enchantment != null) {
                        itemStack.addUnsafeEnchantment(enchantment, enchantEntry.getValue());
                    }
                }
            }

            PowerStoneManager powerStoneManager = ModuleManager.getInstance().getModuleInstance(PowerStoneModule.class).getPowerStoneManager();
            Set<String> appliedPowerStones = new HashSet<>();
            CustomItemUtils.updateItemLore(meta, this, appliedPowerStones, currentTier, powerStoneManager);

            itemStack.setItemMeta(meta);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "ItemMeta is null for item " + id);
        }

        return itemStack;
    }

    public ItemStack toItemStack() {
        return toItemStack(itemLevel);
    }
}
