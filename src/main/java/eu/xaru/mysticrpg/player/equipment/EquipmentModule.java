package eu.xaru.mysticrpg.player.equipment;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
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
    
    private EquipmentManager equipmentManager;

    public EquipmentModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        // Get instances of required modules


        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            DebugLogger.getInstance().error("SaveModule not initialized. EquipmentModule cannot function without it.");
            return;
        }

        // Initialize EquipmentManager
        equipmentManager = new EquipmentManager(plugin);

     //   DebugLogger.getInstance().log(Level.INFO, "EquipmentModule initialized successfully.", 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( SaveModule.class);
    }

    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }
}
