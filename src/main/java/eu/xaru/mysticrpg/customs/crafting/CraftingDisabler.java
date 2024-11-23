package eu.xaru.mysticrpg.customs.crafting;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CraftingDisabler implements Listener {

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        // Cancel crafting in the default crafting interface
        if (event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.WORKBENCH) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getClickedBlock() != null) {
            Material blockType = event.getClickedBlock().getType();
            if (blockType == Material.CRAFTING_TABLE) {
                // Check if the action is a right-click
                if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                    // Cancel the event to prevent the default crafting table from opening
                    event.setCancelled(true);
                } else {
                    // Allow other interactions, such as breaking the block
                    event.setCancelled(false);
                }
            }
        }
    }
}
