package eu.xaru.mysticrpg.guis;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;

import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.logging.Level;

/**
 * Module responsible for registering all GUIs using InvUI's GUIRegisterModule.
 */
public class GUIRegisterModule implements IBaseModule {

    private MysticCore plugin;

    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private PlayerStatModule playerStat;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;

    /**
     * Initializes the GUIRegisterModule by setting up necessary components and registering GUIs.
     */
    @Override
    public void initialize() {

        DebugLogger.getInstance().log(Level.INFO, "Initializing GUIRegisterModule...", 0);

        // Retrieve the main plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        // Retrieve required modules
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);

        // Validate module dependencies
        validateModules();

        // Register all GUIs with InvUI's GUIRegisterModule
        registerGUIs();

        DebugLogger.getInstance().log(Level.INFO, "GUIRegisterModule initialization complete.", 0);
    }

    /**
     * Validates that all required modules are loaded.
     */
    private void validateModules() {
        if (this.auctionHouse == null) {
            DebugLogger.getInstance().error("AuctionHouse module is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("AuctionHouse is not loaded.");
        }

        if (this.equipmentModule == null) {
            DebugLogger.getInstance().error("EquipmentModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("EquipmentModule is not loaded.");
        }

        if (this.levelingModule == null) {
            DebugLogger.getInstance().error("LevelModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("LevelModule is not loaded.");
        }

        if (this.playerStat == null) {
            DebugLogger.getInstance().error("PlayerStatModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("PlayerStatModule is not loaded.");
        }

        if (this.questModule == null) {
            DebugLogger.getInstance().error("QuestModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("QuestModule is not loaded.");
        }

        if (this.friendsModule == null) {
            DebugLogger.getInstance().error("FriendsModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("FriendsModule is not loaded.");
        }

        if (this.partyModule == null) {
            DebugLogger.getInstance().error("PartyModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("PartyModule is not loaded.");
        }
    }

    /**
     * Starts the GUIRegisterModule by registering commands.
     */
    @Override
    public void start() {
        registerCommands();
    }

    /**
     * Stops the GUIRegisterModule. Placeholder for any necessary cleanup.
     */
    @Override
    public void stop() {
        // Any necessary cleanup can be performed here
    }

    /**
     * Unloads the GUIRegisterModule. Placeholder for any necessary unload actions.
     */
    @Override
    public void unload() {
        // Any necessary unload actions can be performed here
    }

    /**
     * Specifies the dependencies required by the GUIRegisterModule.
     *
     * @return A list of module classes that GUIRegisterModule depends on.
     */
    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(
                AuctionHouseModule.class,
                EquipmentModule.class,
                LevelModule.class,
                PlayerStatModule.class,
                QuestModule.class,
                FriendsModule.class,
                PartyModule.class
        );
    }

    /**
     * Specifies the priority level of the GUIRegisterModule.
     *
     * @return The module priority.
     */
    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Registers the /mainmenu command using the CommandAPI.
     */
    private void registerCommands() {
        new CommandAPICommand("mainmenu")
                .withPermission("mysticrpg.mainmenu.use") // Permission check
                .executesPlayer((player, args) -> {
                    MainMenu test = new MainMenu(auctionHouse,equipmentModule,levelingModule,playerStat,questModule,friendsModule,partyModule);
                    test.openGUI(player);
                })
                .register();

        DebugLogger.getInstance().log(Level.INFO, "GUIRegisterModule commands registered.", 0);
    }

    /**
     * Registers all GUI classes with InvUI's GUIRegisterModule.
     * In this implementation, GUIs are registered and managed individually.
     */
    private void registerGUIs() {
        // If InvUI requires a central registration, it can be done here.
        // For this example, GUIs are instantiated and opened as needed.
        // Additional GUIs can be registered similarly.

        DebugLogger.getInstance().log(Level.INFO, "All GUIs registered with InvUI's GUIRegisterModule.", 0);
    }
}
