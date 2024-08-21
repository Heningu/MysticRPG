package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendsMenu {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;

    public FriendsMenu(MysticCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    public void openFriendsMenu(Player player) {
        openFriendsMenu(player, 0);
    }

    public void openFriendsMenu(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(player, 54, "Friends");
        // Fill border with white glass panes
        ItemStack glassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, glassPane);
            }
        }

        // Friend requests head
        ItemStack friendRequestsHead = createPlayerHead(player.getUniqueId(), ChatColor.YELLOW + "Friend Requests");
        inventory.setItem(0, friendRequestsHead);

        // Blocked players head
        ItemStack blockedPlayersHead = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta blockedPlayersMeta = blockedPlayersHead.getItemMeta();
        blockedPlayersMeta.setDisplayName(ChatColor.RED + "Blocked Players");
        blockedPlayersHead.setItemMeta(blockedPlayersMeta);
        inventory.setItem(1, blockedPlayersHead);

        // Block all incoming friend requests
        ItemStack fireCharge = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta fireChargeMeta = fireCharge.getItemMeta();
        fireChargeMeta.setDisplayName(ChatColor.RED + "Block All Incoming Friend Requests");
        if (playerDataManager.getPlayerData(player).isBlockingRequests()) {
            fireChargeMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            fireChargeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        fireCharge.setItemMeta(fireChargeMeta);
        inventory.setItem(2, fireCharge);

        // Navigation logic for friends
        PlayerData playerData = playerDataManager.getPlayerData(player);
        List<UUID> friends = new ArrayList<>(playerData.getFriends());
        int startIndex = page * 36;
        int endIndex = Math.min(startIndex + 36, friends.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID friendUUID = friends.get(i);
            Player friendPlayer = Bukkit.getPlayer(friendUUID);
            if (friendPlayer != null) {
                ItemStack friendHead = createPlayerHead(friendUUID, friendPlayer.getName());
                inventory.setItem(9 + (i - startIndex), friendHead);
            }
        }

        // Add navigation arrows and back button
        if (page > 0) {
            inventory.setItem(47, createCustomHead("60735-nether-traffic-light-left-arrow-on", "Previous Page"));
        } else {
            inventory.setItem(47, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Previous Page"));
        }

        if (endIndex < friends.size()) {
            inventory.setItem(51, createCustomHead("60737-nether-traffic-light-right-arrow-on", "Next Page"));
        } else {
            inventory.setItem(51, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Next Page"));
        }

        player.openInventory(inventory);
    }

    public void openFriendRequestsMenu(Player player) {
        openFriendRequestsMenu(player, 0);
    }

    public void openFriendRequestsMenu(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(player, 54, "Friend Requests");
        // Fill border with white glass panes
        ItemStack glassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, glassPane);
            }
        }

        PlayerData playerData = playerDataManager.getPlayerData(player);
        List<UUID> friendRequests = new ArrayList<>(playerData.getFriendRequests());

        int startIndex = page * 36;
        int endIndex = Math.min(startIndex + 36, friendRequests.size());
        for (int i = startIndex; i < endIndex; i++) {
            UUID requestUUID = friendRequests.get(i);
            Player requestPlayer = Bukkit.getPlayer(requestUUID);
            if (requestPlayer != null) {
                ItemStack requestHead = createPlayerHead(requestUUID, requestPlayer.getName());
                ItemMeta meta = requestHead.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "Left-click to accept");
                lore.add(ChatColor.RED + "Right-click to decline");
                meta.setLore(lore);
                requestHead.setItemMeta(meta);
                inventory.setItem(9 + (i - startIndex), requestHead);
            }
        }

        // Add navigation arrows and back button
        if (page > 0) {
            inventory.setItem(47, createCustomHead("60735-nether-traffic-light-left-arrow-on", "Previous Page"));
        } else {
            inventory.setItem(47, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Previous Page"));
        }

        if (endIndex < friendRequests.size()) {
            inventory.setItem(51, createCustomHead("60737-nether-traffic-light-right-arrow-on", "Next Page"));
        } else {
            inventory.setItem(51, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Next Page"));
        }

        inventory.setItem(49, createItemStack(Material.REDSTONE_BLOCK, ChatColor.RED + "Back to Friends Menu"));

        player.openInventory(inventory);
    }

    public void openBlockedPlayersMenu(Player player) {
        openBlockedPlayersMenu(player, 0);
    }

    public void openBlockedPlayersMenu(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(player, 54, "Blocked Players");
        // Fill border with white glass panes
        ItemStack glassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, glassPane);
            }
        }

        PlayerData playerData = playerDataManager.getPlayerData(player);
        List<UUID> blockedPlayers = new ArrayList<>(playerData.getBlockedPlayers());

        int startIndex = page * 36;
        int endIndex = Math.min(startIndex + 36, blockedPlayers.size());
        for (int i = startIndex; i < endIndex; i++) {
            UUID blockedUUID = blockedPlayers.get(i);
            Player blockedPlayer = Bukkit.getPlayer(blockedUUID);
            if (blockedPlayer != null) {
                ItemStack blockedHead = createPlayerHead(blockedUUID, blockedPlayer.getName());
                ItemMeta meta = blockedHead.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.RED + "Left-click to unblock");
                meta.setLore(lore);
                blockedHead.setItemMeta(meta);
                inventory.setItem(9 + (i - startIndex), blockedHead);
            }
        }

        // Add navigation arrows and back button
        if (page > 0) {
            inventory.setItem(47, createCustomHead("60735-nether-traffic-light-left-arrow-on", "Previous Page"));
        } else {
            inventory.setItem(47, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Previous Page"));
        }

        if (endIndex < blockedPlayers.size()) {
            inventory.setItem(51, createCustomHead("60737-nether-traffic-light-right-arrow-on", "Next Page"));
        } else {
            inventory.setItem(51, createItemStack(Material.WITHER_SKELETON_SKULL, ChatColor.GRAY + "No Next Page"));
        }

        inventory.setItem(49, createItemStack(Material.REDSTONE_BLOCK, ChatColor.RED + "Back to Friends Menu"));

        player.openInventory(inventory);
    }

    public void handleFriendRequestClick(Player player, ItemStack clickedItem, ClickType clickType) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        UUID requestUUID = Bukkit.getOfflinePlayer(displayName).getUniqueId();

        if (clickType == ClickType.LEFT) {
            // Accept friend request
            playerData.addFriend(requestUUID);
            playerData.removeFriendRequest(requestUUID);
            player.sendMessage(ChatColor.GREEN + "You have accepted the friend request from " + displayName);
        } else if (clickType == ClickType.RIGHT) {
            // Decline friend request
            playerData.removeFriendRequest(requestUUID);
            player.sendMessage(ChatColor.RED + "You have declined the friend request from " + displayName);
        }

        playerDataManager.save(player);
    }

    public void handleBlockedPlayerClick(Player player, ItemStack clickedItem) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        UUID blockedUUID = Bukkit.getOfflinePlayer(displayName).getUniqueId();

        playerData.unblockPlayer(blockedUUID);
        player.sendMessage(ChatColor.GREEN + "You have unblocked " + displayName);

        playerDataManager.save(player);
    }

    public void toggleBlockingRequests(Player player) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        boolean isBlocking = playerData.isBlockingRequests();
        playerData.setBlockingRequests(!isBlocking);
        player.sendMessage(ChatColor.YELLOW + "Block all incoming friend requests: " + (!isBlocking));
        playerDataManager.save(player);
    }

    private ItemStack createPlayerHead(UUID uuid, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        meta.setDisplayName(displayName);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createCustomHead(String textureId, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(displayName);
        // Apply custom texture here using textureId
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItemStack(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }
}
