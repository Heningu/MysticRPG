package eu.xaru.mysticrpg.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    private static final NamespacedKey identifierKey = new NamespacedKey("mysticrpg", "identifier");

    public static boolean isCustomItem(ItemStack item, String identifier) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return identifier.equals(meta.getPersistentDataContainer().get(identifierKey, PersistentDataType.STRING));
    }

    public static void setCustomItem(ItemStack item, String identifier) {
        if (item != null && item.getType() != Material.AIR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(identifierKey, PersistentDataType.STRING, identifier);
                item.setItemMeta(meta);
            }
        }
    }
}
