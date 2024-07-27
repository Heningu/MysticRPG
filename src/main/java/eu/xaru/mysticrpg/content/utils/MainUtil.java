package eu.xaru.mysticrpg.content.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class MainUtil {
    private static MainUtil instance;

    private MainUtil() {
        // Private constructor to enforce singleton pattern
    }

    public static MainUtil getInstance() {
        if (instance == null) {
            instance = new MainUtil();
        }
        return instance;
    }

    public void log(String message) {
        Bukkit.getLogger().info(message);
    }

    public ItemStack createMenuItem(Material material, String name, boolean hideEnchants, String description) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (hideEnchants) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (description != null && !description.isEmpty()) {
                meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', description)));
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void giveItemToPlayer(Player player, ItemStack itemStack, int slot) {
        player.getInventory().setItem(slot, itemStack);
    }
}
