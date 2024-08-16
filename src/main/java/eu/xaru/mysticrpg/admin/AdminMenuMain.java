package eu.xaru.mysticrpg.admin;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.admin.players.PlayerStatsFeature;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminMenuMain {
    private final MysticCore plugin;
    private final Map<UUID, Player> playerEditMap = new HashMap<>();

    public AdminMenuMain(MysticCore plugin) {
        this.plugin = plugin;
        loadFeatures();
    }

    private void loadFeatures() {
        // Load all features here
        new PlayerStatsFeature(plugin);
        // Add other features as needed
    }

    public void openAdminMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "Admin Menu");

        // Add player head as an option
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = playerHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Players");
            playerHead.setItemMeta(meta);
        }
        inventory.setItem(0, playerHead);

        // Fill the rest with white glass panes
        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerItem.setItemMeta(fillerMeta);
        }
        for (int i = 1; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillerItem);
        }

        player.openInventory(inventory);
    }

    public void openMainMenu(Player player) {
        openAdminMenu(player);
    }

    public void openPlayerOptionsMenu(Player player, Player target) {
        Inventory inventory = Bukkit.createInventory(null, 54, "Player Options: " + target.getName());

        // Add options like ban and stats
        ItemStack banItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta banMeta = banItem.getItemMeta();
        if (banMeta != null) {
            banMeta.setDisplayName("[BAN]");
            banItem.setItemMeta(banMeta);
        }
        inventory.setItem(0, banItem);

        ItemStack statsItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("[STATS]");
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(1, statsItem);

        // Add back button
        ItemStack backItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("[BACK]");
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(8, backItem);

        // Fill the rest with white glass panes
        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerItem.setItemMeta(fillerMeta);
        }
        for (int i = 2; i < 8; i++) {
            inventory.setItem(i, fillerItem);
        }
        for (int i = 9; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillerItem);
        }

        player.openInventory(inventory);
        playerEditMap.put(player.getUniqueId(), target);
    }

    public boolean isPlayerHead(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD;
    }

    public Map<UUID, Player> getPlayerEditMap() {
        return playerEditMap;
    }

    public void openPlayerStatsMenu(Player player, Player target) {
        new PlayerStatsFeature(plugin).openPlayerStatsMenu(player, target);
    }
}
