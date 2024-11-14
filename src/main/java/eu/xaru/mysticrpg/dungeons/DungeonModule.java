// File: eu/xaru/mysticrpg/dungeons/DungeonModule.java

package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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

        // Clean up leftover instance worlds
        cleanUpInstanceWorlds();

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

    // Added cleanup logic
    private void cleanUpInstanceWorlds() {
        File worldContainer = Bukkit.getWorldContainer();
        File[] files = worldContainer.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith("dungeon_instance_")) {
                // Unload the world if it's loaded
                World world = Bukkit.getWorld(file.getName());
                if (world != null) {
                    Bukkit.unloadWorld(world, false);
                }
                // Delete the world folder
                deleteWorld(file);
                logger.log(Level.INFO, "Deleted leftover dungeon instance world: " + file.getName(), 0);
            }
        }
    }

    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorld(file);
                }
            }
            path.delete();
        }
    }
}
