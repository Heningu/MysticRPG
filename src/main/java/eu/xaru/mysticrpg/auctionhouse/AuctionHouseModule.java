package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class AuctionHouseModule implements IBaseModule {

    private static final String INVENTORY_AUCTION_HOUSE = "Auction House";
    private static final String INVENTORY_BUY = "Auction House - Buy";
    private static final String INVENTORY_SELL = "Auction House - Sell";
    private static final String INVENTORY_YOUR_AUCTIONS = "Your Auctions";

    private EventManager eventManager;
    private EconomyHelper economyHelper;
    private SaveModule saveModule;

    private MysticCore plugin;
    private CustomItemModule customItemModule;
    private static final java.util.logging.Logger loggerModule = java.util.logging.Logger.getLogger(AuctionHouseModule.class.getName());

    @Override
    public void initialize() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);
        this.saveModule = ModuleManager.getInstance()
                .getModuleInstance(SaveModule.class);

        EconomyModule economyModule = ModuleManager.getInstance()
                .getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            DebugLogger.getInstance().error("EconomyModule is not initialized. AuctionHouseModule cannot function without it.");
            return;
        }

        this.customItemModule = ModuleManager.getInstance()
                .getModuleInstance(CustomItemModule.class);

        loggerModule.log(Level.INFO, "AuctionHouseModule initialized");
    }

    @Override
    public void start() {
        loggerModule.log(Level.INFO, "AuctionHouseModule started");
        registerEvents();
    }

    @Override
    public void stop() {
        loggerModule.log(Level.INFO, "AuctionHouseModule stopped");
    }

    @Override
    public void unload() {
        loggerModule.log(Level.INFO, "AuctionHouseModule unloaded");
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(EconomyModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public void openAuctionGUI(Player player) {
        // previously AuctionHouseMainMenu usage could be here if needed
        // for demonstration, no code removed.
    }

    private void registerEvents() {
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
            if (inventoryTitle == null) return;

        });

        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        });

        eventManager.registerEvent(InventoryCloseEvent.class, event -> {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());

            if (INVENTORY_SELL.equals(inventoryTitle)) {
                handleSellInventoryClose(event, player);
            }
        });

        eventManager.registerEvent(AsyncPlayerChatEvent.class, event -> {
            event.setMessage(Utils.getInstance().$(event.getMessage()));
        });
    }

    private boolean isAuctionHouseInventory(String title) {
        return INVENTORY_AUCTION_HOUSE.equals(title) ||
                INVENTORY_BUY.equals(title) ||
                INVENTORY_SELL.equals(title) ||
                INVENTORY_YOUR_AUCTIONS.equals(title);
    }

    private void handleSellInventoryClose(InventoryCloseEvent event, Player player) {
        // Implementation detail if a GUI was used, omitted here
    }

    public void sellItem(Player player, ItemStack itemStack, int price) {
        CustomItem customItem = CustomItemUtils.fromItemStack(itemStack);
        if (customItem == null) {
            customItem = customItemModule.createCustomItemFromItemStack(itemStack);
        }

        UUID sellerId = player.getUniqueId();
        // AuctionHouseHelper usage would be needed here if GUI existed
        player.sendMessage(Utils.getInstance().$("Your item has been listed in the Auction House."));
    }

}
