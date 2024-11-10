package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.customs.items.powerstones.PowerStone;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class CustomItemUtils {

    private static final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(CustomItemUtils.class);
    private static final ItemManager itemManager = new ItemManager(); // Ensure this is properly initialized

    public static boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        return meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    public static boolean canApplyPowerStone(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        CustomItem customItem = itemManager.getCustomItem(itemId);
        return customItem != null && customItem.isUsePowerStones();
    }

    public static boolean applyPowerStoneToItem(ItemStack itemStack, PowerStone powerStone, PowerStoneManager powerStoneManager) {
        if (!canApplyPowerStone(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey appliedPowerStonesKey = new NamespacedKey(plugin, "applied_power_stones");
        String appliedPowerStonesStr = meta.getPersistentDataContainer().get(appliedPowerStonesKey, PersistentDataType.STRING);

        Set<String> appliedPowerStones = new HashSet<>();
        if (appliedPowerStonesStr != null && !appliedPowerStonesStr.isEmpty()) {
            appliedPowerStones.addAll(Arrays.asList(appliedPowerStonesStr.split(",")));
        }

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return false;

        // Check if power stone is already applied
        if (appliedPowerStones.contains(powerStone.getId())) {
            return false;
        }

        // Check available slots
        if (appliedPowerStones.size() >= customItem.getPowerStoneSlots()) {
            return false;
        }

        // Apply power stone
        appliedPowerStones.add(powerStone.getId());
        meta.getPersistentDataContainer().set(appliedPowerStonesKey, PersistentDataType.STRING, String.join(",", appliedPowerStones));

        // Update lore
        updateItemLore(meta, customItem, appliedPowerStones, getCurrentTier(meta, customItem), powerStoneManager);

        // Apply any immediate effects if needed
        // For example, if the power stone modifies attributes directly upon application

        itemStack.setItemMeta(meta);
        return true;
    }

    public static boolean canUpgradeItem(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return false;

        if (!customItem.isUseTierSystem()) return false;

        // Check if item has not reached max tier
        NamespacedKey tierKey = new NamespacedKey(plugin, "custom_item_tier");
        Integer currentTier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        if (currentTier == null) currentTier = customItem.getItemLevel();

        int maxTier = customItem.getItemMaxLevel();

        return currentTier < maxTier;
    }

    public static boolean upgradeItem(ItemStack itemStack, PowerStoneManager powerStoneManager) {
        if (!canUpgradeItem(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (itemId == null) return false;

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return false;

        NamespacedKey tierKey = new NamespacedKey(plugin, "custom_item_tier");
        Integer currentTier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        if (currentTier == null) currentTier = customItem.getItemLevel();

        int maxTier = customItem.getItemMaxLevel();

        if (currentTier >= maxTier) return false; // Already at max tier

        // Increase the tier
        int newTier = currentTier + 1;
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, newTier);

        // Update attributes for the new tier
        Map<String, AttributeData> attributesForTier = customItem.getTierAttributes().get(newTier);
        if (attributesForTier == null) attributesForTier = customItem.getAttributes();

        // Clear existing attribute modifiers
        meta.setAttributeModifiers(null);

        // Add new attribute modifiers
        for (Map.Entry<String, AttributeData> entry : attributesForTier.entrySet()) {
            String attrName = entry.getKey().toUpperCase();
            AttributeData attributeData = entry.getValue();

            Attribute attribute = customItem.getAttributeByName(attrName);
            if (attribute != null && attributeData != null) {
                AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(),
                        attribute.name(),
                        attributeData.getValue(),
                        attributeData.getOperation(),
                        customItem.getEquipmentSlot()
                );
                meta.addAttributeModifier(attribute, modifier);
            }
        }

        // Get applied power stones
        NamespacedKey appliedPowerStonesKey = new NamespacedKey(plugin, "applied_power_stones");
        String appliedPowerStonesStr = meta.getPersistentDataContainer().get(appliedPowerStonesKey, PersistentDataType.STRING);

        Set<String> appliedPowerStones = new HashSet<>();
        if (appliedPowerStonesStr != null && !appliedPowerStonesStr.isEmpty()) {
            appliedPowerStones.addAll(Arrays.asList(appliedPowerStonesStr.split(",")));
        }

        // Update lore
        updateItemLore(meta, customItem, appliedPowerStones, newTier, powerStoneManager);

        itemStack.setItemMeta(meta);

        return true;
    }

    public static boolean deconstructItem(ItemStack itemStack, PowerStoneManager powerStoneManager) {
        if (!isCustomItem(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        // Remove applied power stones
        NamespacedKey appliedPowerStonesKey = new NamespacedKey(plugin, "applied_power_stones");
        String appliedPowerStonesStr = meta.getPersistentDataContainer().get(appliedPowerStonesKey, PersistentDataType.STRING);

        if (appliedPowerStonesStr == null || appliedPowerStonesStr.isEmpty()) {
            // No power stones to remove
            return false;
        }

        // Clear the applied power stones
        meta.getPersistentDataContainer().remove(appliedPowerStonesKey);

        // Get the custom item
        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return false;

        // Get current tier
        int currentTier = getCurrentTier(meta, customItem);

        // Update lore
        updateItemLore(meta, customItem, new HashSet<>(), currentTier, powerStoneManager);

        itemStack.setItemMeta(meta);

        return true;
    }

    private static void updateItemLore(ItemMeta meta, CustomItem customItem, Set<String> appliedPowerStones, int currentTier, PowerStoneManager powerStoneManager) {
        List<String> finalLore = new ArrayList<>();

        // First line: Rarity
        finalLore.add(customItem.getRarity().getColor() + customItem.getRarity().name());

        // Second line: Tier stars
        if (customItem.isUseTierSystem()) {
            String tierStars = getTierStars(currentTier, customItem.getItemMaxLevel());
            finalLore.add(ChatColor.GOLD + tierStars);
        }

        // Add Free Powerstone Slots if applicable
        if (customItem.isUsePowerStones()) {
            int maxSlots = customItem.getPowerStoneSlots();
            int freeSlots = maxSlots - appliedPowerStones.size();
            finalLore.add(ChatColor.GRAY + "Free Powerstone Slots: " + freeSlots);
        }

        // Add a blank line
        finalLore.add("");

        // Add the actual lore/description
        if (customItem.getLore() != null && !customItem.getLore().isEmpty()) {
            finalLore.addAll(customItem.getLore().stream()
                    .map(line -> ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
        }

        // Add applied power stones
        if (!appliedPowerStones.isEmpty()) {
            finalLore.add("");
            finalLore.add(ChatColor.AQUA + "Power Stones:");
            for (String psId : appliedPowerStones) {
                PowerStone ps = powerStoneManager.getPowerStone(psId);
                if (ps != null) {
                    finalLore.add(ChatColor.DARK_GRAY + "- " + ChatColor.translateAlternateColorCodes('&', ps.getName()));
                }
            }
        }

        meta.setLore(finalLore);
    }

    private static String getTierStars(int currentTier, int maxTier) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < currentTier; i++) {
            stars.append("★");
        }
        for (int i = currentTier; i < maxTier; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    private static int getCurrentTier(ItemMeta meta, CustomItem customItem) {
        NamespacedKey tierKey = new NamespacedKey(plugin, "custom_item_tier");
        Integer currentTier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        if (currentTier == null) currentTier = customItem.getItemLevel();
        return currentTier;
    }

    public static boolean isUpgradeStone(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        return "upgrade_stone".equals(itemId);
    }

    public static void applyEnchantedEffect(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // Add enchanted effect without actual enchantments
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        itemStack.setItemMeta(meta);
    }

    // Additional utility methods can be added here if needed

}
