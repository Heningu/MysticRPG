package eu.xaru.mysticrpg.cores;

import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import com.github.juliarn.npclib.bukkit.BukkitWorldAccessor;
import com.github.juliarn.npclib.bukkit.protocol.BukkitProtocolAdapter;
import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import eu.xaru.mysticrpg.config.ConfigCreator;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    private DebugLoggerModule logger;
    public static MysticCore instance = null;

    private Platform<World, Player, ItemStack, Plugin> platform;

    public Platform<World, Player, ItemStack, Plugin> getPlatform() {
        return platform;
    }


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
    }

    @Override
    public void onEnable() {

        PacketEvents.getAPI().init();
        platform = BukkitPlatform
                .bukkitNpcPlatformBuilder()
                .extension(this)
                .packetFactory(BukkitProtocolAdapter.packetEvents())
                .actionController(builder -> {}) // enable action controller without changing the default config
                .build();
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
        PacketEvents.getAPI().terminate();
    }
}
