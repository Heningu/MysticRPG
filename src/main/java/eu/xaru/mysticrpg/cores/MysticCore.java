package eu.xaru.mysticrpg.cores;

import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.xaru.mysticrpg.config.ConfigCreator;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    
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
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));

        PacketEvents.getAPI().load();
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Initialize the module manager
        moduleManager = ModuleManager.getInstance();
        DecentHologramsAPI.onLoad(this);
    }

    @Override
    public void onEnable() {

        PacketEvents.getAPI().init();
        try {
            // Load all modules (eagerly loaded modules)
            moduleManager.loadAllModules();
            CommandAPI.onEnable();
            DecentHologramsAPI.onEnable();


            DebugLogger.getInstance().log("Core plugin enabled successfully.", 0);

        } catch (Exception e) {
            // Attempt to log using JavaPlugin's logger if DebugLoggerModule isn't a

            DebugLogger.getInstance().error("Error during plugin enable. Exception:", e);

            getServer().getPluginManager().disablePlugin(this);
        }

        // Create config files and directories
        new ConfigCreator(this).createFiles();
    }

    @Override
    public void onDisable() {
        // Shutdown the plugin, unload modules, and clean up resources

        CommandAPI.onDisable();
        DecentHologramsAPI.onDisable();

        try {
            moduleManager.shutdown();
            DebugLogger.getInstance().log("Core plugin disabled.", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during plugin disable. Exception:", e);
        }
        PacketEvents.getAPI().terminate();
    }
}
