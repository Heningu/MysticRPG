package eu.xaru.mysticrpg.player.interaction.trading;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.player.interaction.PlayerInteractionMenu;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

public class TradingHandler implements IBaseModule {


    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    public static HashMap<Inventory, Trade> inventoryHandler = new HashMap<>();
    protected static HashMap<Player, Player> trades = new HashMap<Player, Player>();

    @Override
    public void initialize() throws Exception {
        registerTradeCommand();
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if(inventoryHandler.containsKey(event.getInventory())){
                event.setCancelled(true);
                Inventory inv = event.getInventory();
                Trade trade = inventoryHandler.get(inv);
                if(inventoryHandler.containsKey(event.getClickedInventory())){
                    if(event.getWhoClicked() == trade.player1) {
                        if(TradeMenu.isAllowedSlot(event.getSlot(), TradeMenu.leftSet)){
                            inv.setItem(event.getSlot(), null);
                        }else if(event.getSlot() == 45){
                            trade.player1Ready = true;
                            inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.GREEN_WOOL, "§aREADY"));
                            trade.checkReady();
                        }
                    }else if(event.getWhoClicked() == trade.player2){
                        if(TradeMenu.isAllowedSlot(event.getSlot(), TradeMenu.rightSet)){
                            inv.setItem(event.getSlot(), null);
                        }else if(event.getSlot() == 53){
                            trade.player2Ready = true;
                            inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.GREEN_WOOL, "§aREADY"));
                            trade.checkReady();
                        }
                    }
                }else if(event.getClickedInventory() == trade.player1.getInventory()){
                    if(event.getClickedInventory() != null) {
                        if (event.getClickedInventory().getItem(event.getSlot()) != null) {
                            int slot = TradeMenu.getNextFreeSlot("left", inv);
                            if (slot != 0) {
                                inv.setItem(slot, trade.player1.getInventory().getItem(event.getSlot()));
                                trade.modifySlotReadyCheck(inv);
                            }
                        }
                    }
                }else if(event.getClickedInventory() == trade.player2.getInventory()){
                    if(event.getClickedInventory() != null) {
                        if (event.getClickedInventory().getItem(event.getSlot()) != null) {
                            int slot = TradeMenu.getNextFreeSlot("right", inv);
                            if (slot != 0) {
                                inv.setItem(slot, trade.player2.getInventory().getItem(event.getSlot()));
                                trade.modifySlotReadyCheck(inv);
                            }
                        }
                    }
                }
            }
        });
    }

    private void registerTradeCommand() {
        new CommandAPICommand("trade")
                .withPermission("mysticrpg.trade")
                .withArguments(new PlayerArgument("target"))
                .executes((player, args) -> {
                    if(player instanceof Player) {
                        Player p = (Player) player;
                        Player target = (Player) args.get("target");
                        if (target != null) {
                            Trade.sendTradeInvite(p, target);
                        }
                    }
        }).register();
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void unload() throws Exception {

    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return null;
    }
}
