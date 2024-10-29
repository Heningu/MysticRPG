package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SaveModule implements IBaseModule {

    public SaveHelper saveHelper;
    private PlayerDataCache playerDataCache;
    private DebugLoggerModule logger;

    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        String connectionString = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("mongoURL");
        try {
            saveHelper = new SaveHelper(connectionString, "xarumystic", "playerData", logger);
            playerDataCache = PlayerDataCache.getInstance(saveHelper, logger); // Use Singleton
            logger.log(Level.INFO, "SaveModule initialized", 0);
        } catch (Exception e) {
            logger.error("Failed to initialize SaveModule: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "SaveModule started", 0);

        // Register PlayerJoinEvent to load player data
        eventManager.registerEvent(PlayerJoinEvent.class, (event) -> {
            Player player = event.getPlayer();
            loadPlayerData(player, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData result) {
                    logger.log(Level.INFO, "Player data loaded and cached for " + player.getName(), 0);
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
        return List.of(DebugLoggerModule.class);  // Depend on DebugLoggerModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.CRITICAL;  // Ensure it loads early
    }

    // Load player data into cache on player join
    public void loadPlayerData(Player player, Callback<PlayerData> callback) {
        UUID playerUUID = player.getUniqueId();
        logger.log(Level.INFO, "Loading data for player: " + player.getName(), 0);
        playerDataCache.loadPlayerData(playerUUID, callback);
    }

    // Save cached player data to database on player disconnect
    public void savePlayerData(Player player, Callback<Void> callback) {
        UUID playerUUID = player.getUniqueId();
        logger.log(Level.INFO, "Saving data for player: " + player.getName(), 0);
        playerDataCache.savePlayerData(playerUUID, callback);
    }

    // Friends-related methods (cached)
    public void addFriend(UUID playerUUID, UUID friendUUID) {
        playerDataCache.addFriend(playerUUID, friendUUID);
    }

    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        playerDataCache.removeFriend(playerUUID, friendUUID);
    }

    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        playerDataCache.addFriendRequest(playerUUID, requesterUUID);
    }

    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        playerDataCache.removeFriendRequest(playerUUID, requesterUUID);
    }

    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        playerDataCache.blockPlayer(blockerUUID, toBlockUUID);
    }

    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        playerDataCache.unblockPlayer(blockerUUID, toUnblockUUID);
    }

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }
    public SaveHelper getSaveHelper() {
        return saveHelper;
    }
}