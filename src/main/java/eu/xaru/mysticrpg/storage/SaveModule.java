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
import eu.xaru.mysticrpg.storage.database.SaveHelper;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
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

    private EconomyHelper economyHelper;

    // EventManager to handle player join and quit events
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        // Initialize DatabaseManager
        DatabaseManager.initialize();
        databaseManager = DatabaseManager.getInstance();

        // Initialize PlayerDataCache
        PlayerDataCache.initialize(databaseManager);
        playerDataCache = PlayerDataCache.getInstance();

        DebugLogger.getInstance().log(Level.INFO, "SaveModule initialized", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "SaveModule started", 0);

        // Get EconomyHelper instance in the start() method
        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            DebugLogger.getInstance().error("EconomyModule is not initialized. SaveModule cannot function without it.");
            return; // Exit start() method if economyHelper is not available
        }

        // Register PlayerJoinEvent to load player data and handle pending transactions
        eventManager.registerEvent(PlayerJoinEvent.class, (event) -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            DebugLogger.getInstance().log(Level.INFO, "SaveModule: Loading data for player: " + player.getName(), 0);

            playerDataCache.loadPlayerData(playerUUID, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData playerData) {
                    DebugLogger.getInstance().log(Level.INFO, "SaveModule: Player data loaded and cached for " + player.getName(), 0);

                    // Check for pending balance
                    if (playerData.getPendingBalance() > 0) {
                        int pendingBalance = playerData.getPendingBalance();
                        playerData.setBankGold(playerData.getBankGold() + pendingBalance);
                        playerData.setPendingBalance(0);
                        player.sendMessage(Utils.getInstance().$("You have received $" + pendingBalance + " from your sold auctions."));
                        DebugLogger.getInstance().log(Level.INFO, "Applied pending balance for " + player.getName(), 0);
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
                        DebugLogger.getInstance().log(Level.INFO, "Applied pending items for " + player.getName(), 0);
                    }

                    // Save the updated player data
                    playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            player.sendMessage(Utils.getInstance().$("Your data has been saved to the database."));
                            DebugLogger.getInstance().log(Level.INFO, "SaveModule: Data saved successfully for player: " + player.getName(), 0);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            player.sendMessage(Utils.getInstance().$("Failed to save your data. Please try again later."));
                            DebugLogger.getInstance().error("SaveModule: Failed to save data for player: " + player.getName() + ". ", throwable);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    player.sendMessage(Utils.getInstance().$("Failed to load your data. Please try again later."));
                    DebugLogger.getInstance().error("SaveModule: Failed to load data for player: " + player.getName() + ". ", throwable);
                }
            });
        });

        // Register PlayerQuitEvent to save player data and clear cache
        eventManager.registerEvent(PlayerQuitEvent.class, (event) -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            DebugLogger.getInstance().log(Level.INFO, "SaveModule: Saving data for player: " + player.getName(), 0);
            playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "SaveModule: Player data saved for " + player.getName(), 0);
                    playerDataCache.clearPlayerData(playerUUID);  // Clear player data from cache
                    DebugLogger.getInstance().log(Level.INFO, "SaveModule: Player data cache cleared for " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().error("SaveModule: Failed to save player data for " + player.getName() + ": ", throwable);
                }
            });
        });
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "SaveModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "SaveModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(); // Add dependencies if any
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.CRITICAL;  // Ensure it loads early
    }

    // Auction Methods

    /**
     * Saves an auction to the database.
     *
     * @param auction  The auction to save.
     * @param callback Callback for success or failure.
     */
    public void saveAuction(Auction auction, Callback<Void> callback) {
        databaseManager.getAuctionRepository().save(auction, callback);
    }

    /**
     * Loads all auctions from the database.
     *
     * @param callback Callback with the list of auctions.
     */
    public void loadAuctions(Callback<List<Auction>> callback) {
        databaseManager.getAuctionRepository().loadAll(callback);
    }

    /**
     * Deletes an auction from the database.
     *
     * @param auctionId The UUID of the auction to delete.
     * @param callback  Callback for success or failure.
     */
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        databaseManager.getAuctionRepository().delete(auctionId, callback);
    }
}
