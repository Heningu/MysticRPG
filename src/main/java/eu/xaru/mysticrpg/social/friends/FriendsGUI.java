package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
            // Optionally, add an item indicating no friends
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
            ItemStack head = getOfflinePlayerHead(friend);

            // Assign the head to the appropriate slot
            if (i < middleSlots.length) {
                CustomInventoryManager.addItemToSlot(inventory, middleSlots[i], head);
            }
        }

        // Optionally, add pagination controls here (e.g., next page, previous page)
    }

    /**
     * Creates a player head for an online player using the provided method.
     *
     * @param player The online player.
     * @return The player head ItemStack.
     */
    private static ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.YELLOW + player.getName());
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Creates a player head for an offline player.
     *
     * @param offlinePlayer The offline player.
     * @return The player head ItemStack.
     */
    private static ItemStack getOfflinePlayerHead(OfflinePlayer offlinePlayer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ChatColor.YELLOW + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));
            head.setItemMeta(meta);
        }
        return head;
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
            // Optionally, set a default owning player or leave it empty
            meta.setDisplayName(displayName);
            head.setItemMeta(meta);
        }
        return head;
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

        // Optional: Add pagination controls if multiple pages exist
        /*
        if (totalPages > 1) {
            // Previous Page Button (e.g., Slot 45)
            if (currentPage > 1) {
                ItemStack prevPage = new ItemStack(Material.ARROW);
                CustomInventoryManager.setItemDisplayName(prevPage, ChatColor.GREEN + "Previous Page");
                inventory.setItem(45, prevPage);
            }

            // Next Page Button (e.g., Slot 53)
            if (currentPage < totalPages) {
                ItemStack nextPage = new ItemStack(Material.ARROW);
                CustomInventoryManager.setItemDisplayName(nextPage, ChatColor.GREEN + "Next Page");
                inventory.setItem(53, nextPage);
            }
        }
        */
    }
}
