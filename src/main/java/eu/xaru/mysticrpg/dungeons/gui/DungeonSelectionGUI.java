// File: eu/xaru/mysticrpg/dungeons/gui/DungeonSelectionGUI.java

package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DungeonSelectionGUI implements Listener {

    private final DungeonManager dungeonManager;
    private final JavaPlugin plugin;

    public DungeonSelectionGUI(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.plugin = dungeonManager.getDungeonModule().getPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Select a Dungeon");

        int index = 0;
        for (DungeonConfig config : dungeonManager.getConfigManager().getAllConfigs()) {
            ItemStack icon = createDungeonIcon(config);
            gui.setItem(index++, icon);
        }

        player.openInventory(gui);
    }

    private ItemStack createDungeonIcon(DungeonConfig config) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + config.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Min Players: " + config.getMinPlayers());
        lore.add(ChatColor.GRAY + "Max Players: " + config.getMaxPlayers());
        lore.add(ChatColor.GRAY + "Difficulty: " + config.getDifficultyLevel());
        meta.setLore(lore);
        NamespacedKey key = new NamespacedKey(plugin, "dungeon_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, config.getId());
        item.setItemMeta(meta);
        return item;
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.BLUE + "Select a Dungeon")) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "dungeon_id");
        String dungeonId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (dungeonId == null) return;

        Player player = (Player) event.getWhoClicked();
        // Create a lobby and open the lobby GUI
        DungeonLobby lobby = dungeonManager.getLobbyManager().createLobby(dungeonId, player);
        player.closeInventory();
        DungeonLobbyGUI lobbyGUI = new DungeonLobbyGUI(dungeonManager.getLobbyManager());
        lobbyGUI.open(player, lobby);
    }
}
