package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SaveModule handles the initialization and management of player data and auctions saving and loading.
 */
public class SaveModule implements IBaseModule {

    private DatabaseManager databaseManager;
    private PlayerDataCache playerDataCache;
    private DebugLoggerModule logger;
    private EconomyHelper economyHelper;

    // EventManager to handle player join and quit events
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    @Override
    public void initialize() {
        // Initialize logger
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        // Initialize DatabaseManager
        DatabaseManager.initialize(logger);
        databaseManager = DatabaseManager.getInstance();

        // Initialize PlayerDataCache
        PlayerDataCache.initialize(databaseManager, logger);
        playerDataCache = PlayerDataCache.getInstance();

        logger.log(Level.INFO, "SaveModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "SaveModule started", 0);

        // Get EconomyHelper instance in the start() method
        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            logger.error("EconomyModule is not initialized. SaveModule cannot function without it.");
            return; // Exit start() method if economyHelper is not available
        }

        // Register PlayerJoinEvent to load player data and handle pending transactions
        eventManager.registerEvent(PlayerJoinEvent.class, (event) -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            logger.log(Level.INFO, "SaveModule: Loading data for player: " + player.getName(), 0);

            playerDataCache.loadPlayerData(playerUUID, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData playerData) {
                    logger.log(Level.INFO, "SaveModule$1: Player data loaded and cached for " + player.getName(), 0);

                    // Check for pending balance
                    if (playerData.getPendingBalance() > 0) {
                        double pendingBalance = playerData.getPendingBalance();
                        playerData.setBalance(playerData.getBalance() + pendingBalance);
                        playerData.setPendingBalance(0.0);
                        player.sendMessage(Utils.getInstance().$("You have received $" + economyHelper.formatBalance(pendingBalance) + " from your sold auctions."));
                        logger.log(Level.INFO, "Applied pending balance for " + player.getName(), 0);
                    }

                    // Check for pending items
                    if (!playerData.getPendingItems().isEmpty()) {
                        for (String serializedItem : playerData.getPendingItems()) {
                            ItemStack item = SaveHelper.itemStackFromBase64(serializedItem);
                            if (item != null) {
                                player.getInventory().addItem(item);
                            }
                        }
                        playerData.getPendingItems().clear();
                        player.sendMessage(Utils.getInstance().$("You have received items from your expired auctions."));
                        logger.log(Level.INFO, "Applied pending items for " + player.getName(), 0);
                    }

                    // Save the updated player data
                    playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            player.sendMessage(Utils.getInstance().$("Your data has been saved to the database."));
                            logger.log(Level.INFO, "SaveModule$1: Data saved successfully for player: " + player.getName(), 0);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            player.sendMessage(Utils.getInstance().$("Failed to save your data. Please try again later."));
                            logger.error("SaveModule$1: Failed to save data for player: " + player.getName() + ". " + throwable.getMessage());
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    player.sendMessage(Utils.getInstance().$("Failed to load your data. Please try again later."));
                    logger.error("SaveModule$1: Failed to load data for player: " + player.getName() + ". " + throwable.getMessage());
                }
            });
        });

        // Register PlayerQuitEvent to save player data and clear cache
        eventManager.registerEvent(PlayerQuitEvent.class, (event) -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            logger.log(Level.INFO, "SaveModule: Saving data for player: " + player.getName(), 0);
            playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    logger.log(Level.INFO, "SaveModule$1: Player data saved for " + player.getName(), 0);
                    playerDataCache.clearPlayerData(playerUUID);  // Clear player data from cache
                    logger.log(Level.INFO, "SaveModule$1: Player data cache cleared for " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.error("SaveModule$1: Failed to save player data for " + player.getName() + ": " + throwable.getMessage());
                }
            });
        });
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "SaveModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "SaveModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, EconomyModule.class);  // Depend on DebugLoggerModule and EconomyModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.CRITICAL;  // Ensure it loads early
    }

    // -------------------------- Auction Methods --------------------------

    /**
     * Saves an auction to the database.
     *
     * @param auction  The auction to save.
     * @param callback Callback for success or failure.
     */
    public void saveAuction(Auction auction, Callback<Void> callback) {
        databaseManager.saveAuction(auction, callback);
    }

    /**
     * Loads all auctions from the database.
     *
     * @param callback Callback with the list of auctions.
     */
    public void loadAuctions(Callback<List<Auction>> callback) {
        databaseManager.loadAuctions(callback);
    }

    /**
     * Deletes an auction from the database.
     *
     * @param auctionId The UUID of the auction to delete.
     * @param callback  Callback for success or failure.
     */
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        databaseManager.deleteAuction(auctionId, callback);
    }

    // ---------------------- End of Auction Methods -----------------------
}
