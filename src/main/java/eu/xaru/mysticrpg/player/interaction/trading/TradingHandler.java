package eu.xaru.mysticrpg.player.interaction.trading;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

public class TradingHandler implements IBaseModule {


    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));
    private DebugLoggerModule debugLogger;
    public static Map<Inventory, Trade> inventoryHandler = new WeakHashMap<>();
    protected static Map<Player, Player> trades = new WeakHashMap<>();

    @Override
    public void initialize() throws Exception {
        debugLogger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        debugLogger.log(Level.INFO, "TradeModule initialization", 0);

        registerTradeCommand();
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (inventoryHandler.containsKey(event.getInventory())) {

                event.setCancelled(true);

                Inventory inv = event.getInventory();
                Trade trade = inventoryHandler.get(inv);

                if (event.getClickedInventory() != null) {

                    if (event.getClickedInventory().equals(inv)) {
                        if (event.getWhoClicked() == trade.player1) {

                            if (TradeMenu.isAllowedSlot(event.getSlot(), TradeMenu.leftSet)) {
                                inv.setItem(event.getSlot(), null);
                                trade.player1UsedItems.remove(event.getSlot());
                            } else if (event.getSlot() == 45) {
                                if (!trade.player1Ready) {
                                    trade.player1Ready = true;
                                    inv.setItem(45, CustomInventoryManager.createPlaceholder(Material.GREEN_WOOL, "§aREADY"));
                                    trade.checkReady();
                                }
                            }

                        } else if (event.getWhoClicked() == trade.player2) {

                            if (TradeMenu.isAllowedSlot(event.getSlot(), TradeMenu.rightSet)) {
                                inv.setItem(event.getSlot(), null);
                                trade.player2UsedItems.remove(event.getSlot());
                            } else if (event.getSlot() == 53) {
                                if (!trade.player2Ready) {
                                    trade.player2Ready = true;
                                    inv.setItem(53, CustomInventoryManager.createPlaceholder(Material.GREEN_WOOL, "§aREADY"));
                                    trade.checkReady();
                                }
                            }
                        }


                    } else if (event.getClickedInventory().equals(trade.player1.getInventory())) {
                        ItemStack clickedItem = event.getClickedInventory().getItem(event.getSlot());

                        if (clickedItem != null) {
                            if (!trade.player1UsedItems.containsValue(event.getSlot())) {
                                int slot = TradeMenu.getNextFreeSlot("left", inv);
                                if (slot != -1) {
                                    inv.setItem(slot, clickedItem);
                                    trade.modifySlotReadyCheck(inv);
                                    trade.player1UsedItems.put(slot, event.getSlot());
                                }
                            }
                        }


                    } else if (event.getClickedInventory().equals(trade.player2.getInventory())) {
                        ItemStack clickedItem = event.getClickedInventory().getItem(event.getSlot());

                        if (clickedItem != null) {
                            if (!trade.player2UsedItems.containsKey(event.getSlot())) {
                                int slot = TradeMenu.getNextFreeSlot("right", inv);
                                if (slot != -1) {
                                    inv.setItem(slot, clickedItem);
                                    trade.modifySlotReadyCheck(inv);
                                    trade.player2UsedItems.put(slot, event.getSlot());
                                }
                            }
                        }
                    }
                }
            }
        });

        eventManager.registerEvent(InventoryCloseEvent.class, event -> {

            if(inventoryHandler.containsKey(event.getInventory())){
                Trade trade = TradingHandler.inventoryHandler.get(event.getInventory());
                if(!trade.cancelled){
                    if(!trade.tradeCompleted) {
                        trade.cancelled = true;
                        trade.player1.sendMessage("§cTrade cancelled");
                        trade.player2.sendMessage("§cTrade cancelled");
                        trade.player1.closeInventory();
                        trade.player2.closeInventory();

                        trade.clearData();
                    }
                }
            }
        });
        debugLogger.log(Level.INFO, "TradeModule init finished", 0);
    }

    private void registerTradeCommand() {
        new CommandAPICommand("trade")
                .withPermission("mysticrpg.trade")
                .withArguments(new PlayerArgument("target"))
                .executes((player, args) -> {
                    if(player instanceof Player) {
                        Player p = (Player) player;
                        Player target = (Player) args.get("target");
                        if(p != target) {
                            if (target != null) {
                                Trade.sendTradeInvite(p, target);
                            }
                        }
                    }
        }).register();
    }

    @Override
    public void start() throws Exception {
        debugLogger.log(Level.INFO, "TradeModule started", 0);
    }

    @Override
    public void stop() throws Exception {
        debugLogger.log(Level.INFO, "TradeModule stopped", 0);
    }

    @Override
    public void unload() throws Exception {
        debugLogger.log(Level.INFO, "TradeModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }
}
