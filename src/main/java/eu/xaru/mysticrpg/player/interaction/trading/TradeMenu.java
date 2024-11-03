package eu.xaru.mysticrpg.player.interaction.trading;

import eu.xaru.mysticrpg.cores.MysticCore;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.TextComponent;
import java.util.HashMap;

public class TradeMenu {
    private final MysticCore plugin = MysticCore.getPlugin(MysticCore.class);
    protected HashMap<Player, Player> trades = new HashMap<Player, Player>();


    public void sendTradeInvite(Player player, Player target){
        if(trades.containsKey(target) && trades.containsValue(player)){
            acceptTradeRequest(player, target);
        }else if(trades.containsKey(player)){
            player.sendMessage(ChatColor.RED + "You already have an outgoing Trade request!");return;
        }else if(trades.containsKey(target)){
            player.sendMessage(ChatColor.RED + "This player already has an outgoing Trade request!");return;
        }else if(trades.containsValue(player)){
            player.sendMessage(ChatColor.RED + "You already have an incoming Trade Request!");return;
        }else if(trades.containsValue(target)){
            player.sendMessage(ChatColor.RED + "This Player already has an incoming Trade request!");return;
        }else{
            trades.put(player, target);
            TextComponent message = new TextComponent(ChatColor.GREEN + "Trade request sent to " + ChatColor.RESET + target.getName());
            //Implement TradeCommand to clickable text
            player.sendMessage();
            target.sendMessage(ChatColor.GREEN + "Trade request from " + ChatColor.RESET + player.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> removeTradeRequest(player, target), 200L);

        }
    }
    private void removeTradeRequest(Player player, Player target) {
        if (trades.containsKey(player) && trades.get(player).equals(target)) {
            trades.remove(player);
            player.sendMessage(ChatColor.RED + "Trade request to " + target.getName() + " has expired.");
            target.sendMessage(ChatColor.RED + "Trade request from " + player.getName() + " has expired.");
        }
    }

    public void acceptTradeRequest(Player player, Player target){
        trades.remove(player);trades.remove(target);
        new Trade(player, target);
    }


}
