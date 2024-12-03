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
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Module responsible for registering the /mainmenu command and handling the main menu GUI interactions.
 */
public class GUIRegisterModule implements IBaseModule, Listener {

    private MysticCore plugin;
    private EventManager eventManager;
    private DebugLoggerModule debugLogger;
    private PlayerStatModule playerStat;
    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;
    private MainMenu mainMenu;

    /**
     * Initializes the GUIRegisterModule by setting up necessary components.
     */
    @Override
    public void initialize() {
        // Retrieve the debug logger
        this.debugLogger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        if (this.debugLogger == null) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to initialize GUIRegisterModule: DebugLoggerModule is null.");
            throw new IllegalStateException("DebugLoggerModule is not loaded. GUIRegisterModule cannot function.");
        }

        debugLogger.log(Level.INFO, "Initializing GUIRegisterModule...", 0);

        // Retrieve the main plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        // Initialize the event manager
        this.eventManager = new EventManager(plugin);

        // Retrieve AuctionHouseModule
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        if (this.auctionHouse == null) {
            debugLogger.error("AuctionHouse module is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("AuctionHouse is not loaded.");
        }

        // Retrieve EquipmentModule
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        if (this.equipmentModule == null) {
            debugLogger.error("EquipmentModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("EquipmentModule is not loaded.");
        }

        // Retrieve LevelModule
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        if (this.levelingModule == null) {
            debugLogger.error("LevelModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("LevelModule is not loaded.");
        }

        // Retrieve PlayerStatModule
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        if (this.playerStat == null) {
            debugLogger.error("PlayerStatModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("PlayerStatModule is not loaded.");
        }

        // Retrieve QuestModule
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        if (this.questModule == null) {
            debugLogger.error("QuestModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("QuestModule is not loaded.");
        }

        // Retrieve FriendsModule
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        if (this.friendsModule == null) {
            debugLogger.error("FriendsModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("FriendsModule is not loaded.");
        }

        // Retrieve PartyModule
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (this.partyModule == null) {
            debugLogger.error("PartyModule is not loaded. GUIRegisterModule requires it to function.");
            throw new IllegalStateException("PartyModule is not loaded.");
        }

        // Initialize the MainMenu
        this.mainMenu = new MainMenu();

        debugLogger.log(Level.INFO, "GUIRegisterModule initialization complete.", 0);
    }

    /**
     * Starts the GUIRegisterModule by registering commands and event handlers.
     */
    @Override
    public void start() {
        registerCommands();
        registerEventListeners();
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
                DebugLoggerModule.class,
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
                    openMainMenu(player);
                })
                .register();

        debugLogger.log(Level.INFO, "GUIRegisterModule commands registered.", 0);
    }

    /**
     * Registers the event listeners required for the main menu interactions.
     */
    private void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        debugLogger.log(Level.INFO, "GUIRegisterModule event listeners registered.", 0);
    }

    /**
     * Handles inventory click events to manage interactions within the main menu GUI
     * and prevent movement of the main menu item.
     *
     * @param event The InventoryClickEvent triggered when a player clicks in an inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ensure the entity clicking is a player
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle == null) return;

        // Strip color codes to get the plain title
        String strippedTitle = ChatColor.stripColor(inventoryTitle);

        if ("Main Menu".equals(strippedTitle)) {
            // Handle interactions within the Main Menu GUI
            event.setCancelled(true); // Prevent taking or moving items in the GUI

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return; // Do nothing if the clicked slot is empty or has no metadata
            }

            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            switch (displayName) {
                case "Auctions":
                    // Close the main menu before opening the Auctions GUI
                    player.closeInventory();
                    auctionHouse.openAuctionGUI(player);
                    debugLogger.log(Level.INFO, player.getName() + " opened the Auctions GUI from Main Menu.", 0);
                    break;

                case "Equipment":
                    // Close the main menu before opening the Equipment GUI
                    player.closeInventory();
                    equipmentModule.getEquipmentManager().getEquipmentGUI().openEquipmentGUI(player);
                    debugLogger.log(Level.INFO, player.getName() + " opened the Equipment GUI from Main Menu.", 0);
                    break;

                case "Leveling":
                    // Close the main menu before opening the Leveling GUI
                    player.closeInventory();
                    levelingModule.getLevelingMenu().openLevelingMenu(player, 1);
                    debugLogger.log(Level.INFO, player.getName() + " opened the Leveling GUI from Main Menu.", 0);
                    break;

                case "Stats":
                    // Close the main menu before opening the Stats GUI
                    player.closeInventory();
                    playerStat.getPlayerStatMenu().openStatMenu(player);
                    debugLogger.log(Level.INFO, player.getName() + " opened the Stats GUI from Main Menu.", 0);
                    break;

                case "Quests":
                    // Close the main menu before opening the Quest GUI
                    player.closeInventory();
                    questModule.openQuestGUI(player); // Use the method from QuestModule
                    debugLogger.log(Level.INFO, player.getName() + " opened the Quest GUI from Main Menu.", 0);
                    break;

                case "Friends":
                    // Close the main menu before opening the Friends GUI
                    player.closeInventory();
                    friendsModule.openFriendsGUI(player); // Use the method from FriendsModule
                    debugLogger.log(Level.INFO, player.getName() + " opened the Friends GUI from Main Menu.", 0);
                    break;

                case "Party":
                    // Close the main menu before opening the Party GUI
                    player.closeInventory();
                    partyModule.openPartyGUI(player); // Use the method from PartyModule
                    debugLogger.log(Level.INFO, player.getName() + " opened the Party GUI from Main Menu.", 0);
                    break;

                default:
                    // Optional: Handle clicks on unknown items or provide feedback
                    player.sendMessage(ChatColor.YELLOW + "This feature is not yet implemented.");
                    debugLogger.log(Level.WARNING, player.getName() + " clicked an unknown item in the Main Menu: " + displayName, 0);
                    break;
            }
        } else {
            // Not the Main Menu GUI
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null) return;

            if (isMainMenuItem(currentItem)) {
                // Cancel the event to prevent moving the item
                event.setCancelled(true);

                // If the item is clicked in the player's inventory, open the main menu
                if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
                    openMainMenu(player);
                }
            }
        }
    }

    /**
     * Prevents the main menu item from being dropped.
     *
     * @param event The PlayerDropItemEvent triggered when a player drops an item.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isMainMenuItem(droppedItem)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles player interaction with the main menu item in hand.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();
        if (itemInHand == null) return;

        if (isMainMenuItem(itemInHand)) {
            // Open the main menu
            event.setCancelled(true);
            openMainMenu(player);
        }
    }

    /**
     * Prevents the main menu item from being moved via dragging.
     *
     * @param event The InventoryDragEvent triggered when a player drags items in inventory.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack oldCursor = event.getOldCursor();
        if (isMainMenuItem(oldCursor)) {
            event.setCancelled(true);
        }

        // Check if the main menu item is being dragged
        for (ItemStack item : event.getNewItems().values()) {
            if (isMainMenuItem(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    /**
     * Gives the main menu item to the player upon joining.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack mainMenuItem = createMainMenuItem();
        player.getInventory().setItem(8, mainMenuItem);
    }

    /**
     * Creates the main menu item to be placed in the player's hotbar.
     *
     * @return The ItemStack representing the main menu item.
     */
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

    /**
     * Checks if the given item is the main menu item.
     *
     * @param item The ItemStack to check.
     * @return True if the item is the main menu item, false otherwise.
     */
    private boolean isMainMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "main_menu_item");
        Byte result = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return result != null && result == (byte) 1;
    }

    /**
     * Opens the main menu for the player.
     *
     * @param player The player for whom to open the main menu.
     */
    private void openMainMenu(Player player) {
        Inventory mainMenuInventory = mainMenu.getInventory(player); // Pass the player
        if (mainMenuInventory != null) {
            CustomInventoryManager.openInventory(player, mainMenuInventory);
            debugLogger.log(Level.INFO, player.getName() + " opened the Main Menu via item.", 0);
        } else {
            player.sendMessage(ChatColor.RED + "Main Menu is currently unavailable.");
            debugLogger.error("Main Menu inventory is null.");
        }
    }
}
