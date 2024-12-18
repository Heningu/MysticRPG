package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStone;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import eu.xaru.mysticrpg.customs.items.sets.ItemSet;
import eu.xaru.mysticrpg.customs.items.sets.SetManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;
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

    public static Category getCategory(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey categoryKey = new NamespacedKey(plugin, "custom_item_category");
        String categoryName = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        if (categoryName == null) return null;

        try {
            return Category.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Map<StatType, Double> getItemStats(ItemStack item) {
        EnumMap<StatType, Double> stats = new EnumMap<>(StatType.class);
        if (!isCustomItem(item)) {
            DebugLogger.getInstance().log(Level.INFO, "getItemStats: Not a custom item.");
            return stats;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            DebugLogger.getInstance().log(Level.INFO, "getItemStats: Meta is null.");
            return stats;
        }

        NamespacedKey statsKey = new NamespacedKey(plugin, "custom_item_attributes");
        String statsData = meta.getPersistentDataContainer().get(statsKey, PersistentDataType.STRING);
        if (statsData != null && !statsData.isEmpty()) {
            String[] pairs = statsData.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    try {
                        StatType type = StatType.valueOf(kv[0]);
                        double val = Double.parseDouble(kv[1]);
                        stats.put(type, val);
                    } catch (Exception e) {
                        DebugLogger.getInstance().log(Level.WARNING, "getItemStats: Could not parse stat pair " + pair);
                    }
                }
            }
        } else {
            DebugLogger.getInstance().log(Level.INFO, "getItemStats: No attribute data found for item " + item.getType());
        }

        return stats;
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

        if (appliedPowerStones.contains(powerStone.getId())) {
            return false;
        }

        if (appliedPowerStones.size() >= customItem.getPowerStoneSlots()) {
            return false;
        }

        appliedPowerStones.add(powerStone.getId());
        meta.getPersistentDataContainer().set(appliedPowerStonesKey, PersistentDataType.STRING, String.join(",", appliedPowerStones));

        updateItemLore(meta, customItem, appliedPowerStones, getCurrentTier(meta, customItem), powerStoneManager);

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

        int newTier = currentTier + 1;
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, newTier);

        Map<String, AttributeData> attributesForTier = customItem.getTierAttributes().get(newTier);
        if (attributesForTier == null) attributesForTier = customItem.getAttributes();

        meta.setAttributeModifiers(null);

        for (Map.Entry<String, AttributeData> entry : attributesForTier.entrySet()) {
            String attrName = entry.getKey().toUpperCase();
            AttributeData attributeData = entry.getValue();

            var attribute = customItem.getAttributeByName(attrName);
            if (attribute != null && attributeData != null) {
                var modifier = new AttributeModifier(
                        UUID.randomUUID(),
                        attribute.name(),
                        attributeData.getValue(),
                        attributeData.getOperation(),
                        customItem.getEquipmentSlot()
                );
                meta.addAttributeModifier(attribute, modifier);
            }
        }

        // **Important**: Update the persistent data "custom_item_attributes" to reflect the new tier
        NamespacedKey statsKey = new NamespacedKey(plugin, "custom_item_attributes");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, AttributeData> attr : attributesForTier.entrySet()) {
            double value = attr.getValue().getValue();
            if (sb.length() > 0) sb.append(";");
            sb.append(attr.getKey().toUpperCase()).append(":").append(value);
        }
        meta.getPersistentDataContainer().set(statsKey, PersistentDataType.STRING, sb.toString());

        NamespacedKey appliedPowerStonesKey = new NamespacedKey(plugin, "applied_power_stones");
        String appliedPowerStonesStr = meta.getPersistentDataContainer().get(appliedPowerStonesKey, PersistentDataType.STRING);

        Set<String> appliedPowerStones = new HashSet<>();
        if (appliedPowerStonesStr != null && !appliedPowerStonesStr.isEmpty()) {
            appliedPowerStones.addAll(Arrays.asList(appliedPowerStonesStr.split(",")));
        }

        updateItemLore(meta, customItem, appliedPowerStones, newTier, powerStoneManager);

        itemStack.setItemMeta(meta);
        return true;
    }

    public static boolean deconstructItem(ItemStack itemStack, PowerStoneManager powerStoneManager) {
        if (!isCustomItem(itemStack)) return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey appliedPowerStonesKey = new NamespacedKey(plugin, "applied_power_stones");
        String appliedPowerStonesStr = meta.getPersistentDataContainer().get(appliedPowerStonesKey, PersistentDataType.STRING);

        if (appliedPowerStonesStr == null || appliedPowerStonesStr.isEmpty()) {
            return false;
        }

        meta.getPersistentDataContainer().remove(appliedPowerStonesKey);

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return false;

        updateItemLore(meta, customItem, new HashSet<>(), getCurrentTier(meta, customItem), powerStoneManager);

        itemStack.setItemMeta(meta);
        return true;
    }

    static void updateItemLore(ItemMeta meta, CustomItem customItem, Set<String> appliedPowerStones, int currentTier, PowerStoneManager powerStoneManager) {
        List<String> finalLore = new ArrayList<>();

        finalLore.add(customItem.getRarity().getColor() + customItem.getRarity().name());
        finalLore.add(ChatColor.GRAY + "Category: " + customItem.getCategory().name());

        if (customItem.getSetId() != null) {
            finalLore.add(ChatColor.GOLD + "This is part of a set");
            String niceSetName = SetManager.getInstance().formatSetName(customItem.getSetId());
            finalLore.add(ChatColor.GOLD + "Set: " + niceSetName);

            ItemSet itemSet = SetManager.getInstance().getSet(customItem.getSetId());
            if (itemSet != null && !itemSet.getPieceBonuses().isEmpty()) {
                finalLore.add(ChatColor.DARK_AQUA + "Set Effects:");
                for (Map.Entry<Integer, Map<StatType, Double>> thresholdEntry : itemSet.getPieceBonuses().entrySet()) {
                    int pieces = thresholdEntry.getKey();
                    Map<StatType, Double> bonuses = thresholdEntry.getValue();
                    StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.DARK_AQUA).append(pieces).append(" Pieces:");
                    for (Map.Entry<StatType, Double> bonus : bonuses.entrySet()) {
                        double percent = bonus.getValue() * 100.0;
                        sb.append(" ").append(formatSetBonus(bonus.getKey(), percent));
                    }
                    finalLore.add(sb.toString());
                }
            }
        }

        if (customItem.isUseTierSystem()) {
            String tierStars = getTierStars(currentTier, customItem.getItemMaxLevel());
            finalLore.add(Utils.getInstance().$(tierStars));
        }

        if (customItem.isUsePowerStones()) {
            int maxSlots = customItem.getPowerStoneSlots();
            int freeSlots = maxSlots - appliedPowerStones.size();
            finalLore.add(Utils.getInstance().$("Free Powerstone Slots: " + freeSlots));
        }

        finalLore.add("");

        Map<String, AttributeData> baseAttrs = customItem.getAttributes();
        Map<String, AttributeData> tierAttrs = customItem.isUseTierSystem() ?
                customItem.getTierAttributes().getOrDefault(currentTier, baseAttrs) : baseAttrs;

        // Show attributes with differences ( +X ) again
        List<String> attributeLines = formatAttributesForLore(baseAttrs, tierAttrs);
        finalLore.addAll(attributeLines);

        if (!attributeLines.isEmpty()) {
            finalLore.add("");
        }

        if (customItem.getLore() != null && !customItem.getLore().isEmpty()) {
            finalLore.addAll(customItem.getLore().stream()
                    .map(line -> Utils.getInstance().$(line))
                    .collect(Collectors.toList()));
        }

        if (!appliedPowerStones.isEmpty()) {
            finalLore.add("");
            finalLore.add(Utils.getInstance().$("Power Stones:"));
            for (String psId : appliedPowerStones) {
                PowerStone ps = powerStoneManager.getPowerStone(psId);
                if (ps != null) {
                    finalLore.add(Utils.getInstance().$("- " + Utils.getInstance().$(ps.getName())));
                }
            }
        }

        meta.setLore(finalLore);
    }

    private static String formatSetBonus(StatType stat, double percent) {
        return "+" + formatDouble(percent) + "% " + prettifyStatName(stat);
    }

    /**
     * Show final tier value and also show ( +X ) difference from base.
     */
    private static List<String> formatAttributesForLore(Map<String, AttributeData> baseAttrs, Map<String, AttributeData> tierAttrs) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, AttributeData> entry : tierAttrs.entrySet()) {
            String attrName = entry.getKey();
            AttributeData tierData = entry.getValue();
            double tierVal = tierData.getValue();
            double baseVal = baseAttrs.getOrDefault(attrName, new AttributeData(0, tierData.getOperation())).getValue();

            StatType statType;
            try {
                statType = StatType.valueOf(attrName.toUpperCase());
            } catch (Exception e) {
                continue;
            }

            boolean isPercentage = (statType == StatType.CRIT_CHANCE || statType == StatType.CRIT_DAMAGE || statType == StatType.ATTACK_SPEED || statType == StatType.MOVEMENT_SPEED);

            String statDisplayName = prettifyStatName(statType);
            String tierStr = isPercentage ? formatDouble(tierVal) + "%" : "+" + formatDouble(tierVal);
            double diff = tierVal - baseVal;
            String diffStr = "";
            if (Math.abs(diff) > 0.0001) {
                // Show difference only if there's actually a difference
                diffStr = isPercentage ? " (+" + formatDouble(diff) + "%)" : " (+" + formatDouble(diff) + ")";
            }

            lines.add(ChatColor.YELLOW + statDisplayName + ": " + ChatColor.WHITE + tierStr + diffStr);
        }
        return lines;
    }

    private static String prettifyStatName(StatType stat) {
        switch (stat) {
            case HEALTH: return "Health";
            case DEFENSE: return "Defense";
            case STRENGTH: return "Strength";
            case INTELLIGENCE: return "Intelligence";
            case CRIT_CHANCE: return "Crit Chance";
            case CRIT_DAMAGE: return "Crit Damage";
            case ATTACK_SPEED: return "Bonus Attack Speed";
            case HEALTH_REGEN: return "Health Regen";
            case MOVEMENT_SPEED: return "Speed";
            case MANA: return "Mana";
            default: return stat.name().replace("_", " ");
        }
    }

    private static String formatDouble(double val) {
        if (val == (long) val)
            return String.valueOf((long) val);
        else
            return String.format("%.1f", val);
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

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        itemStack.setItemMeta(meta);
    }

    public static List<CustomItem> getItemsByCategory(Category category) {
        return itemManager.getItemsByCategory(category);
    }

    public static Category[] getAllCategories() {
        return itemManager.getAllCategories();
    }

    public static CustomItem fromItemStack(ItemStack itemStack) {
        NamespacedKey idKey = new NamespacedKey(MysticCore.getInstance(), "custom_item_id");
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) {
            String id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
            return ModuleManager.getInstance().getModuleInstance(CustomItemModule.class).getCustomItemById(id);
        }
        return null;
    }
}
