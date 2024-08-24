package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SaveModule implements IBaseModule {

    public SaveHelper saveHelper;
    private PlayerDataCache playerDataCache;
    private DebugLoggerModule logger;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        String connectionString = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("mongoURL");
        try {
            saveHelper = new SaveHelper(connectionString, "xarumystic", "playerData", logger);
            playerDataCache = new PlayerDataCache(saveHelper, logger);
            logger.log(Level.INFO, "SaveModule initialized", 0);
        } catch (Exception e) {
            logger.error("Failed to initialize SaveModule: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "SaveModule started", 0);
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
        return EModulePriority.HIGH;  // Ensure it loads early
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

    // -------------------------------------------------------
    // Friends-related methods (cached)
    // -------------------------------------------------------

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
}
