package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.guis.player.TradeGUI;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

/**
 * TradeListener handles inventory interactions related to trading.
 */
public class TradeListener implements Listener {

    private final TradeManager tradeManager = TradeManager.getInstance();
    private final TradeRequestManager tradeRequestManager;

    /**
     * Constructor to initialize TradeListener.
     *
     * @param tradeRequestManager The TradeRequestManager instance.
     */
    public TradeListener(TradeRequestManager tradeRequestManager) {
        this.tradeRequestManager = tradeRequestManager;
        Bukkit.getPluginManager().registerEvents(this, MysticCore.getPlugin(MysticCore.class));
    }

    /**
     * Handles inventory click events to manage adding/removing items from the trade.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        TradeSession session = tradeManager.getTradeSession(player);
        if (session == null) return;

        // Check if the clicked inventory is the player's own inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            // Check if the player is holding an item and left-clicking with shift to add to trade
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                ItemStack itemToAdd = event.getCurrentItem().clone();
                boolean added = session.addItem(player, itemToAdd);
                if (added) {
                    player.getInventory().removeItem(event.getCurrentItem());
                    player.sendMessage(ChatColor.GREEN + "Added " + itemToAdd.getType().toString() + " to the trade.");
                } else {
                    player.sendMessage(ChatColor.RED + "Cannot add this item to the trade.");
                }
            }
        }
    }

    /**
     * Unregisters all event handlers.
     */
    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
