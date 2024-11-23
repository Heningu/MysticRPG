package eu.xaru.mysticrpg.player.stats;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class PlayerManagerModule implements IBaseModule {

    private DebugLoggerModule logger;
    private PlayerDataCache playerDataCache;
    private PlayerStatManager playerStatManager;

    @Override
    public void initialize() {
        // Initialize dependencies
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);

        if (logger == null || saveModule == null) {
            Bukkit.getLogger().severe("Dependencies not initialized. PlayerManagerModule cannot function without them.");
            return;
        }

        playerDataCache = saveModule.getPlayerDataCache();
        playerStatManager = new PlayerStatManager(playerDataCache, logger);

        logger.log(Level.INFO, "PlayerManagerModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "PlayerManagerModule started.", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "PlayerManagerModule stopped.", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "PlayerManagerModule unloaded.", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public PlayerStatManager getPlayerStatManager() {
        return playerStatManager;
    }
}
