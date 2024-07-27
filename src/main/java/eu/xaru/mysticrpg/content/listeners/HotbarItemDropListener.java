package eu.xaru.mysticrpg.content.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.menus.InventoryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class HotbarItemDropListener implements Listener {

    private final InventoryManager inventoryManager;

    public HotbarItemDropListener(Main plugin) {
        this.inventoryManager = plugin.getManagers().getMenuManager().getInventoryManager();
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (inventoryManager.isHotbarItem(item)) {
            event.setCancelled(true);
        }
    }
}
