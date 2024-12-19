package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.ui.UIModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class DungeonModule implements IBaseModule {

    private JavaPlugin plugin;
    private DungeonManager dungeonManager;

    @Override
    public void initialize() {
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        // Clean leftover instance worlds
        cleanUpInstanceWorlds();

        dungeonManager = new DungeonManager(plugin, this);

        DebugLogger.getInstance().log(Level.INFO, "DungeonModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        dungeonManager.start();
        DebugLogger.getInstance().log(Level.INFO, "DungeonModule started", 0);
    }

    @Override
    public void stop() {
        dungeonManager.stop();
        DebugLogger.getInstance().log(Level.INFO, "DungeonModule stopped", 0);
    }

    @Override
    public void unload() {
        dungeonManager.stop();
        DebugLogger.getInstance().log(Level.INFO, "DungeonModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
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

    private void cleanUpInstanceWorlds() {
        File worldContainer = Bukkit.getWorldContainer();
        File[] files = worldContainer.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith("dungeon_instance_")) {
                World world = Bukkit.getWorld(file.getName());
                if (world != null) {
                    Bukkit.unloadWorld(world, false);
                }
                deleteWorld(file);
                DebugLogger.getInstance().log(Level.INFO, "Deleted leftover dungeon instance world: " + file.getName(), 0);
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
