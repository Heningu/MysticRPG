package eu.xaru.mysticrpg.player.interaction.trading;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TradeMenu {

    public void openTradeMenu(Player p, Player target){

        p.sendMessage(ChatColor.GREEN + "Trade request sent to " + ChatColor.GOLD);

    }

}
