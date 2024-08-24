package eu.xaru.mysticrpg.cores;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.xaru.mysticrpg.config.ConfigCreator;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    private DebugLoggerModule logger;

    @Override
    public void onEnable() {
        try {
            moduleManager.loadAllModules();
            CommandAPI.onEnable();

            if (logger != null) {
                logger.log("Core plugin enabled successfully.", 0);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error during plugin enable. Exception: " + e.getMessage(), e, null);
            }
            getServer().getPluginManager().disablePlugin(this);
        }

        // Create config files and directories
        new ConfigCreator(this).createFiles();
    }

    @Override
    public void onLoad() {

        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Initialize the module manager and logger
        moduleManager = ModuleManager.getInstance();
        logger = moduleManager.getModuleInstance(DebugLoggerModule.class);

        if (logger != null) {
            logger.log("Core plugin loading...", 0);
        }
    }

    @Override
    public void onDisable() {
        // Shutdown the plugin, unload modules, and clean up resources

        CommandAPI.onDisable();

        try {
            moduleManager.shutdown();
            if (logger != null) {
                logger.log("Core plugin disabled.", 0);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error during plugin disable. Exception: " + e.getMessage(), e, null);
            }
        }
        }
    }
