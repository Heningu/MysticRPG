// File: eu/xaru/mysticrpg/dungeons/gui/DungeonLobbyGUI.java

package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonLobbyGUI implements Listener {

    private final LobbyManager lobbyManager;
    private final JavaPlugin plugin;

    public DungeonLobbyGUI(LobbyManager lobbyManager, JavaPlugin plugin) {
        this.lobbyManager = lobbyManager;
        this.plugin = plugin;
        // Event handler registration is now handled in DungeonManager
    }

    public void open(Player player, DungeonLobby lobby) {
        int maxPlayers = lobby.getMaxPlayers();
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Dungeon Lobby");

        // Add player heads and empty slots
        int index = 0;
        for (Player lobbyPlayer : lobby.getPlayers()) {
            gui.setItem(index++, createPlayerHead(lobbyPlayer));
        }

        // Add empty skeleton heads for free slots
        while (index < maxPlayers) {
            gui.setItem(index++, createEmptySlot());
        }

        // Add start item
        gui.setItem(26, createStartItem(lobby.getLobbyId()));

        // Ensure running on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.openInventory(gui);
        });
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.YELLOW + player.getName());
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createEmptySlot() {
        ItemStack head = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Empty Slot");
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createStartItem(String lobbyId) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Start Dungeon");
        NamespacedKey key = new NamespacedKey(plugin, "lobby_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, lobbyId);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GREEN + "Dungeon Lobby")) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        Player player = (Player) event.getWhoClicked();

        if (clickedItem.getType() == Material.GREEN_WOOL) {
            // Start the dungeon
            NamespacedKey key = new NamespacedKey(plugin, "lobby_id");
            String lobbyId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

            if (lobbyId == null) {
                player.sendMessage(ChatColor.RED + "Error: Lobby ID not found.");
                return;
            }

            DungeonLobby lobby = lobbyManager.getLobby(lobbyId);

            if (lobby != null) {
                lobby.playerReady(player);
            } else {
                player.sendMessage(ChatColor.RED + "Error: Lobby not found.");
            }
        }
    }
}
