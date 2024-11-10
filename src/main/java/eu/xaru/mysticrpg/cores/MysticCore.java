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
    public static MysticCore instance = null;

    public MysticCore() {
        if(instance == null) {
            instance = this;
        }

    }

    public static MysticCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Initialize the module manager
        moduleManager = ModuleManager.getInstance();
    }

    @Override
    public void onEnable() {
        try {
            // Load all modules (eagerly loaded modules)
            moduleManager.loadAllModules();
            CommandAPI.onEnable();

            // Retrieve DebugLoggerModule instance after loading modules
            logger = moduleManager.getModuleInstance(DebugLoggerModule.class);

            if (logger != null) {
                logger.log("Core plugin enabled successfully.", 0);
            } else {
                getLogger().severe("DebugLoggerModule is not loaded. Plugin may not function correctly.");
            }

        } catch (Exception e) {
            // Attempt to log using JavaPlugin's logger if DebugLoggerModule isn't available
            if (logger != null) {
                logger.error("Error during plugin enable. Exception: " + e.getMessage(), e, null);
            } else {
                getLogger().severe("Error during plugin enable. Exception: " + e.getMessage());
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }

        // Create config files and directories
        new ConfigCreator(this).createFiles();
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
            } else {
                getLogger().severe("Error during plugin disable. Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
