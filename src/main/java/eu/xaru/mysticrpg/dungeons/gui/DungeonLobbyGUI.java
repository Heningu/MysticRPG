package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

public class DungeonLobbyGUI {

    private final LobbyManager lobbyManager;
    private final NamespacedKey lobbyIdKey;

    public DungeonLobbyGUI(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
        this.lobbyIdKey = new NamespacedKey(lobbyManager.getDungeonManager().getPlugin(), "lobby_id");
    }

    public void open(Player player, DungeonLobby lobby) {
        int maxPlayers = lobby.getMaxPlayers();
        String[] structure = {
                "#########",
                "#########",
                "#########"
        };

        Gui gui = Gui.normal()
                .setStructure(structure)
                .addIngredient('#', getFillerItem())
                .build();

        List<Player> lobbyPlayers = lobby.getPlayers();
        int index = 0;
        for (Player lobbyPlayer : lobbyPlayers) {
            gui.setItem(index++, createPlayerHeadItem(lobbyPlayer));
        }

        while (index < maxPlayers && index < 27) {
            gui.setItem(index++, createEmptySlotItem());
        }

        // Place the start item at slot 26 if possible
        if (26 < 27) {
            gui.setItem(26, createStartItem(lobby.getLobbyId()));
        }

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.GREEN + "Dungeon Lobby")
                .build();

        window.open();
    }

    private Item createPlayerHeadItem(Player lobbyPlayer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(lobbyPlayer);
            meta.setDisplayName(ChatColor.YELLOW + lobbyPlayer.getName());
            head.setItemMeta(meta);
        }

        return new SimpleItem(head) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                // No specific action on player head click in this GUI
            }
        };
    }

    private Item createEmptySlotItem() {
        ItemStack head = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Empty Slot");
            head.setItemMeta(meta);
        }
        return new SimpleItem(head);
    }

    private Item createStartItem(String lobbyId) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Start Dungeon");
            meta.getPersistentDataContainer().set(lobbyIdKey, PersistentDataType.STRING, lobbyId);
            item.setItemMeta(meta);
        }

        return new SimpleItem(item) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                String id = getLobbyIdFromItem(event.getCurrentItem());
                if (id == null) {
                    clickPlayer.sendMessage(ChatColor.RED + "Error: Lobby ID not found.");
                    return;
                }

                DungeonLobby lobby = lobbyManager.getLobby(id);
                if (lobby != null) {
                    lobby.playerReady(clickPlayer);
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Error: Lobby not found.");
                }
            }
        };
    }

    private String getLobbyIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(lobbyIdKey, PersistentDataType.STRING);
    }

    private Item getFillerItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
    }
}