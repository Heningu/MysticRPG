package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Set;

public class TradeMenu {

    public static Inventory inv;

    public static int[] right = {14,15,16,23,24,25,32,33,34,41,42,43};
    public static int[] left = {10,11,12,19,20,21,28,29,30,37,38,39};
    final protected static Set<Integer> leftSet = Set.of(10,11,12,19,20,21,28,29,30,37,38,39);
    final protected static Set<Integer> rightSet = Set.of(14,15,16,23,24,25,32,33,34,41,42,43);

    public static void createTradingGUI(Player pPlayer1, Player pPlayer2){
        if(pPlayer1 != pPlayer2) {
            inv = Bukkit.createInventory(null, 6 * 9, pPlayer1.getName() + " - " + pPlayer2.getName());
            setupTradingInventory();
            TradingHandler.inventoryHandler.put(inv, new Trade(pPlayer1, pPlayer2, inv));
            pPlayer1.openInventory(inv);
            pPlayer2.openInventory(inv);
        }
    }

    private static void setupTradingInventory(){
        int[] placeholders = {0,1,2,3,4,5,6,7,8,9,13,17,18,22,26,27,31,35,36,40,44,45,46,47,48,49,50,51,52,53};
        for(int i = 0; i < placeholders.length;i++){
            inv.setItem(placeholders[i], CustomInventoryManager.createPlaceholder(Material.WHITE_STAINED_GLASS_PANE, " "));
        }
        inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
        inv.setItem(53, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
    }

    protected static int getNextFreeSlot(String side, Inventory pInv){
        if(side.equals("left")){
            for(int i = 0; i < left.length; i++){
                if(pInv.getItem(left[i]) == null){
                    return i;
                }
            }
        }else{
            for(int i = 0; i < right.length; i++){
                if(pInv.getItem(right[i]) == null){
                    return i;
                }
            }
        }
        return 0;
    }

    public static boolean isAllowedSlot(int clickedSlot, Set<Integer> allowedSlots) {
        return allowedSlots.contains(clickedSlot);
    }

}
