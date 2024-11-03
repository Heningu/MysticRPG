package eu.xaru.mysticrpg.player.interaction;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.HeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractionMenu {

    private MysticCore plugin;
    final protected static String interactionInventoryName = "Interaction Inventory";

    public static void openPlayerInteractionMenu(Player p, Player target) {

        Inventory inv = Bukkit.createInventory(null, 1*9, interactionInventoryName);
        CustomInventoryManager.fillEmptySlots(inv, CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " "));
        //Slot setup für Interaction Menu

        inv.setItem(1, HeadUtils.getPlayerHead(target, "Send party invite"));
        inv.setItem(4, HeadUtils.createCustomHead("ewogICJ0aW1lc3RhbXAiIDogMTczMDY1Njk5OTgwMSwKICAicHJvZmlsZUlkIiA6ICI1MmNjZGNlODdjNzA0Zjg5ODc1ODIzMWU0MmJjM2NjMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNQ1RlR3R5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M1MmQxOGViNDc2YzUyMDQyM2JhZTZhNGZiN2VmM2RhZTgxYTMwYjg2ODJlOGJhYWY4N2EwNTU1NjNhNjEyZmQiCiAgICB9CiAgfQp9", "Send Trade invite"));
        inv.setItem(7, HeadUtils.getPlayerHead(target, "Add Friend"));
    }

}
