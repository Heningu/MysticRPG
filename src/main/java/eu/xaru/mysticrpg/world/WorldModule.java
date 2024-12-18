package eu.xaru.mysticrpg.world;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class WorldModule implements IBaseModule {

    private JavaPlugin plugin;
    private WorldManager worldManager;
    private RegionManager regionManager;
    private EventManager eventManager;

    @Override
    public void initialize() {
        plugin = JavaPlugin.getPlugin(MysticCore.class);
        eventManager = new EventManager(plugin);
        worldManager = new WorldManager(plugin, eventManager);
        regionManager = new RegionManager(plugin, eventManager, worldManager);
        DebugLogger.getInstance().log(Level.INFO, "WorldModule initialized", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "WorldModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "WorldModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "WorldModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        // Add AdminModule as a dependency so that adminModule is available when WorldManager tries to get it
        return List.of(eu.xaru.mysticrpg.admin.AdminModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }
}
