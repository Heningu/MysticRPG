package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.Utils;
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
 * Utility class for creating and managing the Friend Requests GUI.
 */
public class FriendRequestsGUI {

    /**
     * Opens the Friend Requests GUI for the specified player.
     *
     * @param player          The player who will see the GUI.
     * @param playerDataCache The cache containing player data.
     * @param page            The current page number (for pagination).
     */
    public static void openFriendRequestsGUI(Player player, PlayerDataCache playerDataCache, int page) {
        // Create a double chest inventory with 54 slots and the title "Friend Requests"
        Inventory friendRequestsInventory = CustomInventoryManager.createInventory(54, "&aFriend Requests");

        // Define a black stained glass pane as the placeholder for border slots
        ItemStack placeholder = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill the border slots with the placeholder
        for (int slot = 0; slot < 54; slot++) {
            if (isBorderSlot(slot)) {
                CustomInventoryManager.addItemToSlot(friendRequestsInventory, slot, placeholder);
            }
        }

        // Add the back button (arrow) in slot 53
        ItemStack backButton = new ItemStack(Material.ARROW);
        CustomInventoryManager.setItemDisplayName(backButton, Utils.getInstance().$("Back"));
        CustomInventoryManager.addItemToSlot(friendRequestsInventory, 53, backButton);

        // Populate the middle slots with incoming friend requests
        populateFriendRequestHeads(player, playerDataCache, friendRequestsInventory, page);

        // Open the inventory for the player
        CustomInventoryManager.openInventory(player, friendRequestsInventory);
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
     * Populates the middle slots of the inventory with incoming friend request heads.
     *
     * @param player          The player viewing the GUI.
     * @param playerDataCache The cache containing player data.
     * @param inventory       The inventory to populate.
     * @param page            The current page number (for pagination).
     */
    private static void populateFriendRequestHeads(Player player, PlayerDataCache playerDataCache, Inventory inventory, int page) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("An error occurred while accessing your friend data."));
            return;
        }

        Set<String> incomingRequests = playerData.getFriendRequests();
        if (incomingRequests.isEmpty()) {
            // Add an item indicating no friend requests
            ItemStack noRequests = getDefaultHead(Utils.getInstance().$( "No Friend Requests"));
            // Place it in the center slot (e.g., slot 22)
            CustomInventoryManager.addItemToSlot(inventory, 22, noRequests);
            return;
        }

        // Convert Set to List for easier pagination
        List<String> requestsList = new ArrayList<>(incomingRequests);

        // Pagination setup
        int requestsPerPage = 28; // Number of friend request heads per page
        int totalPages = (int) Math.ceil((double) requestsList.size() / requestsPerPage);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int startIndex = (currentPage - 1) * requestsPerPage;
        int endIndex = Math.min(startIndex + requestsPerPage, requestsList.size());

        List<String> requestsToShow = requestsList.subList(startIndex, endIndex);

        // Define the slots available for friend request heads in the middle area (excluding back button)
        int[] middleSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < requestsToShow.size(); i++) {
            String senderUUIDString = requestsToShow.get(i);
            UUID senderUUID;
            try {
                senderUUID = UUID.fromString(senderUUIDString);
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
                continue;
            }

            OfflinePlayer sender = Bukkit.getOfflinePlayer(senderUUID);

            // Create a player head for the sender with lore hints
            ItemStack head = getPlayerHead(sender, senderUUID);

            // Assign the head to the appropriate slot
            if (i < middleSlots.length) {
                CustomInventoryManager.addItemToSlot(inventory, middleSlots[i], head);
            }
        }

        // Optionally, add pagination controls here (e.g., next page, previous page)
        // This example does not include pagination controls for simplicity
    }

    /**
     * Creates a player head for a sender with UUID in lore and adds lore hints for interactions.
     *
     * @param sender     The player who sent the friend request.
     * @param senderUUID The UUID of the sender.
     * @return The player head ItemStack.
     */
    private static ItemStack getPlayerHead(OfflinePlayer sender, UUID senderUUID) {
        Material headType = sender.isOnline() ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
        ItemStack head = new ItemStack(headType);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (sender.isOnline() && sender.getPlayer() != null) {
                meta.setOwningPlayer(sender.getPlayer());
                meta.setDisplayName(Utils.getInstance().$( sender.getName()));
            } else {
                meta.setDisplayName(Utils.getInstance().$( "[Offline] " + (sender.getName() != null ? sender.getName() : "Unknown")));
            }
            // Add lore: First line is UUID, followed by interaction hints
            List<String> lore = new ArrayList<>();
            lore.add(senderUUID.toString()); // For identification
            lore.add(Utils.getInstance().$("Left-click to accept"));
            lore.add(Utils.getInstance().$("Right-click to deny"));
            meta.setLore(lore);
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
            meta.setDisplayName(displayName);
            head.setItemMeta(meta);
        }
        return head;
    }
}
