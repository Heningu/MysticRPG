package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.benchmark.DbBenchmarkCommand;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.storage.database.SaveHelper;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SaveModule handles initialization and management of player data (load/save) and auctions.
 */
public class SaveModule implements IBaseModule {

    private DatabaseManager databaseManager;
    private PlayerDataCache playerDataCache;
    private EconomyHelper economyHelper;

    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        // Initialize DB
        DatabaseManager.initialize();
        databaseManager = DatabaseManager.getInstance();

        // Initialize PlayerDataCache
        PlayerDataCache.initialize(databaseManager);
        playerDataCache = PlayerDataCache.getInstance();

        DebugLogger.getInstance().log(Level.INFO, "SaveModule initialized", 0);
    }

    @Override
    public void start() {

        // Register /dbbench command for DB benchmarking
        DbBenchmarkCommand.register();

        // Acquire EconomyHelper
        EconomyModule econModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (econModule != null) {
            this.economyHelper = econModule.getEconomyHelper();
        } else {
            DebugLogger.getInstance().error("EconomyModule not initialized. SaveModule can't function without it.");
            return;
        }

        // Register PlayerJoinEvent to load data
        eventManager.registerEvent(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            DebugLogger.getInstance().log(Level.INFO, "Load data for: " + player.getName(), 0);
            playerDataCache.loadPlayerData(uuid, new Callback<>() {
                @Override
                public void onSuccess(PlayerData data) {
                    DebugLogger.getInstance().log(Level.INFO, "Data loaded for " + player.getName(), 0);

                    // Check pending balance/items
                    if (data.getPendingBalance() > 0) {
                        int pending = data.getPendingBalance();
                        data.setBankGold(data.getBankGold() + pending);
                        data.setPendingBalance(0);
                        player.sendMessage(Utils.getInstance().$("You received $" + pending + " from sold auctions."));
                    }
                    if (!data.getPendingItems().isEmpty()) {
                        for (String base64 : data.getPendingItems()) {
                            ItemStack item = SaveHelper.itemStackFromBase64(base64);
                            if (item != null) {
                                player.getInventory().addItem(item);
                            }
                        }
                        data.getPendingItems().clear();
                        player.sendMessage(Utils.getInstance().$("You received items from expired auctions."));
                    }
                    // Mark dirty so it eventually flushes
                    playerDataCache.markDirty(uuid);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    player.sendMessage(Utils.getInstance().$("Failed to load your data."));
                    DebugLogger.getInstance().error("Failed to load data for " + player.getName(), throwable);
                }
            });
        });

        // Register PlayerQuitEvent
        eventManager.registerEvent(PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            DebugLogger.getInstance().log(Level.INFO, "Saving data for: " + player.getName(), 0);
            playerDataCache.savePlayerData(uuid, new Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "Data saved for " + player.getName(), 0);
                    playerDataCache.clearPlayerData(uuid);
                    DebugLogger.getInstance().log(Level.INFO, "Cache cleared for " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().error("Failed saving data for " + player.getName(), throwable);
                }
            });
        });
    }



    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.CRITICAL;
    }

    // Auction methods
    public void saveAuction(Auction auction, Callback<Void> callback) {
        databaseManager.getAuctionRepository().save(auction, callback);
    }

    public void loadAuctions(Callback<List<Auction>> callback) {
        databaseManager.getAuctionRepository().loadAll(callback);
    }

    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        databaseManager.getAuctionRepository().delete(auctionId, callback);
    }
}
