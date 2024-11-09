package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Utility class for creating and managing the Friends GUI.
 */
public class FriendsGUI {

    /**
     * Opens the Friends GUI for the specified player.
     *
     * @param player          The player who will see the GUI.
     * @param playerDataCache The cache containing player data.
     * @param someFlag        Placeholder for additional flags (e.g., pagination).
     * @param page            The current page number (for pagination).
     */
    public static void openFriendsGUI(Player player, PlayerDataCache playerDataCache, boolean someFlag, int page) {
        // Create a double chest inventory with 54 slots and the title "Friends"
        Inventory friendsInventory = CustomInventoryManager.createInventory(54, "&aFriends");

        // Define a black stained glass pane as the placeholder for border slots
        ItemStack placeholder = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill the border slots with the placeholder
        for (int slot = 0; slot < 54; slot++) {
            if (isBorderSlot(slot)) {
                CustomInventoryManager.addItemToSlot(friendsInventory, slot, placeholder);
            }
        }

        // Populate the middle slots with friends' player heads
        populateFriendHeads(player, playerDataCache, friendsInventory, page);

        // Add interactive items to the last row
        addLastRowItems(player, playerDataCache, friendsInventory, page);

        // Open the inventory for the player
        CustomInventoryManager.openInventory(player, friendsInventory);
    }

    /**
     * Determines whether a given slot is part of the border.
     *
     * @param slot The slot number (0-53).
     * @return True if the slot is a border slot, false otherwise.
     */
    private static boolean isBorderSlot(int slot) {
        // Top and bottom rows
        if (slot >= 0 && slot <= 8) return true;
        if (slot >= 45 && slot <= 53) return true;

        // First and last columns of the middle rows
        if (slot % 9 == 0 || slot % 9 == 8) return true;

        return false;
    }

