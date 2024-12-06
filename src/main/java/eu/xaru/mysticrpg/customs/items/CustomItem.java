package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
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
import java.util.stream.Collectors;

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
                      boolean usePowerStones, int powerStoneSlots, String armorType) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.rarity = rarity;
        this.category = category;
        this.customModelData = customModelData;
        this.lore = lore;
        this.attributes = attributes;
        this.enchantments = enchantments;
        this.enchantedEffect = enchantedEffect;
        this.useTierSystem = useTierSystem;
        this.itemLevel = itemLevel;
        this.itemMaxLevel = itemMaxLevel;
        this.tierAttributes = tierAttributes;
        this.usePowerStones = usePowerStones;
        this.powerStoneSlots = powerStoneSlots;
        this.armorType = armorType;
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

    public Attribute getAttributeByName(String name) {
        try {
            return Attribute.valueOf("GENERIC_" + name);
        } catch (IllegalArgumentException e) {
            // Try without GENERIC_ prefix
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ex) {
                // Log an error message
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
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // Set display name to just the item name
            String displayName = Utils.getInstance().$(name);
            meta.setDisplayName(displayName);

            // Set lore
            List<String> finalLore = new ArrayList<>();

            // First line: Rarity
            finalLore.add(rarity.getColor() + rarity.name());

            // Second line: Category
            finalLore.add(ChatColor.GRAY + "Category: " + category.name());

            // Third line: Tier stars (if applicable)
            if (useTierSystem) {
                String tierStars = getTierStars(currentTier, itemMaxLevel);
                finalLore.add(Utils.getInstance().$(tierStars));
            }

            // Add Free Powerstone Slots if applicable
            if (usePowerStones) {
                int freeSlots = powerStoneSlots; // Initially, all slots are free
                finalLore.add(Utils.getInstance().$("Free Powerstone Slots: " + freeSlots));
            }

            // Add a blank line
            finalLore.add("");

            // Add the actual lore/description
            if (lore != null && !lore.isEmpty()) {
                finalLore.addAll(lore.stream()
                        .map(line -> Utils.getInstance().$(line))
                        .collect(Collectors.toList()));
            }

            meta.setLore(finalLore);

            // Set CustomModelData
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Add enchanted effect without actual enchantments
            if (enchantedEffect) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(meta);
                itemStack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            } else {
                itemStack.setItemMeta(meta);
            }

            // Add enchantments
            if (enchantments != null && !enchantments.isEmpty()) {
                enchantments.forEach((enchantKey, level) -> {
                    org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey.toLowerCase()));
                    if (enchantment != null) {
                        itemStack.addUnsafeEnchantment(enchantment, level);
                    }
                });
            }

            // Clear all attribute modifiers, including default ones
            meta.setAttributeModifiers(null);

            // Add attributes for the current tier
            Map<String, AttributeData> attributesForTier;
            if (useTierSystem) {
                attributesForTier = tierAttributes.getOrDefault(currentTier, attributes);
            } else {
                attributesForTier = attributes;
            }

            if (attributesForTier != null && !attributesForTier.isEmpty()) {
                for (Map.Entry<String, AttributeData> entry : attributesForTier.entrySet()) {
                    String attrName = entry.getKey().toUpperCase();
                    AttributeData attributeData = entry.getValue();

                    Attribute attribute = getAttributeByName(attrName);
                    if (attribute != null && attributeData != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                attribute.name(),
                                attributeData.getValue(),
                                attributeData.getOperation(),
                                getEquipmentSlot()
                        );
                        meta.addAttributeModifier(attribute, modifier);
                    }
                }
            }

            // Store item ID in PersistentDataContainer
            NamespacedKey idKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_id");
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

            // Store category in PersistentDataContainer
            NamespacedKey categoryKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_category");
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category.name());

            // Store current tier in PersistentDataContainer if tier system is used
            if (useTierSystem) {
                NamespacedKey tierKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_tier");
                meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, currentTier);

                // Store max tier
                NamespacedKey maxTierKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "custom_item_max_tier");
                meta.getPersistentDataContainer().set(maxTierKey, PersistentDataType.INTEGER, itemMaxLevel);
            }

            // Store power stone slots if used
            if (usePowerStones) {
                NamespacedKey powerStoneSlotsKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "power_stone_slots");
                meta.getPersistentDataContainer().set(powerStoneSlotsKey, PersistentDataType.INTEGER, powerStoneSlots);
            }

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    // Overloaded method for initial item creation with starting tier
    public ItemStack toItemStack() {
        return toItemStack(itemLevel);
    }

    private String getTierStars(int currentTier, int maxTier) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < currentTier; i++) {
            stars.append("★");
        }
        for (int i = currentTier; i < maxTier; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    /**
     * Formats the remaining time into a human-readable string.
     *
     * @param millis The time in milliseconds.
     * @return A formatted string representing the time left.
     */
    private String formatTimeLeft(long millis) {
        if (millis < 0) {
            return ChatColor.RED + "Expired";
        }
        long seconds = millis / 1000 % 60;
        long minutes = millis / (1000 * 60) % 60;
        long hours = millis / (1000 * 60 * 60) % 24;
        long days = millis / (1000 * 60 * 60 * 24);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }
}
