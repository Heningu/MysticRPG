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
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonLobbyGUI {

    private final LobbyManager lobbyManager;
    private final NamespacedKey lobbyIdKey;

    /**
     * If a player's UUID is in this set, the next forced close is ignored (i.e. we don't remove them from the lobby).
     */
    private final Set<UUID> ignoreCloses = new HashSet<>();

    private final Map<UUID, Window> openWindows = new HashMap<>();
    private final Map<UUID, Gui> openGuis = new HashMap<>();

    private static final char[] HEAD_CHARS = {'P','T','Z','U','Y','G'};
    private static final char[] WOOL_CHARS = {'p','t','z','u','y','g'};
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

    /**
     * Called by DungeonLobby.startDungeon() to ignore the forced close for the given player.
     */
    public void ignoreNextClose(UUID playerUuid) {
        ignoreCloses.add(playerUuid);
    }

    private void handleClose(Window window, Player player, DungeonLobby lobby) {
        openWindows.remove(player.getUniqueId());
        openGuis.remove(player.getUniqueId());

        // If they are in ignoreCloses => skip
        if (!ignoreCloses.remove(player.getUniqueId())) {
            // Not ignoring => remove from lobby
            if (lobby.getPlayers().contains(player)) {
                lobby.removePlayer(player);
            }
        }
    }

    public boolean hasOpenWindow(UUID uuid) {
        return openWindows.containsKey(uuid);
    }

    // -----------------------------------
    // OPEN & REFRESH
    // -----------------------------------

    public void open(Player player, DungeonLobby lobby) {
        if (openWindows.containsKey(player.getUniqueId())) {
            return;
        }

        Gui.Builder.Normal builder = Gui.normal();
        builder.setStructure(BASE_STRUCTURE);
        Gui gui = builder.build();

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.GREEN + "Dungeon Lobby")
                .setCloseable(true)
                .build();

        openWindows.put(player.getUniqueId(), window);
        openGuis.put(player.getUniqueId(), gui);

        window.addCloseHandler(() -> handleClose(window, player, lobby));
        window.open();

        refreshGuiContents(gui, player, lobby);
    }

    public void refresh(Player player, DungeonLobby lobby) {
        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) {
            open(player, lobby);
            return;
        }
        clearGui(gui);
        refreshGuiContents(gui, player, lobby);
    }

    private void clearGui(Gui gui) {
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            gui.remove(i);
        }
    }

    private void refreshGuiContents(Gui gui, Player viewer, DungeonLobby lobby) {
        int maxPlayers = Math.min(lobby.getMaxPlayers(), 6);

        String[] structCopy = Arrays.copyOf(BASE_STRUCTURE, BASE_STRUCTURE.length);
        for (int i = 0; i < structCopy.length; i++) {
            String line = structCopy[i];
            for (int slotIndex = maxPlayers; slotIndex < 6; slotIndex++) {
                line = line.replace(Character.toString(HEAD_CHARS[slotIndex]), "#");
                line = line.replace(Character.toString(WOOL_CHARS[slotIndex]), "#");
            }
            structCopy[i] = line;
        }

        List<Character> structChars = new ArrayList<>();
        for (String row : structCopy) {
            String noSpaces = row.replace(" ", "");
            for (char c : noSpaces.toCharArray()) {
                structChars.add(c);
            }
        }

        boolean isViewerReady = lobby.isReady(viewer.getUniqueId());
        List<UUID> members = lobby.getPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toList());

        for (int slotIndex = 0; slotIndex < structChars.size(); slotIndex++) {
            char c = structChars.get(slotIndex);

            switch (c) {
                case 'L':
                    gui.setItem(slotIndex, createLeaveItem(lobby.getLobbyId()));
                    break;
                case 'R':
                    gui.setItem(slotIndex, createToggleReadyItem(lobby.getLobbyId(), isViewerReady));
                    break;
                case 'S':
                    gui.setItem(slotIndex, createStartItem(lobby.getLobbyId(), lobby));
                    break;
                case '#':
                    gui.setItem(slotIndex, getFillerItem());
                    break;
                case 'P':
                case 'T':
                case 'Z':
                case 'U':
                case 'Y':
                case 'G': {
                    int idx = findIndexInArray(c, HEAD_CHARS);
                    if (idx >= 0) {
                        placePlayerHead(gui, slotIndex, members, idx, lobby);
                    } else {
                        gui.setItem(slotIndex, getFillerItem());
                    }
                    break;
                }
                case 'p':
                case 't':
                case 'z':
                case 'u':
                case 'y':
                case 'g': {
                    int idx = findIndexInArray(c, WOOL_CHARS);
                    if (idx >= 0) {
                        placePlayerWool(gui, slotIndex, members, idx, lobby);
                    } else {
                        gui.setItem(slotIndex, getFillerItem());
                    }
                    break;
                }
                default:
                    gui.setItem(slotIndex, getFillerItem());
                    break;
            }
        }
    }

    private int findIndexInArray(char c, char[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == c) return i;
        }
        return -1;
    }

    private void placePlayerHead(Gui gui, int slotIndex, List<UUID> members, int idx, DungeonLobby lobby) {
        if (idx < members.size()) {
            UUID memberUUID = members.get(idx);
            Player member = lobbyManager.getDungeonManager()
                    .getPlugin().getServer().getPlayer(memberUUID);

            if (member == null) {
                gui.setItem(slotIndex, createEmptyPlayerSlot());
            } else {
                gui.setItem(slotIndex, createPlayerHeadItem(member));
            }
        } else {
            gui.setItem(slotIndex, createEmptyPlayerSlot());
        }
    }

    private void placePlayerWool(Gui gui, int slotIndex, List<UUID> members, int idx, DungeonLobby lobby) {
        if (idx < members.size()) {
            UUID memberUUID = members.get(idx);
            boolean isReady = lobby.isReady(memberUUID);
            gui.setItem(slotIndex, createReadyWool(isReady));
        } else {
            gui.setItem(slotIndex, createEmptyWool(false));
        }
    }

    private Item createPlayerHeadItem(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.YELLOW + player.getName());
            head.setItemMeta(meta);
        }
        return new SimpleItem(head);
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
        return createReadyWool(false);
    }

    private Item getFillerItem() {
        return new SimpleItem(
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("")
        );
    }

    private Item createLeaveItem(String lobbyId) {
        return new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Leave Lobby")
                .addLoreLines("",
                        ChatColor.GRAY + "Click to leave this lobby"))
        {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                DungeonLobby lobby = lobbyManager.getLobby(lobbyId);
                if (lobby != null) {
                    lobby.removePlayer(clickPlayer);
                }
                event.getView().close();
                Bukkit.getScheduler().runTask(
                        lobbyManager.getDungeonManager().getPlugin(),
                        () -> new DungeonSelectionGUI(lobbyManager.getDungeonManager())
                                .open(clickPlayer)
                );
            }
        };
    }

    /**
     * Toggling "Ready" simply updates readiness and refreshes the GUI.
     */
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
                DungeonLobby lob = lobbyManager.getLobby(lobbyId);
                if (lob != null) {
                    boolean currentlyReady = lob.isReady(clickPlayer.getUniqueId());
                    lob.setReady(clickPlayer.getUniqueId(), !currentlyReady);
                    // This updates all members' GUIs
                    lob.updateLobbyGUI();
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
                    if (lob.allPlayersReady()) {
                        // Step 1: ignore next close for each player
                        for (Player p : lob.getPlayers()) {
                            ignoreNextClose(p.getUniqueId());
                        }
                        // Step 2: forcibly close everyone's GUI => won't remove them
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
}
