/*package eu.xaru.mysticrpg.stats;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class StatListener implements Listener {
    private final StatManager statManager;
    private final StatMenu statMenu;

    public StatListener(StatManager statManager, StatMenu statMenu) {
        this.statManager = statManager;
        this.statMenu = statMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof StatMenu) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals("Increase Vitality")) {
                statManager.increaseVitality(player);
            } else if (displayName.equals("Increase Intelligence")) {
                statManager.increaseIntelligence(player);
            } else if (displayName.equals("Increase Dexterity")) {
                statManager.increaseDexterity(player);
            } else if (displayName.equals("Increase Strength")) {
                statManager.increaseStrength(player);
            }

            // Refresh the inventory to show updated stats
            statMenu.openStatMenu(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof StatMenu) {
            event.setCancelled(true);
        }
    }
}
*/