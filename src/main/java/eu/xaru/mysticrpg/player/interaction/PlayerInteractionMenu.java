package eu.xaru.mysticrpg.player.interaction;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PlayerInteractionMenu {

    private MysticCore plugin;

    public static void openPlayerInteractionMenu(Player p, Player target) {

        Inventory inv = Bukkit.createInventory(null, 6*9, target.getName());
        CustomInventoryManager.fillEmptySlots(inv, CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " "));
        //Slot setup für Interaction Menu

    }

}
