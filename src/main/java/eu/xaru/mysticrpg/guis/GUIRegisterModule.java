package eu.xaru.mysticrpg.guis;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class GUIRegisterModule implements IBaseModule {

    private static final Logger log = LoggerFactory.getLogger(GUIRegisterModule.class);
    private MysticCore plugin;

    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private PlayerStatModule playerStat;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;
    private EventManager eventManager;

    @Override
    public void initialize() {
        DebugLogger.getInstance().log(Level.INFO, "Initializing GUIRegisterModule...", 0);

        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        this.eventManager = new EventManager(plugin);

        validateModules();

        DebugLogger.getInstance().log(Level.INFO, "GUIRegisterModule initialization complete.", 0);
    }

    private void validateModules() {
        if (this.auctionHouse == null) {
            DebugLogger.getInstance().error("AuctionHouse module is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("AuctionHouse is not loaded.");
        }

        if (this.equipmentModule == null) {
            DebugLogger.getInstance().error("EquipmentModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("EquipmentModule is not loaded.");
        }

        if (this.levelingModule == null) {
            DebugLogger.getInstance().error("LevelModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("LevelModule is not loaded.");
        }

        if (this.playerStat == null) {
            DebugLogger.getInstance().error("PlayerStatModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("PlayerStatModule is not loaded.");
        }

        if (this.questModule == null) {
            DebugLogger.getInstance().error("QuestModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("QuestModule is not loaded.");
        }

        if (this.friendsModule == null) {
            DebugLogger.getInstance().error("FriendsModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("FriendsModule is not loaded.");
        }

        if (this.partyModule == null) {
            DebugLogger.getInstance().error("PartyModule is not loaded. GUIRegisterModule requires it.");
            throw new IllegalStateException("PartyModule is not loaded.");
        }
    }

    @Override
    public void start() {
        registerCommands();
    }

    @Override
    public void stop() {
        // Cleanup if necessary
    }

    @Override
    public void unload() {
        // Unload actions if necessary
    }

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

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void registerCommands() {
        new CommandAPICommand("mainmenu")
                .withPermission("mysticrpg.mainmenu.use")
                .executesPlayer((player, args) -> {
                    openMainMenu(player);
                })
                .register();

        DebugLogger.getInstance().log(Level.INFO, "GUIRegisterModule commands registered.", 0);
    }

    private void openMainMenu(Player player) {
        MainMenu test = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
        test.openGUI(player);
    }
}
