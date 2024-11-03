package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class Trade {

    Inventory inv;
    Inventory pInv;
    Inventory tInv;
    Player player;
    Player target;

    public int[] right = {14,15,16,23,24,25,32,33,34,41,42,43};
    public int[] left = {10,11,12,19,20,21,28,29,30,37,38,39};

    public Trade(Player pPlayer, Player pTarget){
        player = pPlayer;
        target = pTarget;
        inv = Bukkit.createInventory(null, 6*9, pPlayer.getName() + " - " + pTarget.getName());
        setupTradingInventory();
        pInv = inv;tInv = inv;

    }

    private void setupTradingInventory(){
        int[] placeholders = {0,1,2,3,4,5,6,7,8,9,13,17,18,22,26,27,31,35,36,40,44,45,46,47,48,49,50,51,52,53};
        for(int i = placeholders.length; i < 54;i++){
            inv.setItem(placeholders[i], CustomInventoryManager.createPlaceholder(Material.WHITE_STAINED_GLASS_PANE, " "));
        }
    }
}
