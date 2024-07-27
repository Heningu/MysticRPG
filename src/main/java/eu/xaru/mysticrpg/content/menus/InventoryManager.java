package eu.xaru.mysticrpg.content.menus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InventoryManager {

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<String, Inventory> menus;

    public InventoryManager(Map<String, Inventory> menus) {
        this.menus = menus;
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
}
