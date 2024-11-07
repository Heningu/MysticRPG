package eu.xaru.mysticrpg.auctionhouse;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * AuctionHouseModule handles the initialization and management
 * of the auction house.
 */
public class AuctionHouseModule implements IBaseModule {

    private AuctionHouseHelper auctionHouseHelper;
    private EventManager eventManager;
    private EconomyHelper economyHelper;
    private SaveModule saveModule;
    private DebugLoggerModule logger;
    private AuctionsGUI auctionsGUI;
    private MysticCore plugin;

    @Override
    public void initialize() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);
        this.logger = ModuleManager.getInstance()
                .getModuleInstance(DebugLoggerModule.class);
        this.saveModule = ModuleManager.getInstance()
                .getModuleInstance(SaveModule.class);

        EconomyModule economyModule = ModuleManager.getInstance()
                .getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            logger.error("EconomyModule is not initialized. " +
                    "AuctionHouseModule cannot function without it.");
            return;
        }

        // Instantiate AuctionHouseHelper once
        this.auctionHouseHelper = new AuctionHouseHelper(economyHelper);

        // Pass the same instance to AuctionsGUI
        this.auctionsGUI = new AuctionsGUI(auctionHouseHelper,
                economyHelper, logger);

        logger.log(Level.INFO, "AuctionHouseModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "AuctionHouseModule started", 0);

        // Register commands and events
        registerCommands();
        registerEvents();
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "AuctionHouseModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "AuctionHouseModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class,
                EconomyModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void registerCommands() {
        new CommandAPICommand("auctionhouse")
                .withAliases("ah")
                .withPermission("mysticrpg.auctionhouse")
                .executesPlayer((player, args) -> {
                    if (auctionHouseHelper.areAuctionsLoaded()) {
                        auctionsGUI.openMainGUI(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "Please wait, " +
                                "auctions are still loading.");
                    }
                })
                .register();
    }

    private void registerEvents() {
        // Register InventoryClickEvent for AuctionsGUI
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle == null) return;

            String strippedTitle = ChatColor.stripColor(inventoryTitle);

            if ("Auction House".equals(strippedTitle) ||
                    "Auction House - Buy".equals(strippedTitle) ||
                    "Auction House - Sell".equals(strippedTitle) ||
                    "Your Auctions".equals(strippedTitle)) {

                auctionsGUI.onInventoryClick(event);
            }
        });

        // Register InventoryDragEvent for AuctionsGUI
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle == null) return;

            String strippedTitle = ChatColor.stripColor(inventoryTitle);

            if ("Auction House".equals(strippedTitle) ||
                    "Auction House - Buy".equals(strippedTitle) ||
                    "Auction House - Sell".equals(strippedTitle) ||
                    "Your Auctions".equals(strippedTitle)) {

                auctionsGUI.onInventoryDrag(event);
            }
        });

        // Register AsyncPlayerChatEvent for handling bids
        eventManager.registerEvent(AsyncPlayerChatEvent.class, event -> {
            auctionsGUI.onPlayerChat(event);
        });
    }
}
