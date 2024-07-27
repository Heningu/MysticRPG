package eu.xaru.mysticrpg.content.menus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InventoryManager {

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<String, Inventory> menus;
    private final Map<String, HotbarItem> hotbarItems;

    public InventoryManager(Map<String, Inventory> menus, Map<String, HotbarItem> hotbarItems) {
        this.menus = menus;
        this.hotbarItems = hotbarItems;
    }

    public void saveInventory(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
    }

    public void loadInventory(Player player) {
        ItemStack[] savedInventory = savedInventories.get(player.getUniqueId());
        if (savedInventory != null) {
            player.getInventory().setContents(savedInventory);
        }
    }

    public void clearInventory(Player player) {
        player.getInventory().clear();
    }

    public boolean isMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        String itemName = item.getItemMeta().getDisplayName();
        for (Inventory menu : menus.values()) {
            for (ItemStack menuItem : menu.getContents()) {
                if (menuItem != null && menuItem.hasItemMeta() && itemName.equals(menuItem.getItemMeta().getDisplayName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHotbarItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        String itemName = item.getItemMeta().getDisplayName();
        for (HotbarItem hotbarItem : hotbarItems.values()) {
            if (itemName.equals(hotbarItem.getName())) {
                return true;
            }
        }
        return false;
    }

    public MenuItem getMenuItem(String displayName) {
        for (Inventory menu : menus.values()) {
            for (ItemStack menuItem : menu.getContents()) {
                if (menuItem != null && menuItem.hasItemMeta() && displayName.equals(menuItem.getItemMeta().getDisplayName())) {
                    return mapItemStackToMenuItem(menuItem);
                }
            }
        }
        return null;
    }

    public HotbarItem getHotbarItem(String displayName) {
        for (HotbarItem hotbarItem : hotbarItems.values()) {
            if (displayName.equals(hotbarItem.getName())) {
                return hotbarItem;
            }
        }
        return null;
    }

    private MenuItem mapItemStackToMenuItem(ItemStack itemStack) {
        MenuItem menuItem = new MenuItem();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            menuItem.setName(meta.getDisplayName());
            menuItem.setDescription(meta.getLore() != null ? String.join("\n", meta.getLore()) : "");
        }
        return menuItem;
    }
}
