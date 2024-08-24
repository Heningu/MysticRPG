package eu.xaru.mysticrpg.social.friends;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendsInventory {

    private final PlayerDataCache playerDataCache;

    public FriendsInventory(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
    }

    public void openFriendsMenu(Player player) {
        openFriendsMenu(player, 0);
    }

    public void openFriendsMenu(Player player, int page) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
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

        if (playerData.blockingRequests) {
            fireChargeMeta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            fireChargeMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        fireCharge.setItemMeta(fireChargeMeta);
        inventory.setItem(2, fireCharge);

        // Navigation logic for friends
        List<UUID> friends = playerData.friends.stream()
                .map(UUID::fromString)  // Convert each String to a UUID
                .collect(Collectors.toList());

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
