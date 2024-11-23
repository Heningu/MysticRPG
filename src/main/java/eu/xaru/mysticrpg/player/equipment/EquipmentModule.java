package eu.xaru.mysticrpg.player.equipment;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * EquipmentModule initializes and manages the equipment system.
 */
public class EquipmentModule implements IBaseModule, Listener {

    private final JavaPlugin plugin;
    private DebugLoggerModule logger;
    private EquipmentManager equipmentManager;

    public EquipmentModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        // Get instances of required modules
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            Bukkit.getLogger().severe("DebugLoggerModule not initialized. EquipmentModule cannot function without it.");
            return;
        }

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            logger.error("SaveModule not initialized. EquipmentModule cannot function without it.");
            return;
        }

        // Initialize EquipmentManager
        equipmentManager = new EquipmentManager(plugin, logger);

        // Register commands
        registerEquipmentCommand();

        logger.log(Level.INFO, "EquipmentModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "EquipmentModule started.", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "EquipmentModule stopped.", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "EquipmentModule unloaded.", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers the equipment-related commands using CommandAPI.
     */
    private void registerEquipmentCommand() {
        new CommandAPICommand("equipment")
                .withAliases("equip")
                .withPermission("mysticrpg.equipment")
                .executesPlayer((player, args) -> {
                    equipmentManager.getEquipmentGUI().openEquipmentGUI(player);
                })
                .register();
    }
}
