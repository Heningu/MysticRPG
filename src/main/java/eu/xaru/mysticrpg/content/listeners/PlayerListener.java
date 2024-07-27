package eu.xaru.mysticrpg.content.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.menus.InventoryManager;
import eu.xaru.mysticrpg.content.menus.MenuItem;
import eu.xaru.mysticrpg.content.menus.HotbarItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final InventoryManager inventoryManager;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getManagers().getMenuManager().getInventoryManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player base stats, class, XP, etc.
        player.sendMessage(ChatColor.GREEN + "Welcome! Your base stats have been loaded.");
        plugin.getManagers().getPlayerManager().initializePlayer(player);

        // Give hotbar items
        plugin.getManagers().getMenuManager().giveHotbarItemsToPlayer(player);
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            if (inventoryManager.isMenuItem(event.getCurrentItem())) {
                MenuItem menuItem = inventoryManager.getMenuItem(displayName);
                if (menuItem != null) {
                    menuItem.runCommand((Player) event.getWhoClicked());
                }
                event.setCancelled(true);
            } else if (inventoryManager.isHotbarItem(event.getCurrentItem())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getOldCursor() != null && event.getOldCursor().hasItemMeta()) {
            if (inventoryManager.isMenuItem(event.getOldCursor()) || inventoryManager.isHotbarItem(event.getOldCursor())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (inventoryManager.isHotbarItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemClick(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (inventoryManager.isMenuItem(item)) {
                MenuItem menuItem = inventoryManager.getMenuItem(displayName);
                if (menuItem != null) {
                    menuItem.runCommand(player);
                }
                event.setCancelled(true);
            } else if (inventoryManager.isHotbarItem(item)) {
                HotbarItem hotbarItem = inventoryManager.getHotbarItem(displayName);
                if (hotbarItem != null) {
                    player.performCommand(hotbarItem.getCommand());
                }
                event.setCancelled(true);
            }
        }
    }
}