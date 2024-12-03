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
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
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
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // Map to track players' current pages in the Friend Requests GUI
    private final Map<UUID, Integer> friendRequestsPages = new HashMap<>();

    // Map to track players' selected friends for the Friend Options GUI
    private final Map<UUID, UUID> openFriendOptions = new HashMap<>();

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
        this.playerDataCache = PlayerDataCache.getInstance();
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
        return EModulePriority.LOW;
    }

    /**
     * Registers the Friends commands using the CommandAPI.
     * Commands include adding, removing, accepting, and denying friends.
     */
    private void registerCommands() {
        new CommandAPICommand("friends")
                .withAliases("f")
                // /friends add <player>
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            try {
                                Player target = (Player) args.get("player");
                                friendsHelper.sendFriendRequest(player, target);
                            } catch (Exception e) {
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
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
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
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
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
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
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
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
     * Opens the Friends GUI for the specified player.
     *
     * @param player The player for whom the Friends GUI should be opened.
     */
    public void openFriendsGUI(Player player) {
        FriendsGUI.openFriendsGUI(player, playerDataCache, false, 1); // 'false' and '1' are placeholders for additional parameters
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
                    player.sendMessage(Utils.getInstance().$("You have " + pendingRequests + " pending friend request(s)."));
                }
            }
        });

        // Handle InventoryClickEvent for the Friends GUI, Friend Requests GUI, and Friend Options GUI
        eventManager.registerEvent(InventoryClickEvent.class, (Consumer<InventoryClickEvent>) event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null) return;

            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
            ItemStack clickedItem = event.getCurrentItem();

            if (inventoryTitle.equalsIgnoreCase("Friends")) {
                handleFriendsInventoryClick(event, player, clickedItem);
                event.setCancelled(true); // Prevent item movement
            } else if (inventoryTitle.equalsIgnoreCase("Friend Requests")) {
                handleFriendRequestsInventoryClick(event, player, clickedItem);
                event.setCancelled(true); // Prevent item movement
            } else {
                // Check if the inventory title matches any friend's name the player has open
                UUID friendUUID = openFriendOptions.get(player.getUniqueId());
                if (friendUUID != null) {
                    String friendName = Bukkit.getOfflinePlayer(friendUUID).getName();
                    if (inventoryTitle.equalsIgnoreCase(friendName)) {
                        handleFriendOptionsInventoryClick(event, player, clickedItem, friendUUID);
                        event.setCancelled(true); // Prevent item movement
                        return;
                    }
                }
            }
        }, EventPriority.NORMAL);

        // Handle InventoryDragEvent for the Friends GUI, Friend Requests GUI, and Friend Options GUI
        eventManager.registerEvent(InventoryDragEvent.class, (Consumer<InventoryDragEvent>) event -> {
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
            if ("Friends".equalsIgnoreCase(inventoryTitle) || "Friend Requests".equalsIgnoreCase(inventoryTitle)) {
                event.setCancelled(true); // Prevent item dragging
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(Utils.getInstance().$("You cannot move items in this GUI."));
            } else {
                // Check if the inventory title matches any friend's name the player has open
                Player player = (Player) event.getWhoClicked();
                UUID friendUUID = openFriendOptions.get(player.getUniqueId());
                if (friendUUID != null) {
                    String friendName = Bukkit.getOfflinePlayer(friendUUID).getName();
                    if (inventoryTitle.equalsIgnoreCase(friendName)) {
                        event.setCancelled(true); // Prevent item dragging
                        player.sendMessage(Utils.getInstance().$("You cannot move items in this GUI."));
                    }
                }
            }
        }, EventPriority.NORMAL);

        // Handle InventoryCloseEvent to clean up page tracking and sub-GUI tracking
        eventManager.registerEvent(org.bukkit.event.inventory.InventoryCloseEvent.class, (Consumer<org.bukkit.event.inventory.InventoryCloseEvent>) event -> {
            if (!(event.getPlayer() instanceof Player)) return;

            Player player = (Player) event.getPlayer();
            String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());

            if (inventoryTitle.equalsIgnoreCase("Friends")) {
                // Remove the player's page tracking
                playerPages.remove(player.getUniqueId());
            } else if (inventoryTitle.equalsIgnoreCase("Friend Requests")) {
                // Remove the player's friend requests page tracking
                friendRequestsPages.remove(player.getUniqueId());
            } else {
                // Check if the inventory title matches any friend's name the player has open
                UUID friendUUID = openFriendOptions.get(player.getUniqueId());
                if (friendUUID != null) {
                    String friendName = Bukkit.getOfflinePlayer(friendUUID).getName();
                    if (inventoryTitle.equalsIgnoreCase(friendName)) {
                        // Remove the player's friend option tracking
                        openFriendOptions.remove(player.getUniqueId());
                    }
                }
            }
        }, EventPriority.NORMAL);
    }

    /**
     * Handles clicks within the Friends GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleFriendsInventoryClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        // Check if the clicked item is a player head or skeleton skull
        Material itemType = clickedItem.getType();
        if (itemType == Material.PLAYER_HEAD || itemType == Material.SKELETON_SKULL) {
            // Handle friend head click
            handleFriendHeadClick(event, player, clickedItem);
            return;
        }

        // Handle other clickable items by their display name
        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        switch (itemName) {
            case "Friend Requests":
                // Open Friend Requests GUI
                FriendRequestsGUI.openFriendRequestsGUI(player, playerDataCache, 1);
                // Track the current page for the player
                friendRequestsPages.put(player.getUniqueId(), 1);
                break;

            case "Close":
                player.closeInventory();
                break;

            case "Reminders: ON":
            case "Reminders: OFF":
                toggleReminders(player);
                break;

            default:
                // Do nothing for other items
                break;
        }
    }

    /**
     * Handles clicks within the Friend Requests GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleFriendRequestsInventoryClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        // Check if the clicked item is the back button
        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (itemName.equalsIgnoreCase("Back") && clickedItem.getType() == Material.ARROW) {
            // Remove the player's page tracking
            friendRequestsPages.remove(player.getUniqueId());
            // Reopen the main Friends GUI
            FriendsGUI.openFriendsGUI(player, playerDataCache, false, 1);
            return;
        }

        // Check if the clicked item is a player head or skeleton skull
        Material itemType = clickedItem.getType();
        if (itemType == Material.PLAYER_HEAD || itemType == Material.SKELETON_SKULL) {
            // Determine if left or right click
            boolean isLeftClick = event.isLeftClick();
            boolean isRightClick = event.isRightClick();

            if (isLeftClick || isRightClick) {
                // Get the sender's UUID from the item's lore
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                if (meta == null || !meta.hasLore()) {
                    player.sendMessage(Utils.getInstance().$("Unable to retrieve friend request information."));
                    return;
                }

                List<String> lore = meta.getLore();
                if (lore == null || lore.isEmpty()) {
                    player.sendMessage(Utils.getInstance().$("Unable to retrieve friend request information."));
                    return;
                }

                String senderUUIDString = lore.get(0);
                UUID senderUUID;
                try {
                    senderUUID = UUID.fromString(senderUUIDString);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Utils.getInstance().$("Invalid friend request data."));
                    return;
                }

                OfflinePlayer sender = Bukkit.getOfflinePlayer(senderUUID);

                if (isLeftClick) {
                    // Accept the friend request
                    friendsHelper.acceptFriendRequest(player, sender.getName());
                } else if (isRightClick) {
                    // Deny the friend request
                    friendsHelper.denyFriendRequest(player, sender.getName());
                }

                // Refresh the Friend Requests GUI
                FriendRequestsGUI.openFriendRequestsGUI(player, playerDataCache, friendRequestsPages.getOrDefault(player.getUniqueId(), 1));
            }
            return;
        }
    }

    /**
     * Handles clicks within the Friend Options GUI.
     *
     * @param event      The InventoryClickEvent.
     * @param player     The player who clicked.
     * @param clickedItem The item that was clicked.
     * @param friendUUID The UUID of the friend associated with this GUI.
     */
    private void handleFriendOptionsInventoryClick(InventoryClickEvent event, Player player, ItemStack clickedItem, UUID friendUUID) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        // Check if the clicked item is one of the button materials
        Material itemType = clickedItem.getType();
        if (!(itemType == Material.CAKE || itemType == Material.PAPER || itemType == Material.REDSTONE_BLOCK || itemType == Material.ARROW)) {
            // Clicked item is a placeholder or unhandled, do nothing
            return;
        }

        OfflinePlayer friend = Bukkit.getOfflinePlayer(friendUUID);
        String friendName = (friend.getName() != null) ? friend.getName() : "Unknown";

        switch (itemName) {
            case "Invite to Party":
                // Implement invite to party logic using PartyHelper
                inviteFriendToParty(player, friend);
                break;

            case "Send a Message":
                // Placeholder: Implement send message logic
                player.sendMessage(Utils.getInstance().$("Send a Message clicked. (Not implemented yet)"));
                break;

            case "Remove Friend":
                // Remove the friend
                friendsHelper.removeFriend(player, friend);
                // Remove from map and reopen Friends GUI
                openFriendOptions.remove(player.getUniqueId());
                FriendsGUI.openFriendsGUI(player, playerDataCache, false, 1);
                // Optionally, notify the player
                player.sendMessage(Utils.getInstance().$("Removed " + friendName + " from your friends list."));
                break;

            case "Back":
                // Remove from map and reopen Friends GUI
                openFriendOptions.remove(player.getUniqueId());
                FriendsGUI.openFriendsGUI(player, playerDataCache, false, 1);
                break;

            default:
                // Unknown item clicked
                break;
        }
    }

    /**
     * Invites the friend to the party using PartyHelper.
     *
     * @param inviter The player sending the invitation.
     * @param invitee The player being invited.
     */
    private void inviteFriendToParty(Player inviter, OfflinePlayer invitee) {
        if (invitee == null || invitee.getUniqueId() == null) {
            inviter.sendMessage(Utils.getInstance().$("Unable to find the player to invite."));
            return;
        }

        UUID inviterUUID = inviter.getUniqueId();
        UUID inviteeUUID = invitee.getUniqueId();

        // Ensure that the invitee is online to receive the invitation
        if (!invitee.isOnline()) {
            inviter.sendMessage(Utils.getInstance().$(invitee.getName() + " is not online."));
            return;
        }

        Player inviteePlayer = invitee.getPlayer();
        if (inviteePlayer == null) {
            inviter.sendMessage(Utils.getInstance().$("Unable to find the player to invite."));
            return;
        }

        // Use PartyHelper to send the party invitation
        partyHelper.invitePlayer(inviter, inviteePlayer);

        // Optionally, you can add feedback to the inviter
        inviter.sendMessage(Utils.getInstance().$("You have invited " + invitee.getName() + " to your party."));
    }

    /**
     * Handles clicks on friend heads within the Friends GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleFriendHeadClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        // Get the friend's UUID from the item's lore
        SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            player.sendMessage(Utils.getInstance().$("Unable to retrieve friend information."));
            return;
        }

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("Unable to retrieve friend information."));
            return;
        }

        String friendUUIDString = lore.get(0);
        UUID friendUUID;
        try {
            friendUUID = UUID.fromString(friendUUIDString);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Utils.getInstance().$("Invalid friend data."));
            return;
        }

        // Track the selected friend in the map
        openFriendOptions.put(player.getUniqueId(), friendUUID);

        // Open the Friend Options GUI
        FriendsGUI.openFriendOptionsGUI(player, playerDataCache, friendUUID);
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
                    CustomInventoryManager.setItemDisplayName(reminderTorch, Utils.getInstance().$("Reminders: " + newStatus));
                }
            }

            player.sendMessage(Utils.getInstance().$("Reminders have been turned " + newStatus + "."));
        }
    }

    /**
     * Navigates to the next or previous page in the Friends GUI.
     *
     * @param player    The player navigating the GUI.
     * @param direction The direction to navigate (-1 for previous, 1 for next).
     */
    private void navigatePage(Player player, int direction) {
        UUID playerUUID = player.getUniqueId();
        int currentPage = playerPages.getOrDefault(playerUUID, 1);
        int newPage = currentPage + direction;

        // Retrieve total pages
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        if (playerData == null) return;

        Set<String> friendsUUIDs = playerData.getFriends();
        int friendsPerPage = 28;
        int totalPages = (int) Math.ceil((double) friendsUUIDs.size() / friendsPerPage);
        newPage = Math.max(1, Math.min(newPage, totalPages));

        if (newPage == currentPage) {
            // Already at the boundary; do nothing or notify the player
            player.sendMessage(Utils.getInstance().$("No more pages in that direction."));
            return;
        }

        // Update the page tracking
        playerPages.put(playerUUID, newPage);

        // Reopen the Friends GUI with the new page
        FriendsGUI.openFriendsGUI(player, playerDataCache, false, newPage);
    }
}
