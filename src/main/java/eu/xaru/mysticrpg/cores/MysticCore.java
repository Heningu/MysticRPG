package eu.xaru.mysticrpg.cores;

import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

/**
 * Main plugin class for MysticCore, now using native Bukkit config loading.
 */
public class MysticCore extends JavaPlugin {

    private static MysticCore instance;
    private ModuleManager moduleManager;

    public MysticCore() {
        if (instance == null) {
            instance = this;
        }
    }

    public static MysticCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        // Initialize PacketEvents
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        // Initialize CommandAPI in onLoad
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Module manager
        moduleManager = ModuleManager.getInstance();

        // DecentHolograms load
        DecentHologramsAPI.onLoad(this);
    }

    @Override
    public void onEnable() {
        // Ensure InvUI knows our plugin
        InvUI.getInstance().setPlugin(this);

        // Load the default config if missing
        saveDefaultConfig();

        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        try {
            // Module manager loads all modules
            moduleManager.loadAllModules();
            // CommandAPI enable
            CommandAPI.onEnable();
            // DecentHolograms enable
            DecentHologramsAPI.onEnable();

            DebugLogger.getInstance().log("Core plugin enabled successfully.", 0);

        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during plugin enable. Exception:", e);
            getServer().getPluginManager().disablePlugin(this);
        }

        // Optionally reload config to ensure merges, copyDefaults, etc.
        reloadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        CommandAPI.onDisable();
        DecentHologramsAPI.onDisable();

        try {
            moduleManager.shutdown();
            DebugLogger.getInstance().log("Core plugin disabled.", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during plugin disable. Exception:", e);
        }

        // Terminate PacketEvents
        PacketEvents.getAPI().terminate();
    }

    /**
     * If you want to override getConfig(), just return super.getConfig().
     */
    @Override
    public FileConfiguration getConfig() {
        // Return the standard Bukkit config
        return super.getConfig();
    }

    /**
     * Reloads the plugin's config.yml from disk.
     * (You can call this whenever you need to refresh config values.)
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
}
