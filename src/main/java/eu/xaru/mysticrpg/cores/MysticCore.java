package eu.xaru.mysticrpg.cores;

import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

/**
 * Main plugin class for MysticCore.
 */
public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    private static MysticCore instance;

    private DynamicConfig con;
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
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Initialize the module manager
        moduleManager = ModuleManager.getInstance();
        DecentHologramsAPI.onLoad(this);
    }

    @Override
    public void onEnable() {
        DynamicConfigManager.init(this);

        // Initialize dynamic config system
        InvUI.getInstance().setPlugin(this);


        con = DynamicConfigManager.loadConfig("config.yml");
        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        try {
            moduleManager.loadAllModules();
            CommandAPI.onEnable();
            DecentHologramsAPI.onEnable();

            DebugLogger.getInstance().log("Core plugin enabled successfully.", 0);

        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during plugin enable. Exception:", e);
            getServer().getPluginManager().disablePlugin(this);
        }
        reloadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        CommandAPI.onDisable();
        DecentHologramsAPI.onDisable();

        DynamicConfigManager.saveAll();
        try {
            moduleManager.shutdown();
            DebugLogger.getInstance().log("Core plugin disabled.", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during plugin disable. Exception:", e);
        }

        PacketEvents.getAPI().terminate();
        // Save any dynamic config changes
    }

    /**
     * We typically can't override getConfig() to return a different type
     * (it must return FileConfiguration). So we either override and throw
     * or do a bridging approach.
     *
     * If you want to strictly override getConfig(), you have to
     * return a FileConfiguration. So let's do a bridging approach
     * that returns a "dummy" YamlConfiguration, or we can simply
     * override it to throw an exception.
     */

    @Override
    public FileConfiguration getConfig() {
        // We can return a dummy if you want, or throw:
        throw new UnsupportedOperationException("Use getMysticConfig() instead of getConfig() in MysticCore!");
    }

    /**
     * Our custom method to get the dynamic config for "config.yml".
     * This returns a DynamicConfig object so you can do getString, getInt, etc.
     */
    public DynamicConfig getMysticConfig() {
        return DynamicConfigManager.loadConfig("config.yml");
    }

    /**
     * Overloaded function if you want to load a different resource & user path.
     * e.g. getMysticConfig("someResource.yml", "someFile.yml").
     */
    public DynamicConfig getMysticConfig(String resourceName, String userFileName) {
        // If it's not loaded yet, let's load it:
        if (DynamicConfigManager.getConfig(userFileName) == null) {
            DynamicConfigManager.loadConfig(resourceName);
        }
        return DynamicConfigManager.getConfig(userFileName);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        super.saveDefaultConfig();
        DynamicConfigManager.init(this);
        con = getMysticConfig();
        con.getOptions().copyDefaults(true);
    }

}
