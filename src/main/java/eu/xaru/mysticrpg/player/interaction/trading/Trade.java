//package eu.xaru.mysticrpg.player.interaction.trading;
//
//import eu.xaru.mysticrpg.cores.MysticCore;
//import eu.xaru.mysticrpg.utils.CustomInventoryManager;
//import net.md_5.bungee.api.chat.ClickEvent;
//import net.md_5.bungee.api.chat.ComponentBuilder;
//import net.md_5.bungee.api.chat.HoverEvent;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//
//import net.md_5.bungee.api.chat.TextComponent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//
//import java.util.Arrays;
//import java.util.Map;
//import java.util.WeakHashMap;
//
//public class Trade {
//    private final static MysticCore plugin = MysticCore.getPlugin(MysticCore.class);
//
//    public Map<Integer, Integer> player1UsedItems = new WeakHashMap<>();
//    public Map<Integer, Integer> player2UsedItems = new WeakHashMap<>();
//    public Player player1;
//    public Player player2;
//    public boolean player1Ready = false;
//    public boolean player2Ready = false;
//    final private Inventory inv;
//    public boolean tradeCompleted = false;
//    public boolean cancelled = false;
//
//    public Trade(Player pPlayer1, Player pPlayer2, Inventory pInv) {
//        this.player1 = pPlayer1;
//        this.player2 = pPlayer2;
//        this.inv = pInv;
//
//    }
//
//    public static void sendTradeInvite(Player player, Player target) {
//        if (TradingHandler.trades.containsKey(target) && TradingHandler.trades.containsValue(player)) {
//            acceptTradeRequest(player, target);
//        } else if (TradingHandler.trades.containsKey(player)) {
//            player.sendMessage(ChatColor.RED + "You already have an outgoing Trade request!");
//            return;
//        } else if (TradingHandler.trades.containsKey(target)) {
//            player.sendMessage(ChatColor.RED + "This player already has an outgoing Trade request!");
//            return;
//        } else if (TradingHandler.trades.containsValue(player)) {
//            player.sendMessage(ChatColor.RED + "You already have an incoming Trade Request!");
//            return;
//        } else if (TradingHandler.trades.containsValue(target)) {
//            player.sendMessage(ChatColor.RED + "This Player already has an incoming Trade request!");
//            return;
//        } else {
//            TradingHandler.trades.put(player, target);
//            String messageTo = ChatColor.GREEN + "Trade request sent to " + ChatColor.RESET + target.getName();
//
//            player.sendMessage(messageTo);
//            TextComponent messageFrom = new TextComponent(ChatColor.GREEN + "Trade request from " + ChatColor.RESET + player.getName());
//            messageFrom.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade " + player.getName()));
//            messageFrom.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here accept trading request").color(net.md_5.bungee.api.ChatColor.WHITE).create()));
//            target.spigot().sendMessage(messageFrom);
//            Bukkit.getScheduler().runTaskLater(plugin, () -> removeTradeRequest(player, target), 200L);
//
//        }
//    }
//
//    private static void removeTradeRequest(Player player, Player target) {
//        if (TradingHandler.trades.containsKey(player) && TradingHandler.trades.get(player).equals(target)) {
//            TradingHandler.trades.remove(player);
//            player.sendMessage(ChatColor.RED + "Trade request to " + target.getName() + " has expired.");
//            target.sendMessage(ChatColor.RED + "Trade request from " + player.getName() + " has expired.");
//        }
//    }
//
//    public static void acceptTradeRequest(Player player, Player target) {
//        TradingHandler.trades.remove(player);
//        TradingHandler.trades.remove(target);
//        TradeMenu.createTradingGUI(player, target);
//
//    }
//
//    public void modifySlotReadyCheck(Inventory inv) {
//        if (player1Ready) {
//            inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
//            player1Ready = false;
//        } else if (player2Ready) {
//            inv.setItem(53, CustomInventoryManager.createPlaceholder(Material.RED_WOOL, "§cNOT READY"));
//            player2Ready = false;
//        }
//    }
//
//    public void checkReady() {
//        if (player1Ready && player2Ready) {
//            completeTrade();
//        }
//    }
//
//    public void completeTrade() {
//        tradeCompleted = true;
//        for (int i = 0; i < TradeMenu.left.length; i++) {
//            if (inv.getItem(TradeMenu.left[i]) != null) {
//                player1.getInventory().setItem(player1UsedItems.get(TradeMenu.left[i]), null);
//            }
//        }
//        for (int i = 0; i < TradeMenu.right.length; i++) {
//            if (inv.getItem(TradeMenu.right[i]) != null) {
//                player2.getInventory().setItem(player2UsedItems.get(TradeMenu.right[i]), null);
//            }
//        }
//
//        for (int i = 0; i < TradeMenu.left.length; i++) {
//            if (inv.getItem(TradeMenu.left[i]) != null) {
//                player2.getInventory().addItem(inv.getItem(TradeMenu.left[i]));
//            }
//        }
//        for (int i = 0; i < TradeMenu.right.length; i++) {
//            if (inv.getItem(TradeMenu.right[i]) != null) {
//                player1.getInventory().addItem(inv.getItem(TradeMenu.right[i]));
//            }
//        }
//        clearData();
//    }
//
//    public void clearData() {
//        TradingHandler.trades.remove(player1);
//        TradingHandler.trades.remove(player2);
//        TradingHandler.inventoryHandler.remove(inv);
//
//        if (player1.getOpenInventory().getTopInventory().equals(inv)) {
//            player1.closeInventory();
//        }
//        if (player2.getOpenInventory().getTopInventory().equals(inv)) {
//            player2.closeInventory();
//        }
//        player1UsedItems.clear();
//        player2UsedItems.clear();
//        player1Ready = false;
//        player2Ready = false;
//
//        player1 = null;
//        player2 = null;
//        inv.clear();
//    }
//
//    private static int getItemSlot(ItemStack item, Inventory inv) {
//        for (int i = 0; i < inv.getSize(); i++) {
//            if (inv.getItem(i) == item) {
//                return i;
//            }
//        }
//        return 0;
//    }
//}
//
//
