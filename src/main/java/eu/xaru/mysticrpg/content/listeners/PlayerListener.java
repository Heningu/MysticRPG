package eu.xaru.mysticrpg.content.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.menus.InventoryManager;
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
            if (displayName.equals(ChatColor.GOLD + "[QUESTS]") || displayName.equals(ChatColor.LIGHT_PURPLE + "[MENU]")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getOldCursor() != null && event.getOldCursor().hasItemMeta()) {
            String displayName = event.getOldCursor().getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.GOLD + "[QUESTS]") || displayName.equals(ChatColor.LIGHT_PURPLE + "[MENU]")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getManagers().getMenuManager().onItemDrop(event);
    }

    @EventHandler
    public void onItemClick(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (displayName.equals(ChatColor.GOLD + "[QUESTS]")) {
                player.performCommand("menu quests");
                event.setCancelled(true);
            } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "[MENU]")) {
                player.performCommand("menu mainmenu");
                event.setCancelled(true);
            }
        }
    }
}
