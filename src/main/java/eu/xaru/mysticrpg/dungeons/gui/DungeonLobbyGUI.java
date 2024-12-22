package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import org.bukkit.Bukkit;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class DungeonLobbyGUI {

    private final LobbyManager lobbyManager;
    private final NamespacedKey lobbyIdKey;

    // Player placeholders for up to 6 players
    private static final char[] HEAD_CHARS = {'P','T','Z','U','Y','G'};
    private static final char[] WOOL_CHARS = {'p','t','z','u','y','g'};

    // Base structure with all placeholders:
    private static final String[] BASE_STRUCTURE = {
            "# # # # # # # # S",
            "# P p # # # U u #",
            "# T t # # # Y y #",
            "# Z z # # # G g #",
            "# # # # # # # # #",
            "L # # # # # # # R"
    };

    public DungeonLobbyGUI(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
        this.lobbyIdKey = new NamespacedKey(lobbyManager.getDungeonManager().getPlugin(), "lobby_id");
    }

    public void open(Player player, DungeonLobby lobby) {
        String lobbyId = lobby.getLobbyId();
        boolean isPlayerReady = lobby.isReady(player.getUniqueId());

        int maxPlayers = lobby.getMaxPlayers();
        if (maxPlayers > 6) {
            maxPlayers = 6; // cap at 6
        }

        // Dynamically adjust the structure based on maxPlayers:
        String[] modifiedStructure = new String[BASE_STRUCTURE.length];
        for (int i = 0; i < BASE_STRUCTURE.length; i++) {
            String line = BASE_STRUCTURE[i];
            // For any player slot beyond maxPlayers, replace with '#'
            for (int slotIndex = maxPlayers; slotIndex < 6; slotIndex++) {
                line = line.replace(Character.toString(HEAD_CHARS[slotIndex]), "#");
                line = line.replace(Character.toString(WOOL_CHARS[slotIndex]), "#");
            }
            modifiedStructure[i] = line;
        }

        Gui.Builder guiBuilder = Gui.normal()
                .setStructure(modifiedStructure)
                .addIngredient('#', getFillerItem())
                .addIngredient('L', createLeaveItem(lobbyId))
                .addIngredient('R', createToggleReadyItem(lobbyId, isPlayerReady))
                .addIngredient('S', createStartItem(lobbyId, lobby));

        List<UUID> members = lobby.getPlayers().stream().map(Player::getUniqueId).collect(Collectors.toList());

        // Fill up to maxPlayers slots
        for (int i = 0; i < maxPlayers; i++) {
            setPlayerSlot(guiBuilder, lobby, members, i, HEAD_CHARS[i], WOOL_CHARS[i]);
        }

        Gui gui = guiBuilder.build();

        // Build the Window AND make it non-closeable
        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.GREEN + "Dungeon Lobby")
                .setCloseable(false) // <--- Make the GUI impossible to close via ESC
                .build();

        window.open();
    }

    private void setPlayerSlot(Gui.Builder guiBuilder, DungeonLobby lobby, List<UUID> members, int index,
                               char playerChar, char woolChar)
    {
        if (index < members.size()) {
            UUID memberUUID = members.get(index);
            Player member = lobbyManager.getDungeonManager().getPlugin().getServer().getPlayer(memberUUID);
            if (member == null) {
                guiBuilder.addIngredient(playerChar, createEmptyPlayerSlot());
                guiBuilder.addIngredient(woolChar, createEmptyWool(false));
            } else {
                guiBuilder.addIngredient(playerChar, createPlayerHeadItem(member));
                boolean memberReady = lobby.isReady(memberUUID);
                guiBuilder.addIngredient(woolChar, createReadyWool(memberReady));
            }
        } else {
            guiBuilder.addIngredient(playerChar, createEmptyPlayerSlot());
            guiBuilder.addIngredient(woolChar, createEmptyWool(false));
        }
    }

    private Item createPlayerHeadItem(Player lobbyPlayer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(lobbyPlayer);
            meta.setDisplayName(ChatColor.YELLOW + lobbyPlayer.getName());
            head.setItemMeta(meta);
        }
        return new SimpleItem(head) {};
    }

    private Item createEmptyPlayerSlot() {
        ItemStack head = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Empty Slot");
            head.setItemMeta(meta);
        }
        return new SimpleItem(head);
    }

    private Item createReadyWool(boolean ready) {
        Material woolMaterial = ready ? Material.GREEN_WOOL : Material.RED_WOOL;
        String state = ready ? "Ready" : "Not Ready";
        return new SimpleItem(new ItemBuilder(woolMaterial)
                .setDisplayName(ChatColor.YELLOW + state)
        );
    }

    private Item createEmptyWool(boolean ready) {
        // For empty slots, default to red wool
        return createReadyWool(false);
    }

    private Item createLeaveItem(String lobbyId) {
        return new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Leave Lobby")
                .addLoreLines("", ChatColor.GRAY + "Click to leave this lobby"))
        {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                DungeonLobby lobby = lobbyManager.getLobby(lobbyId);
                if (lobby != null) {
                    lobby.removePlayer(clickPlayer);
                }
                event.getView().close();
                // Delay reopening the dungeon selection GUI by 1 tick
                Bukkit.getScheduler().runTask(lobbyManager.getDungeonManager().getPlugin(), () -> {
                    new DungeonSelectionGUI(lobbyManager.getDungeonManager()).open(clickPlayer);
                });
            }
        };
    }

    private Item createToggleReadyItem(String lobbyId, boolean isPlayerReady) {
        Material wool = isPlayerReady ? Material.GREEN_WOOL : Material.RED_WOOL;
        String state = isPlayerReady ? "Unready" : "Ready";
        return new SimpleItem(
                new ItemBuilder(wool)
                        .setDisplayName(ChatColor.YELLOW + "Toggle " + state)
                        .addLoreLines("", ChatColor.GRAY + "Click to toggle your ready state.")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                DungeonLobby lobby = lobbyManager.getLobby(lobbyId);
                if (lobby != null) {
                    boolean currentlyReady = lobby.isReady(clickPlayer.getUniqueId());
                    lobby.setReady(clickPlayer.getUniqueId(), !currentlyReady);
                    event.getView().close();
                    // Reopen GUI next tick
                    Bukkit.getScheduler().runTask(lobbyManager.getDungeonManager().getPlugin(), () -> open(clickPlayer, lobby));
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Error: Lobby not found.");
                }
            }
        };
    }

    private Item createStartItem(String lobbyId, DungeonLobby lobby) {
        return new SimpleItem(new ItemBuilder(Material.JIGSAW)
                .setDisplayName(ChatColor.GREEN + "Start Dungeon")
                .addLoreLines("",
                        ChatColor.GRAY + "Click to start the dungeon if",
                        ChatColor.GRAY + "all players are ready."))
        {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                DungeonLobby lob = lobbyManager.getLobby(lobbyId);
                if (lob != null) {
                    // Check if all ready
                    if (lob.allPlayersReady()) {
                        lob.startDungeon();
                        event.getView().close();
                    } else {
                        clickPlayer.sendMessage(ChatColor.RED + "Not all players are ready.");
                    }
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Error: Lobby not found.");
                }
            }
        };
    }

    private Item getFillerItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
    }
}
