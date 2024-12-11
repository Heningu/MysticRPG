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
        registerEvents();

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

    private void registerEvents() {
        // Prevent dropping the main menu item
        eventManager.registerEvent(PlayerDropItemEvent.class, event -> {
            ItemStack droppedItem = event.getItemDrop().getItemStack();
            log.info("DropEvent: Player={}, DroppedItem={}, IsMainMenuItem={}", event.getPlayer().getName(),
                    droppedItem != null ? droppedItem.getType() + " (" + droppedItem.getItemMeta().getDisplayName() + ")" : "null",
                    isMainMenuItem(droppedItem));
            if (isMainMenuItem(droppedItem)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop the Main Menu item.");
            }
        }, EventPriority.HIGHEST);

        // Open main menu on interaction (when held in hand)
        eventManager.registerEvent(PlayerInteractEvent.class, event -> {
            Player player = event.getPlayer();
            ItemStack itemInHand = event.getItem();
            if (itemInHand == null) return;

            boolean mainMenu = isMainMenuItem(itemInHand);
            log.info("PlayerInteractEvent: Player={}, ItemInHand={}, IsMainMenuItem={}", player.getName(),
                    itemInHand.getType() + " (" + itemInHand.getItemMeta().getDisplayName() + ")", mainMenu);

            if (mainMenu) {
                event.setCancelled(true);
                openMainMenu(player);
            }
        }, EventPriority.HIGHEST);

        // Open main menu if clicked inside inventory on the main menu item
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            Player player = (Player) event.getWhoClicked();
            ItemStack currentItem = event.getCurrentItem();

            log.info("InventoryClickEvent: Player={}, Slot={}, Click={}, CurrentItem={}, CursorItem={}",
                    player.getName(),
                    event.getRawSlot(),
                    event.getClick(),
                    currentItem != null ? currentItem.getType() + " (" + currentItem.getItemMeta().getDisplayName() + ")" : "null",
                    event.getCursor() != null ? event.getCursor().getType() + " (" + event.getCursor().getItemMeta().getDisplayName() + ")" : "null"
            );

            if (isMainMenuItem(currentItem)) {
                // Cancel the event so they cannot pick it up
                event.setCancelled(true);
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Opening main menu...");
                openMainMenu(player);
                log.info("Player {} clicked the main menu item in inventory and menu opened. No dragging possible.", player.getName());
            }

        }, EventPriority.HIGHEST);

        // Prevent dragging the item
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            Player player = (Player) event.getWhoClicked();
            ItemStack oldCursor = event.getOldCursor();

            log.info("InventoryDragEvent: Player={}, OldCursor={}, OldCursorMainMenuItem={}",
                    player.getName(),
                    oldCursor != null ? oldCursor.getType() + "(" + oldCursor.getItemMeta().getDisplayName() + ")" : "null",
                    isMainMenuItem(oldCursor));

            // If oldCursor is main menu item
            if (isMainMenuItem(oldCursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot drag the Main Menu item.");
                log.info("Player {} tried to drag main menu item via oldCursor.", player.getName());
            }

            // Check the new items being placed by the drag event
            event.getNewItems().forEach((slot, item) -> {
                boolean mainMenu = isMainMenuItem(item);
                log.info("Drag new items: Slot={}, Item={}, IsMainMenuItem={}", slot,
                        item != null ? item.getType() + " (" + item.getItemMeta().getDisplayName() + ")" : "null",
                        mainMenu
                );
                if (mainMenu) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot move the Main Menu item.");
                    log.info("Player {} attempted to drag main menu item into slot {}.", player.getName(), slot);
                }
            });

        }, EventPriority.HIGHEST);

        // Give the main menu item upon joining
        eventManager.registerEvent(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();
            ItemStack mainMenuItem = createMainMenuItem();
            player.getInventory().setItem(8, mainMenuItem);
            log.info("Gave main menu item to {} in slot 8 on join.", player.getName());
        }, EventPriority.HIGHEST);
    }

    private ItemStack createMainMenuItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Main Menu");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to open the main menu."));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);

        // Mark the item with a custom key to identify it later
        NamespacedKey key = new NamespacedKey(plugin, "main_menu_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        return item;
    }

    private boolean isMainMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "main_menu_item");
        Byte result = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);

        boolean mainMenu = result != null && result == (byte) 1;

        log.info("isMainMenuItem check: Item={}, DisplayName={}, HasKey={}, Result={}, Determined={}",
                item.getType(),
                meta.hasDisplayName() ? meta.getDisplayName() : "NoName",
                result != null,
                result,
                mainMenu);

        return mainMenu;
    }

    private void openMainMenu(Player player) {
        MainMenu test = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
        test.openGUI(player);
    }
}
