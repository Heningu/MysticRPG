package eu.xaru.mysticrpg.games;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * A module that initializes and starts the slot game feature.
 * Depends on EconomyModule for currency handling.
 */
public class SlotGameModule implements IBaseModule {

    private MysticCore plugin;
    private EventManager eventManager;
    private EconomyHelper economyHelper;
    private SlotGameHelper slotGameHelper;
    private SlotGameManager slotGameManager;

    @Override
    public void initialize() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);

        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule != null) {
            this.economyHelper = economyModule.getEconomyHelper();
        } else {
            DebugLogger.getInstance().error("EconomyModule not initialized. SlotGameModule cannot start.");
            return;
        }

        // Create our slot game helper for spinning logic & payouts
        this.slotGameHelper = new SlotGameHelper(plugin, economyHelper);

       // DebugLogger.getInstance().log(Level.INFO, "SlotGameModule initialized", 0);
    }

    @Override
    public void start() {

        // Create the SlotGameManager, which registers the /game command automatically
        this.slotGameManager = new SlotGameManager(economyHelper);

        // (Optional) If you have any additional events to register, do so here using eventManager
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        // We depend on EconomyModule (for money) and SaveModule (if you need storage)
        return List.of(EconomyModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }
}
