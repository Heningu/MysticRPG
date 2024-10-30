package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class UIModule implements IBaseModule {

    private ActionBarManager actionBarManager;
    private ScoreboardManager scoreboardManager;
    private DebugLoggerModule logger;
    private final JavaPlugin plugin;

    public UIModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Assuming MysticCore is the main class
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);

        if (saveModule != null) {
            PlayerDataCache playerDataCache = saveModule.getPlayerDataCache();
            this.actionBarManager = new ActionBarManager((MysticCore) plugin, playerDataCache);
            this.scoreboardManager = new ScoreboardManager();
            logger.log(Level.INFO, "UIModule initialized successfully.", 0);
        } else {
            logger.error("SaveModule is not initialized. UIModule cannot function without it.");
        }
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "UIModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "UIModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "UIModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, DebugLoggerModule.class);  // Depend on SaveModule and DebugLoggerModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }
}
