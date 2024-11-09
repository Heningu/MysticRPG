//package eu.xaru.mysticrpg.social.friends;
//
//import eu.xaru.mysticrpg.social.party.PartyHelper;
//import eu.xaru.mysticrpg.storage.PlayerData;
//import eu.xaru.mysticrpg.storage.PlayerDataCache;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.OfflinePlayer;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.*;
//import org.bukkit.inventory.*;
//import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.inventory.meta.SkullMeta;
//
//import java.util.*;
//
///**
// * Handles the Friends GUI, including opening the GUI, handling clicks, and managing pagination.
// */
//public class FriendsGUI {
//
//    // Maps to track player-specific GUI states
//    private static final Map<UUID, Inventory> friendsInventories = new HashMap<>();
//    private static final Map<UUID, Integer> currentPageMap = new HashMap<>();
//    private static final Map<UUID, Boolean> showingRequests = new HashMap<>();
//    private static final Map<UUID, PlayerDataCache> playerDataCaches = new HashMap<>();
//    private static final Map<UUID, String> currentGUI = new HashMap<>(); // Tracks which GUI the player is viewing
//
//    // Number of friend entries per page
//    private static final int ITEMS_PER_PAGE = 28;
//
//    /**
//     * Opens the Friends GUI for the specified player.
//     *
//     * @param player          The player for whom the GUI is opened.
//     * @param playerDataCache The cache containing player data.
//     * @param showRequests    If true, shows pending friend requests; otherwise, shows the friends list.
//     * @param page            The page number to display.
//     */
//    public static void openFriendsGUI(Player player, PlayerDataCache playerDataCache, boolean showRequests, int page) {
//        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
//        if (playerData == null) {
//            player.sendMessage(ChatColor.RED + "An error occurred while accessing your data.");
//            return;
//        }
//
//        // Update GUI state maps
//        showingRequests.put(player.getUniqueId(), showRequests);
//        playerDataCaches.put(player.getUniqueId(), playerDataCache);
//        currentPageMap.put(player.getUniqueId(), page);
//
//        Set<String> displayList;
//        String title;
//
//        // Determine which list to display and set the inventory title accordingly
//        if (showRequests) {
//            displayList = playerData.getFriendRequests();
//            title = ChatColor.DARK_BLUE + "Pending Friend Requests";
//        } else {
//            displayList = playerData.getFriends();
//            title = ChatColor.DARK_BLUE + "Friends List";
//        }
//
//        List<String> uuids = new ArrayList<>(displayList);
//
//        // Calculate pagination details
//        int totalItems = uuids.size();
//        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
//        if (maxPage == 0) maxPage = 1;
//
//        // Adjust the current page if out of bounds
//        if (page > maxPage) page = maxPage;
//        if (page < 1) page = 1;
//        currentPageMap.put(player.getUniqueId(), page);
//
//        // Create a new inventory with 54 slots (double chest size)
//        Inventory gui = Bukkit.createInventory(null, 54, title);
//
//        // Fill non-content slots with placeholders
//        fillWithPlaceholders(gui);
//
//        // Add pagination buttons if multiple pages exist
//        if (maxPage > 1) {
//            ItemStack previousPage = createGuiItem(Material.ARROW, ChatColor.GREEN + "Previous Page");
//            ItemStack nextPage = createGuiItem(Material.ARROW, ChatColor.GREEN + "Next Page");
//            gui.setItem(45, previousPage);
//            gui.setItem(53, nextPage);
//        } else {
//            // Fill pagination slots with placeholders if only one page exists
//            ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
//            gui.setItem(45, placeholder);
//            gui.setItem(53, placeholder);
//        }
//
//        // Add Close button in the center of the bottom row
//        ItemStack closeButton = createGuiItem(Material.BARRIER, ChatColor.RED + "Close");
//        gui.setItem(49, closeButton);
//
//        // Add Toggle View button to switch between friends list and friend requests
//        ItemStack toggleViewButton = createGuiItem(Material.BOOK, showRequests ? ChatColor.AQUA + "View Friends List" : ChatColor.AQUA + "View Friend Requests");
//        gui.setItem(47, toggleViewButton);
//
//        // Add Reminders toggle button
//        ItemStack remindersButton = createGuiItem(Material.REDSTONE_TORCH, playerData.isRemindersEnabled() ? ChatColor.GREEN + "Reminders: ON" : ChatColor.RED + "Reminders: OFF");
//        gui.setItem(51, remindersButton);
//
//        // Determine the subset of friends/friend requests to display on the current page
//        int startIndex = (page - 1) * ITEMS_PER_PAGE;
//        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
//
//        List<String> itemsToDisplay = uuids.subList(startIndex, endIndex);
//
//        // Get the list of content slots where friend entries will be placed
//        List<Integer> slots = getContentSlots();
//
//        // Populate the GUI with friend entries
//        for (int i = 0; i < itemsToDisplay.size(); i++) {
//            String uuidString = itemsToDisplay.get(i);
//            UUID targetUUID = UUID.fromString(uuidString);
//            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
//
//            ItemStack headItem;
//            if (targetPlayer.isOnline()) {
//                headItem = getPlayerHead(targetPlayer.getPlayer());
//                ItemMeta meta = headItem.getItemMeta();
//                meta.setDisplayName(ChatColor.GREEN + targetPlayer.getName());
//                headItem.setItemMeta(meta);
//            } else {
//                headItem = new ItemStack(Material.SKELETON_SKULL);
//                ItemMeta meta = headItem.getItemMeta();
//                meta.setDisplayName(ChatColor.GRAY + "[Offline] " + targetPlayer.getName());
//                headItem.setItemMeta(meta);
//            }
//
//            // If displaying friend requests, add lore for accepting or denying
//            if (showRequests) {
//                ItemMeta meta = headItem.getItemMeta();
//                List<String> lore = new ArrayList<>();
//                lore.add(ChatColor.YELLOW + "Left click to accept");
//                lore.add(ChatColor.YELLOW + "Right click to deny");
//                meta.setLore(lore);
//                headItem.setItemMeta(meta);
//            }
//
//            // Place the head item in the corresponding slot
//            gui.setItem(slots.get(i), headItem);
//        }
//
//        // Track the current GUI state for the player
//        friendsInventories.put(player.getUniqueId(), gui);
//        currentGUI.put(player.getUniqueId(), "friends"); // Indicates the player is viewing the friends GUI
//
//        // Open the inventory for the player
//        player.openInventory(gui);
//    }
//
//    /**
//     * Creates a GUI item with the specified material and display name.
//     *
//     * @param material The material of the item.
//     * @param name     The display name of the item.
//     * @return The customized ItemStack.
//     */
//    private static ItemStack createGuiItem(Material material, String name) {
//        ItemStack item = new ItemStack(material);
//        ItemMeta meta = item.getItemMeta();
//        meta.setDisplayName(name);
//        item.setItemMeta(meta);
//        return item;
//    }
//
//    /**
//     * Retrieves the list of content slots where friend entries can be placed.
//     *
//     * @return A list of slot indices.
//     */
//    private static List<Integer> getContentSlots() {
//        List<Integer> slots = new ArrayList<>();
//        for (int i = 0; i < 54; i++) {
//            if (isContentSlot(i)) {
//                slots.add(i);
//            }
//        }
//        return slots;
//    }
//
//    /**
//     * Determines if a given slot is designated for content (friend entries).
//     *
//     * @param slot The slot index.
//     * @return True if the slot is for content; otherwise, false.
//     */
//    private static boolean isContentSlot(int slot) {
//        int row = slot / 9;
//        int col = slot % 9;
//
//        // Exclude the first and last rows
//        if (row == 0 || row == 5) return false;
//        // Exclude the first and last columns
//        if (col == 0 || col == 8) return false;
//        // Exclude specific slots reserved for buttons
//        if (slot == 45 || slot == 47 || slot == 49 || slot == 51 || slot == 53) return false;
//
//        return true;
//    }
//
//    /**
//     * Fills non-content slots of the inventory with placeholder items.
//     *
//     * @param inventory The inventory to fill with placeholders.
//     */
//    private static void fillWithPlaceholders(Inventory inventory) {
//        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
//        ItemMeta meta = placeholder.getItemMeta();
//        meta.setDisplayName(" ");
//        placeholder.setItemMeta(meta);
//
//        for (int i = 0; i < inventory.getSize(); i++) {
//            if (!isContentSlot(i)) {
//                inventory.setItem(i, placeholder);
//            }
//        }
//    }
//
//    /**
//     * Creates a player head ItemStack for the specified player.
//     *
//     * @param player The player whose head is to be created.
//     * @return The customized player head ItemStack.
//     */
//    private static ItemStack getPlayerHead(Player player) {
//        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
//        SkullMeta meta = (SkullMeta) head.getItemMeta();
//        meta.setOwningPlayer(player);
//        head.setItemMeta(meta);
//        return head;
//    }
//
//    /**
//     * Handles click events within the Friends GUI.
//     *
//     * @param event         The InventoryClickEvent triggered by the player.
//     * @param friendsHelper The helper class managing friend operations.
//     * @param partyHelper   The helper class managing party operations.
//     */
//    public static void handleInventoryClick(InventoryClickEvent event, FriendsHelper friendsHelper, PartyHelper partyHelper) {
//        if (!(event.getWhoClicked() instanceof Player)) return;
//        Player player = (Player) event.getWhoClicked();
//
//        // Retrieve the inventory associated with the player
//        if (!friendsInventories.containsKey(player.getUniqueId())) return;
//        Inventory inventory = friendsInventories.get(player.getUniqueId());
//        if (!event.getInventory().equals(inventory)) return;
//
//        ItemStack clickedItem = event.getCurrentItem();
//        if (clickedItem == null || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
//            event.setCancelled(true);
//            return;
//        }
//
//        // Determine which GUI the player is currently viewing
//        String guiType = currentGUI.get(player.getUniqueId());
//
//        if ("friends".equals(guiType)) {
//            handleFriendsGUIClick(event, friendsHelper, partyHelper);
//        } else if ("friend_options".equals(guiType)) {
//            handleFriendOptionsClick(event, friendsHelper, partyHelper);
//        }
//
//        // After handling the click, cancel the event to prevent item movement
//        event.setCancelled(true);
//    }
//
//    /**
//     * Handles click events specific to the main Friends GUI (Friends List or Pending Friend Requests).
//     *
//     * @param event         The InventoryClickEvent triggered by the player.
//     * @param friendsHelper The helper class managing friend operations.
//     * @param partyHelper   The helper class managing party operations.
//     */
//    private static void handleFriendsGUIClick(InventoryClickEvent event, FriendsHelper friendsHelper, PartyHelper partyHelper) {
//        Player player = (Player) event.getWhoClicked();
//        ItemStack clickedItem = event.getCurrentItem();
//        Boolean showRequests = showingRequests.getOrDefault(player.getUniqueId(), false);
//        PlayerDataCache playerDataCache = playerDataCaches.get(player.getUniqueId());
//        int currentPage = currentPageMap.getOrDefault(player.getUniqueId(), 1);
//
//        // Handle Pagination Buttons
//        if (clickedItem.getType() == Material.ARROW) {
//            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
//            int totalItems;
//            if (showRequests) {
//                totalItems = playerDataCache.getCachedPlayerData(player.getUniqueId()).getFriendRequests().size();
//            } else {
//                totalItems = playerDataCache.getCachedPlayerData(player.getUniqueId()).getFriends().size();
//            }
//            int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
//            if (displayName.equalsIgnoreCase("Previous Page")) {
//                if (currentPage > 1) {
//                    currentPage--;
//                    openFriendsGUI(player, playerDataCache, showRequests, currentPage);
//                }
//            } else if (displayName.equalsIgnoreCase("Next Page")) {
//                if (currentPage < maxPage) {
//                    currentPage++;
//                    openFriendsGUI(player, playerDataCache, showRequests, currentPage);
//                }
//            }
//        }
//        // Handle Close Button
//        else if (clickedItem.getType() == Material.BARRIER) {
//            player.closeInventory();
//        }
//        // Handle Toggle View Button
//        else if (clickedItem.getType() == Material.BOOK) {
//            openFriendsGUI(player, playerDataCache, !showRequests, 1);
//        }
//        // Handle Reminders Toggle Button
//        else if (clickedItem.getType() == Material.REDSTONE_TORCH) {
//            PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
//            playerData.setRemindersEnabled(!playerData.isRemindersEnabled());
//            player.sendMessage(ChatColor.YELLOW + "Reminders are now " + (playerData.isRemindersEnabled() ? "enabled." : "disabled."));
//            openFriendsGUI(player, playerDataCache, showRequests, currentPage);
//        }
//        // Handle Friend Entries
//        else {
//            int slot = event.getSlot();
//            if (isContentSlot(slot)) {
//                List<Integer> contentSlots = getContentSlots();
//                int index = contentSlots.indexOf(slot);
//                int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
//                int targetIndex = startIndex + index;
//
//                PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
//                List<String> displayList = new ArrayList<>();
//                if (showRequests) {
//                    displayList.addAll(playerData.getFriendRequests());
//                } else {
//                    displayList.addAll(playerData.getFriends());
//                }
//
//                if (targetIndex >= 0 && targetIndex < displayList.size()) {
//                    String targetUUIDString = displayList.get(targetIndex);
//                    UUID targetUUID = UUID.fromString(targetUUIDString);
//                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
//
//                    if (showRequests) {
//                        // Handle accepting or denying friend requests based on click type
//                        if (event.getClick() == ClickType.LEFT) {
//                            friendsHelper.acceptFriendRequest(player, targetPlayer.getName());
//                            openFriendsGUI(player, playerDataCache, true, currentPage);
//                        } else if (event.getClick() == ClickType.RIGHT) {
//                            friendsHelper.denyFriendRequest(player, targetPlayer.getName());
//                            openFriendsGUI(player, playerDataCache, true, currentPage);
//                        }
//                    } else {
//                        // Open the Friend Options GUI when clicking on a friend
//                        openFriendOptionsGUI(player, targetPlayer, partyHelper, friendsHelper);
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * Opens the Friend Options GUI for a specific friend, allowing actions like inviting to a party or removing the friend.
//     *
//     * @param player         The player who opened the GUI.
//     * @param targetPlayer   The friend player whose options are being displayed.
//     * @param partyHelper    The helper class managing party operations.
//     * @param friendsHelper  The helper class managing friend operations.
//     */
//    private static void openFriendOptionsGUI(Player player, OfflinePlayer targetPlayer, PartyHelper partyHelper, FriendsHelper friendsHelper) {
//        // Create a new inventory with 27 slots (triple chest size)
//        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + targetPlayer.getName());
//
//        // Fill non-content slots with placeholders
//        fillWithPlaceholders(gui);
//
//        // Add Invite to Party button
//        ItemStack cakeItem = createGuiItem(Material.CAKE, ChatColor.GREEN + "Invite to Party");
//        ItemMeta cakeMeta = cakeItem.getItemMeta();
//        cakeMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to invite " + targetPlayer.getName() + " to your party."));
//        cakeItem.setItemMeta(cakeMeta);
//        gui.setItem(11, cakeItem);
//
//        // Add Remove Friend button
//        ItemStack removeItem = createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Remove Friend");
//        ItemMeta removeMeta = removeItem.getItemMeta();
//        removeMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to remove " + targetPlayer.getName() + " from your friends list."));
//        removeItem.setItemMeta(removeMeta);
//        gui.setItem(15, removeItem);
//
//        // Add Back button to return to the main Friends GUI
//        ItemStack backButton = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back");
//        gui.setItem(22, backButton);
//
//        // Track the current GUI state for the player
//        friendsInventories.put(player.getUniqueId(), gui);
//        currentGUI.put(player.getUniqueId(), "friend_options"); // Indicates the player is viewing the friend options GUI
//
//        // Open the Friend Options GUI for the player
//        player.openInventory(gui);
//    }
//
//    /**
//     * Handles click events specific to the Friend Options GUI.
//     *
//     * @param event         The InventoryClickEvent triggered by the player.
//     * @param friendsHelper The helper class managing friend operations.
//     * @param partyHelper   The helper class managing party operations.
//     */
//    private static void handleFriendOptionsClick(InventoryClickEvent event, FriendsHelper friendsHelper, PartyHelper partyHelper) {
//        Player player = (Player) event.getWhoClicked();
//        ItemStack clickedItem = event.getCurrentItem();
//        String inventoryTitle = event.getView().getTitle();
//        String targetPlayerName = ChatColor.stripColor(inventoryTitle);
//
//        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
//
//        // Handle Invite to Party button
//        if (clickedItem.getType() == Material.CAKE) {
//            if (targetPlayer.isOnline()) {
//                Player onlineTarget = targetPlayer.getPlayer();
//                partyHelper.invitePlayer(player, onlineTarget);
//                player.closeInventory();
//            } else {
//                player.sendMessage(ChatColor.RED + "Player is not online.");
//            }
//        }
//        // Handle Remove Friend button
//        else if (clickedItem.getType() == Material.REDSTONE_BLOCK) {
//            if (targetPlayer.isOnline()) {
//                Player onlineTarget = targetPlayer.getPlayer();
//                friendsHelper.removeFriend(player, onlineTarget);
//            } else {
//                friendsHelper.removeFriend(player, targetPlayer);
//            }
//            // Return to the main Friends GUI after removing the friend
//            PlayerDataCache playerDataCache = playerDataCaches.get(player.getUniqueId());
//            openFriendsGUI(player, playerDataCache, false, 1);
//        }
//        // Handle Back button
//        else if (clickedItem.getType() == Material.ARROW) {
//            PlayerDataCache playerDataCache = playerDataCaches.get(player.getUniqueId());
//            Boolean showRequests = showingRequests.getOrDefault(player.getUniqueId(), false);
//            int currentPage = currentPageMap.getOrDefault(player.getUniqueId(), 1);
//            openFriendsGUI(player, playerDataCache, showRequests, currentPage);
//        }
//    }
//
//    /**
//     * Handles drag events within the Friends GUI by canceling them to prevent item movement.
//     *
//     * @param event The InventoryDragEvent triggered by the player.
//     */
//    public static void handleInventoryDrag(InventoryDragEvent event) {
//        Player player = (Player) event.getWhoClicked();
//
//        // Check if the player has an open Friends GUI
//        if (!friendsInventories.containsKey(player.getUniqueId())) return;
//
//        Inventory inventory = friendsInventories.get(player.getUniqueId());
//
//        // Check if the drag event is within the Friends GUI
//        if (event.getInventory().equals(inventory)) {
//            // Cancel the drag event to prevent item movement
//            event.setCancelled(true);
//        }
//    }
//
//    /**
//     * Handles the closing of the Friends GUI by cleaning up the tracking maps.
//     *
//     * @param event The InventoryCloseEvent triggered by the player.
//     */
//    public static void handleInventoryClose(InventoryCloseEvent event) {
//        Player player = (Player) event.getPlayer();
//        friendsInventories.remove(player.getUniqueId());
//        currentPageMap.remove(player.getUniqueId());
//        showingRequests.remove(player.getUniqueId());
//        playerDataCaches.remove(player.getUniqueId());
//        currentGUI.remove(player.getUniqueId());
//    }
//
//    /**
//     * Checks if the given inventory is a Friends GUI for the specified player.
//     *
//     * @param player    The player to check.
//     * @param inventory The inventory to verify.
//     * @return True if the inventory is a Friends GUI for the player; otherwise, false.
//     */
//    public static boolean isFriendsGUI(Player player, Inventory inventory) {
//        return friendsInventories.get(player.getUniqueId()) == inventory;
//    }
//}
