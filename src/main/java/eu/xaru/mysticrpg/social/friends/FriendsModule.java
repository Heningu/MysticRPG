package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;

import java.util.List;
import java.util.logging.Level;

public class FriendsModule implements IBaseModule {

    private FriendsHelper friendsHelper;
    private DebugLoggerModule logger;
    private PlayerDataCache playerDataCache; // Reference to the cache


    @Override
    public void initialize() {
        // Get the logger instance
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
      //  this.playerDataCache = new PlayerDataCache(ModuleManager.getInstance().getModuleInstance(SaveModule.class).saveHelper,ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class));

        // Initialize FriendsHelper with PlayerDataCache and logger
        friendsHelper = new FriendsHelper(playerDataCache, logger);

        // Log the initialization
        logger.log(Level.INFO, "FriendsModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "FriendsModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "FriendsModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "FriendsModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        // Ensure that PlayerDataCache and DebugLoggerModule are loaded first
        return List.of(DebugLoggerModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public FriendsHelper getFriendsHelper() {
        return friendsHelper;
    }
}
