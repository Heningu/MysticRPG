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

import org.bukkit.Bukkit;
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
import java.util.logging.Logger;

// import eu.xaru.mysticrpg.guis.auctionhouse.AuctionHouseMainMenu;

/**
 * AuctionHouseModule handles the initialization and management
 * of the auction house.
 */
public class AuctionHouseModule implements IBaseModule {

    private static final String INVENTORY_AUCTION_HOUSE = "Auction House";
    private static final String INVENTORY_BUY = "Auction House - Buy";
    private static final String INVENTORY_SELL = "Auction House - Sell";
    private static final String INVENTORY_YOUR_AUCTIONS = "Your Auctions";

    // private AuctionHouseHelper auctionHouseHelper;
    private EventManager eventManager;
    private EconomyHelper economyHelper;
    private SaveModule saveModule;
    
    // private AuctionHouseMainMenu auctionsGUI;
    private MysticCore plugin;
    private CustomItemModule customItemModule;
    private static final Logger loggerModule = Logger.getLogger(AuctionHouseModule.class.getName());


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
            DebugLogger.getInstance().error("EconomyModule is not initialized. " +
                    "AuctionHouseModule cannot function without it.");
            return;
        }

        this.customItemModule = ModuleManager.getInstance()
                .getModuleInstance(CustomItemModule.class);
        // Instantiate AuctionHouseHelper once
      //  this.auctionHouseHelper = new AuctionHouseHelper(economyHelper);

        // Instantiate AuctionsGUI with necessary dependencies
        //this.auctionsGUI = new AuctionHouseMainMenu();

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
        return List.of(
                
                EconomyModule.class,
                SaveModule.class
        );
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Opens the main Auction House GUI for the player.
     *
     * @param player The player to open the GUI for.
     */
    public void openAuctionGUI(Player player) {
      //  auctionsGUI.openAuctionsGUI(player);
    }

    /**
     * Registers relevant event listeners for the Auction House.
     */
    private void registerEvents() {
        // Register InventoryClickEvent for AuctionsGUI
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
            if (inventoryTitle == null) return;

        });

        // Register InventoryDragEvent for AuctionsGUI
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        });

        // Register InventoryCloseEvent for AuctionsGUI
        eventManager.registerEvent(InventoryCloseEvent.class, event -> {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());

            if (INVENTORY_SELL.equals(inventoryTitle)) {
                handleSellInventoryClose(event, player);
            }
            // Remove the following block to prevent removing PaginationHelpers when Buy GUI is closed
            /*
            else if (INVENTORY_BUY.equals(inventoryTitle)) {
                auctionsGUI.removePagination(player.getUniqueId());
            }
            */
        });

        // Register AsyncPlayerChatEvent for handling bids and custom prices
        eventManager.registerEvent(AsyncPlayerChatEvent.class, event -> {
            // Sanitize the message using Utils (assuming it handles such operations)
            event.setMessage(Utils.getInstance().$(event.getMessage()));
        });
    }

    /**
     * Checks if the given inventory title is related to the Auction House.
     *
     * @param title The stripped inventory title.
     * @return True if it's an Auction House inventory, false otherwise.
     */
    private boolean isAuctionHouseInventory(String title) {
        return INVENTORY_AUCTION_HOUSE.equals(title) ||
                INVENTORY_BUY.equals(title) ||
                INVENTORY_SELL.equals(title) ||
                INVENTORY_YOUR_AUCTIONS.equals(title);
    }


//    public AuctionHouseMainMenu getAuctionsGUI() {
//        return auctionsGUI;
//    }


    /**
     * Handles the InventoryCloseEvent for the Sell GUI.
     *
     * @param event  The InventoryCloseEvent.
     * @param player The player who closed the inventory.
     */
    private void handleSellInventoryClose(InventoryCloseEvent event, Player player) {
        // Delay the check to the next tick to ensure pendingPriceInput is updated
//        Bukkit.getScheduler().runTaskLater(plugin, () -> {
//            if (!auctionsGUI.isPendingPriceInput(player.getUniqueId())) {
//                // Return the item in slot 22 to the player
//                ItemStack item = event.getInventory().getItem(22);
//                if (item != null && item.getType() != Material.AIR) {
//                    player.getInventory().addItem(item);
//                }
//                // Remove the item from the sellingItems map
//                auctionsGUI.removeSellingItem(player.getUniqueId());
//            }
//        }, 1L);
    }

    public void sellItem(Player player, ItemStack itemStack, int price) {
        CustomItem customItem = CustomItemUtils.fromItemStack(itemStack);
        if (customItem == null) {
            // Convert normal ItemStack to CustomItem
            customItem = customItemModule.createCustomItemFromItemStack(itemStack);
        }

        UUID sellerId = player.getUniqueId();
        //auctionHouseHelper.addAuction(sellerId, customItem, price, /* duration */ 86400000L); // Example duration: 1 day

        player.sendMessage(Utils.getInstance().$("Your item has been listed in the Auction House."));
    }

}
