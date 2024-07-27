package eu.xaru.mysticrpg.content.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import eu.xaru.mysticrpg.Main;

public class NBTUtils {
    private static NBTUtils instance;

    private NBTUtils() {
        // Private constructor to enforce singleton pattern
    }

    public static NBTUtils getInstance() {
        if (instance == null) {
            instance = new NBTUtils();
        }
        return instance;
    }

    public ItemStack setNBTBoolean(ItemStack item, String key, boolean value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(Main.getInstance(), key), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
        item.setItemMeta(meta);
        return item;
    }

    public boolean hasNBTBoolean(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(new NamespacedKey(Main.getInstance(), key), PersistentDataType.BYTE);
    }
}