    /**
     * Populates the middle slots of the inventory with the player's friends' heads.
     *
     * @param player          The player viewing the GUI.
     * @param playerDataCache The cache containing player data.
     * @param inventory       The inventory to populate.
     * @param page            The current page number (for pagination).
     */
    private static void populateFriendHeads(Player player, PlayerDataCache playerDataCache, Inventory inventory, int page) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "An error occurred while accessing your friend data.");
            return;
        }

        Set<String> friendsUUIDs = playerData.getFriends();
        if (friendsUUIDs.isEmpty()) {
            // Add an item indicating no friends
            ItemStack noFriends = getDefaultHead(ChatColor.GRAY + "No Friends");
            // Place it in the center slot (e.g., slot 22)
            CustomInventoryManager.addItemToSlot(inventory, 22, noFriends);
            return;
        }

        // Convert Set to List for easier pagination
        List<String> friendsList = new ArrayList<>(friendsUUIDs);

        // Pagination setup (optional)
        int friendsPerPage = 28; // Number of friend heads per page (slots 10-16, 19-25, 28-34, 37-43)
        int totalPages = (int) Math.ceil((double) friendsList.size() / friendsPerPage);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int startIndex = (currentPage - 1) * friendsPerPage;
        int endIndex = Math.min(startIndex + friendsPerPage, friendsList.size());

        List<String> friendsToShow = friendsList.subList(startIndex, endIndex);

        // Define the slots available for friend heads in the middle area
        int[] middleSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < friendsToShow.size(); i++) {
            String friendUUIDString = friendsToShow.get(i);
            UUID friendUUID;
            try {
                friendUUID = UUID.fromString(friendUUIDString);
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
                continue;
            }

            OfflinePlayer friend = player.getServer().getOfflinePlayer(friendUUID);

            // Create a player head for the friend
            ItemStack head;
            if (friend.isOnline() && friend.getPlayer() != null) {
                head = getPlayerHead(friend.getPlayer(), friendUUID);
            } else {
                head = getOfflineSkeletonHead(friend, friendUUID);
            }

            // Assign the head to the appropriate slot
            if (i < middleSlots.length) {
                CustomInventoryManager.addItemToSlot(inventory, middleSlots[i], head);
            }
        }

        // Optionally, add pagination controls here (e.g., next page, previous page)
    }

    /**
     * Creates a player head for an online player with UUID in lore.
     *
     * @param player     The online player.
     * @param friendUUID The UUID of the friend.
     * @return The player head ItemStack.
     */
    private static ItemStack getPlayerHead(Player player, UUID friendUUID) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.YELLOW + player.getName());
            meta.setLore(Collections.singletonList(friendUUID.toString())); // Embed UUID in lore
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Creates a skeleton head for an offline player with "[Offline]" prefix and UUID in lore.
     *
     * @param offlinePlayer The offline player.
     * @param friendUUID    The UUID of the friend.
     * @return The skeleton head ItemStack.
     */
    private static ItemStack getOfflineSkeletonHead(OfflinePlayer offlinePlayer, UUID friendUUID) {
        ItemStack skeletonHead = new ItemStack(Material.SKELETON_SKULL);
        SkullMeta meta = (SkullMeta) skeletonHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "[Offline] " + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));
            meta.setLore(Collections.singletonList(friendUUID.toString())); // Embed UUID in lore
            skeletonHead.setItemMeta(meta);
        }
        return skeletonHead;
    }

    /**
     * Creates a default head with a specified display name.
     *
     * @param displayName The display name for the default head.
     * @return The default head ItemStack.
     */
    private static ItemStack getDefaultHead(String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Opens the Friend Options GUI for the specified player and friend.
     *
     * @param player          The player who will see the GUI.
     * @param cache           The cache containing player data.
     * @param friendUUID      The UUID of the friend.
     */
    public static void openFriendOptionsGUI(Player player, PlayerDataCache cache, UUID friendUUID) {
        // Retrieve the friend's name
        OfflinePlayer friend = Bukkit.getOfflinePlayer(friendUUID);
        String friendName = (friend.getName() != null) ? friend.getName() : "Unknown";

        // Create an inventory with 9 slots (single row) titled "FRIENDNAME"
        String inventoryTitle = ChatColor.translateAlternateColorCodes('&', friendName);
        Inventory friendOptionsInventory = CustomInventoryManager.createInventory(9, inventoryTitle);

        // Define placeholder for empty slots
        ItemStack placeholder = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill all slots with placeholders first
        for (int slot = 0; slot < 9; slot++) {
            CustomInventoryManager.addItemToSlot(friendOptionsInventory, slot, placeholder);
        }

        // Add the friend's head to slot 0
        ItemStack friendHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) friendHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(friend);
            meta.setDisplayName(ChatColor.YELLOW + friendName);
            meta.setLore(Collections.singletonList(friendUUID.toString())); // Embed UUID in lore
            friendHead.setItemMeta(meta);
        }
        CustomInventoryManager.addItemToSlot(friendOptionsInventory, 0, friendHead);

        // Define the buttons
        // Slot 2: Invite to Party (Cake)
        ItemStack inviteButton = new ItemStack(Material.CAKE);
        CustomInventoryManager.setItemDisplayName(inviteButton, ChatColor.GREEN + "Invite to Party");
        CustomInventoryManager.addItemToSlot(friendOptionsInventory, 2, inviteButton);

        // Slot 4: Send a Message (Paper)
        ItemStack messageButton = new ItemStack(Material.PAPER);
        CustomInventoryManager.setItemDisplayName(messageButton, ChatColor.BLUE + "Send a Message");
        CustomInventoryManager.addItemToSlot(friendOptionsInventory, 4, messageButton);

        // Slot 6: Remove Friend (Redstone Block)
        ItemStack removeButton = new ItemStack(Material.REDSTONE_BLOCK);
        CustomInventoryManager.setItemDisplayName(removeButton, ChatColor.RED + "Remove Friend");
        CustomInventoryManager.addItemToSlot(friendOptionsInventory, 6, removeButton);

        // Slot 8: Back (Arrow)
        ItemStack backButton = new ItemStack(Material.ARROW);
        CustomInventoryManager.setItemDisplayName(backButton, ChatColor.GOLD + "Back");
        CustomInventoryManager.addItemToSlot(friendOptionsInventory, 8, backButton);

        // Open the inventory for the player
        CustomInventoryManager.openInventory(player, friendOptionsInventory);
    }

    /**
     * Adds the interactive items to the last row of the inventory.
     *
     * @param player          The player viewing the GUI.
     * @param playerDataCache The cache containing player data.
     * @param inventory       The inventory to modify.
     * @param currentPage     The current page number.
     */
    private static void addLastRowItems(Player player, PlayerDataCache playerDataCache, Inventory inventory, int currentPage) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());

        // Slot 47 (3rd slot): Book named "Friend Requests"
        ItemStack friendRequestsBook = new ItemStack(Material.BOOK);
        CustomInventoryManager.setItemDisplayName(friendRequestsBook, ChatColor.AQUA + "Friend Requests");
        inventory.setItem(47, friendRequestsBook);

        // Slot 49 (5th slot): Barrier named "Close"
        ItemStack closeBarrier = new ItemStack(Material.BARRIER);
        CustomInventoryManager.setItemDisplayName(closeBarrier, ChatColor.RED + "Close");
        inventory.setItem(49, closeBarrier);

        // Slot 51 (7th slot): Redstone torch named "Reminders: ON | OFF"
        ItemStack reminderTorch = new ItemStack(Material.REDSTONE_TORCH);
        boolean remindersEnabled = playerData != null && playerData.isRemindersEnabled();
        String reminderStatus = remindersEnabled ? "ON" : "OFF";
        CustomInventoryManager.setItemDisplayName(reminderTorch, ChatColor.GOLD + "Reminders: " + reminderStatus);
        inventory.setItem(51, reminderTorch);
    }
}
