package eu.xaru.mysticrpg.cores;

import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.xaru.mysticrpg.config.ConfigCreator;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

// Import necessary NPC-Lib classes
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    private DebugLoggerModule logger;

    // Initialize the NPC-Lib platform
    private final Platform<World, Player, ItemStack, Plugin> npcPlatform = BukkitPlatform
            .bukkitNpcPlatformBuilder()
            .extension(this)
            .actionController(builder -> {}) // Enable action controller without changing the default config
            .build();

    // Getter for the NPC platform
    public Platform<World, Player, ItemStack, Plugin> getNpcPlatform() {
        return npcPlatform;
    }

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
