package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
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

    private SaveHelper saveHelper;
    private PlayerDataCache playerDataCache;
    private DebugLoggerModule logger;
    private EconomyHelper economyHelper;

    // EventManager to handle player join and quit events
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        // Initialize logger
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        // Retrieve MongoDB connection string from configuration
        String connectionString = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("mongoURL");
        try {
            // Initialize SaveHelper with MongoDB connection
            saveHelper = new SaveHelper(connectionString, "xarumystic", "playerData", logger);
            playerDataCache = PlayerDataCache.getInstance(saveHelper, logger); // Use Singleton pattern
            logger.log(Level.INFO, "SaveModule initialized", 0);
        } catch (Exception e) {
            logger.error("Failed to initialize SaveModule: " + e.getMessage());
        }
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
            loadPlayerData(player, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData playerData) {
                    logger.log(Level.INFO, "Player data loaded and cached for " + player.getName(), 0);

                    // Check for pending balance
                    if (playerData.getPendingBalance() > 0) {
                        double pendingBalance = playerData.getPendingBalance();
                        playerData.setBalance(playerData.getBalance() + pendingBalance);
                        playerData.setPendingBalance(0.0);
                        player.sendMessage(ChatColor.GREEN + "You have received $" + economyHelper.formatBalance(pendingBalance) + " from your sold auctions.");
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
                        player.sendMessage(ChatColor.GREEN + "You have received items from your expired auctions.");
                    }

                    // Save the updated player data
                    playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            logger.log(Level.INFO, "Updated player data saved for " + player.getName(), 0);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            logger.error("Failed to save updated player data for " + player.getName() + ": " + throwable.getMessage());
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.error("Failed to load player data for " + player.getName() + ": " + throwable.getMessage());
                }
            });
        });

        // Register PlayerQuitEvent to save player data and clear cache
        eventManager.registerEvent(PlayerQuitEvent.class, (event) -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            savePlayerData(player, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    logger.log(Level.INFO, "Player data saved for " + player.getName(), 0);
                    playerDataCache.clearPlayerData(playerUUID);  // Clear player data from cache
                    logger.log(Level.INFO, "Player data cache cleared for " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.error("Failed to save player data for " + player.getName() + ": " + throwable.getMessage());
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

    // -------------------------- Player Data Methods --------------------------

    /**
     * Loads player data into cache on player join.
     *
     * @param player   The player whose data is to be loaded.
     * @param callback Callback for success or failure.
     */
    public void loadPlayerData(Player player, Callback<PlayerData> callback) {
        UUID playerUUID = player.getUniqueId();
        logger.log(Level.INFO, "Loading data for player: " + player.getName(), 0);
        playerDataCache.loadPlayerData(playerUUID, callback);
    }

    /**
     * Saves cached player data to database on player disconnect.
     *
     * @param player   The player whose data is to be saved.
     * @param callback Callback for success or failure.
     */
    public void savePlayerData(Player player, Callback<Void> callback) {
        UUID playerUUID = player.getUniqueId();
        logger.log(Level.INFO, "Saving data for player: " + player.getName(), 0);
        playerDataCache.savePlayerData(playerUUID, callback);
    }

    // -------------------------- Auction Methods --------------------------

    /**
     * Saves an auction to the database.
     *
     * @param auction  The auction to save.
     * @param callback Callback for success or failure.
     */
    public void saveAuction(Auction auction, Callback<Void> callback) {
        saveHelper.saveAuction(auction, callback);
    }

    /**
     * Loads all auctions from the database.
     *
     * @param callback Callback with the list of auctions.
     */
    public void loadAuctions(Callback<List<Auction>> callback) {
        saveHelper.loadAuctions(callback);
    }

    /**
     * Deletes an auction from the database.
     *
     * @param auctionId The UUID of the auction to delete.
     * @param callback  Callback for success or failure.
     */
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        saveHelper.deleteAuction(auctionId, callback);
    }

    // ---------------------- End of Auction Methods -----------------------

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    public SaveHelper getSaveHelper() {
        return saveHelper;
    }
}
