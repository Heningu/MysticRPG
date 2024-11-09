package eu.xaru.mysticrpg.social.friends;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The main module class for managing the Friends system.
 * Handles initialization, command registration, and event handling.
 */
public class FriendsModule implements IBaseModule {

    private MysticCore plugin;
    private EventManager eventManager;
    private DebugLoggerModule logger;
    private PlayerDataCache playerDataCache;
    private FriendsHelper friendsHelper;
    private PartyHelper partyHelper;

    // Map to track players' current pages in the Friends GUI
    private final java.util.Map<UUID, Integer> playerPages = new java.util.HashMap<>();

    /**
     * Initializes the FriendsModule by setting up necessary components.
     */
    @Override
    public void initialize() {
        // Retrieve the main plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        // Initialize the event manager
        this.eventManager = new EventManager(plugin);
        // Retrieve the debug logger module for logging purposes
        this.logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        // Initialize the player data cache
        this.playerDataCache = PlayerDataCache.getInstance(null, logger);
        // Initialize the friends helper
        this.friendsHelper = new FriendsHelper(playerDataCache, logger);

        // Retrieve the PartyHelper from the PartyModule dependency
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule != null) {
            this.partyHelper = partyModule.getPartyHelper();
        } else {
            logger.error("PartyModule not initialized. Party features in FriendsModule will not function.");
        }
    }

    /**
     * Starts the FriendsModule by registering commands and event handlers.
     */
    @Override
    public void start() {
        registerCommands();
        registerEvents();
    }

    /**
     * Stops the FriendsModule. Placeholder for any necessary cleanup.
     */
    @Override
    public void stop() {
        // Any necessary cleanup can be performed here
    }

    /**
     * Unloads the FriendsModule. Placeholder for any necessary unload actions.
     */
    @Override
    public void unload() {
        // Any necessary unload actions can be performed here
    }

    /**
     * Specifies the dependencies required by the FriendsModule.
     *
     * @return A list of module classes that FriendsModule depends on.
     */
    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, PartyModule.class);
    }

    /**
     * Specifies the priority level of the FriendsModule.
     *
     * @return The module priority.
     */
    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers the Friends commands using the CommandAPI.
     * Commands include adding, removing, accepting, and denying friends.
     */
    private void registerCommands() {
        new CommandAPICommand("friends")
                .withAliases("friend", "f")
                // /friends add <player>
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            try {
                                Player target = (Player) args.get("player");
                                friendsHelper.sendFriendRequest(player, target);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
                                logger.error("Error executing /friends add command: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }))
                // /friends remove <player>
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            try {
                                Player target = (Player) args.get("player");
                                friendsHelper.removeFriend(player, target);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
                                logger.error("Error executing /friends remove command: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }))
                // /friends accept <playerName>
                .withSubcommand(new CommandAPICommand("accept")
                        .withArguments(new StringArgument("playerName"))
                        .executesPlayer((player, args) -> {
                            try {
                                String senderName = (String) args.get("playerName");
                                friendsHelper.acceptFriendRequest(player, senderName);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
                                logger.error("Error executing /friends accept command: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }))
                // /friends deny <playerName>
                .withSubcommand(new CommandAPICommand("deny")
                        .withArguments(new StringArgument("playerName"))
                        .executesPlayer((player, args) -> {
                            try {
                                String senderName = (String) args.get("playerName");
                                friendsHelper.denyFriendRequest(player, senderName);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
                                logger.error("Error executing /friends deny command: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }))
                // /friends (opens the Friends GUI)
                .executesPlayer((player, args) -> {
                    FriendsGUI.openFriendsGUI(player, playerDataCache, false, 1);
                    // Track the current page for the player
                    playerPages.put(player.getUniqueId(), 1);
                })
                .register();
    }

    /**
     * Registers event handlers related to the Friends system.
     * This includes handling inventory clicks and inventory closures.
     */
    private void registerEvents() {
        // Handle PlayerJoinEvent to notify players of pending friend requests if reminders are enabled
        eventManager.registerEvent(PlayerJoinEvent.class, (Consumer<PlayerJoinEvent>) event -> {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

            if (playerData != null && playerData.isRemindersEnabled()) {
                int pendingRequests = playerData.getFriendRequests().size();
                if (pendingRequests > 0) {
                    player.sendMessage(ChatColor.YELLOW + "You have " + pendingRequests + " pending friend request(s).");
                }
            }
        });

        // Handle InventoryClickEvent for the Friends GUI
        eventManager.registerEvent(InventoryClickEvent.class, (Consumer<InventoryClickEvent>) event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null) return;

            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
            ItemStack clickedItem = event.getCurrentItem();

            if (!inventoryTitle.equalsIgnoreCase("Friends")) return;

            event.setCancelled(true); // Prevent taking items

            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            switch (itemName) {
                case "Friend Requests":
                    // Open Friend Requests GUI or perform related action
                    player.sendMessage(ChatColor.YELLOW + "Opening Friend Requests...");
                    // Implement the Friend Requests GUI here
                    // Example:
                    // FriendRequestsGUI.openFriendRequestsGUI(player, playerDataCache, false, 1);
                    break;

                case "Close":
                    player.closeInventory();
                    break;

                case "Reminders: ON":
                case "Reminders: OFF":
                    toggleReminders(player);
                    break;

                case "Previous Page":
                    navigatePage(player, -1);
                    break;

                case "Next Page":
                    navigatePage(player, 1);
                    break;

                default:
                    // Handle clicks on friend heads if needed
                    // For example, opening a profile view or sending a message
                    // Implement additional cases as necessary
                    break;
            }
        }, EventPriority.NORMAL);

        // Handle InventoryCloseEvent to clean up page tracking
        eventManager.registerEvent(org.bukkit.event.inventory.InventoryCloseEvent.class, (Consumer<org.bukkit.event.inventory.InventoryCloseEvent>) event -> {
            if (!(event.getPlayer() instanceof Player)) return;

            Player player = (Player) event.getPlayer();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());

            if (inventoryTitle.equalsIgnoreCase("Friends")) {
                // Remove the player's page tracking
                playerPages.remove(player.getUniqueId());
            }
        }, EventPriority.NORMAL);
    }

    /**
     * Toggles the reminders setting for the player.
     *
     * @param player The player whose reminders are to be toggled.
     */
    private void toggleReminders(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

        if (playerData != null) {
            boolean currentStatus = playerData.isRemindersEnabled();
            playerData.setRemindersEnabled(!currentStatus);
            String newStatus = !currentStatus ? "ON" : "OFF";

            // Update the GUI
            if (player.getOpenInventory().getTitle().equalsIgnoreCase("Friends")) {
                Inventory inventory = player.getOpenInventory().getTopInventory();
                ItemStack reminderTorch = inventory.getItem(51); // Slot 51
                if (reminderTorch != null) {
                    CustomInventoryManager.setItemDisplayName(reminderTorch, ChatColor.GOLD + "Reminders: " + newStatus);
                }
            }

            player.sendMessage(ChatColor.GREEN + "Reminders have been turned " + newStatus + ".");
        }
    }

    /**
     * Navigates to the next or previous page in the Friends GUI.
     *
     * @param player The player navigating the GUI.
     * @param direction The direction to navigate (-1 for previous, 1 for next).
     */
    private void navigatePage(Player player, int direction) {
        UUID playerUUID = player.getUniqueId();
        int currentPage = playerPages.getOrDefault(playerUUID, 1);
        int newPage = currentPage + direction;

        // Update the page tracking
        playerPages.put(playerUUID, newPage);

        // Reopen the Friends GUI with the new page
        FriendsGUI.openFriendsGUI(player, playerDataCache, false, newPage);
    }
}
