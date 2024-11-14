// File: eu/xaru/mysticrpg/dungeons/DungeonModule.java

package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class DungeonModule implements IBaseModule {

    private DebugLoggerModule logger;
    private JavaPlugin plugin;
    private DungeonManager dungeonManager;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            throw new IllegalStateException("DebugLoggerModule not initialized. DungeonModule cannot function without it.");
        }

        plugin = JavaPlugin.getPlugin(MysticCore.class);

        dungeonManager = new DungeonManager(plugin, logger, this);

        logger.log(Level.INFO, "DungeonModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        dungeonManager.start();
        logger.log(Level.INFO, "DungeonModule started", 0);
    }

    @Override
    public void stop() {
        dungeonManager.stop();
        logger.log(Level.INFO, "DungeonModule stopped", 0);
    }

    @Override
    public void unload() {
        dungeonManager.stop();
        logger.log(Level.INFO, "DungeonModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
