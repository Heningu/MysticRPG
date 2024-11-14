package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Set;

public class TradeMenu {

    public static Inventory inv;

    public static int[] right = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44};
    public static int[] left = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39};
    final protected static Set<Integer> leftSet = Set.of(0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39);
    final protected static Set<Integer> rightSet = Set.of(5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44);

    public static void createTradingGUI(Player pPlayer1, Player pPlayer2) {
        if (pPlayer1 != pPlayer2) {
            inv = Bukkit.createInventory(null, 6 * 9, pPlayer1.getName() + " - " + pPlayer2.getName());
            setupTradingInventory();
            TradingHandler.inventoryHandler.put(inv, new Trade(pPlayer1, pPlayer2, inv));
            pPlayer1.openInventory(inv);
            pPlayer2.openInventory(inv);
        }
    }

    private static void setupTradingInventory() {
        int[] placeholders = {4, 13, 22, 31, 40, 49, 46, 47, 48, 50, 51, 52};
        for (int i = 0; i < placeholders.length; i++) {
            inv.setItem(placeholders[i], CustomInventoryManager.createPlaceholder(Material.WHITE_STAINED_GLASS_PANE, " "));
        }
        inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
        inv.setItem(53, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
    }

    protected static int getNextFreeSlot(String side, Inventory pInv) {
        if (side.equals("left")) {
            for (int i = 0; i < left.length; i++) {
                if (pInv.getItem(left[i]) == null) {
                    return left[i];
                }
            }
        } else {
            for (int i = 0; i < right.length; i++) {
                if (pInv.getItem(right[i]) == null) {
                    return right[i];
                }
            }
        }
        return -1;
    }

    public static boolean isAllowedSlot(int clickedSlot, Set<Integer> allowedSlots) {
        return allowedSlots.contains(clickedSlot);
    }

}