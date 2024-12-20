package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonLobbiesGUI {

    private final DungeonManager dungeonManager;
    private final LobbyManager lobbyManager;
    private final DungeonConfig config;
    private final NamespacedKey lobbyIdKey;

    public DungeonLobbiesGUI(DungeonManager dungeonManager, DungeonConfig config) {
        this.dungeonManager = dungeonManager;
        this.lobbyManager = dungeonManager.getLobbyManager();
        this.config = config;
        this.lobbyIdKey = new NamespacedKey(dungeonManager.getPlugin(), "lobby_id");
    }

    public void open(Player player) {
        // Find all lobbies for this dungeon
        List<DungeonLobby> lobbies = lobbyManager.getActiveLobbiesForDungeon(config.getId());

        // Add a 'B' character for the back button in the structure at slot 18 (bottom-left)
        // 3 rows of 9: row 0: indices 0-8, row 1: 9-17, row 2: 18-26
        String[] structure = {
                "# # # # # # # # #",
                "# # # # # # # # #",
                "B # # # # # # # #"
        };

        Gui.Builder guiBuilder = Gui.normal()
                .setStructure(structure)
                .addIngredient('#', getFillerItem())
                .addIngredient('B', createBackButton(player));

        Gui gui = guiBuilder.build();

        if (lobbies.isEmpty()) {
            // No lobbies, show create button in bottom-right (slot 26)
            gui.setItem(26, createCreateLobbyItem(config.getId()));
        } else {
            // Show lobbies
            int index = 0;
            for (DungeonLobby lobby : lobbies) {
                if (index >= 27) break; // Limit to 27 slots
                gui.setItem(index, createLobbyItem(lobby));
                index++;
            }
            // Also show create lobby button if there's space (just in case)
            if (index < 27) {
                gui.setItem(26, createCreateLobbyItem(config.getId()));
            }
        }

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.BLUE + "Lobbies for " + config.getName())
                .build();

        window.open();
    }

    private Item createLobbyItem(DungeonLobby lobby) {
        String difficulty = lobby.getConfig().getDifficulty();
        // Attempt to get the creator as the first player in the lobby's list
        Player creator = lobby.getPlayers().isEmpty() ? null : lobby.getPlayers().get(0);
        String creatorName = (creator != null) ? creator.getName() : "Unknown";

        ChatColor diffColor = ChatColor.BLUE;
        String diff = difficulty.toLowerCase();
        switch (diff) {
            case "easy" -> diffColor = ChatColor.GREEN;
            case "normal" -> diffColor = ChatColor.BLUE;
            case "hard" -> diffColor = ChatColor.GOLD;
            case "deadly" -> diffColor = ChatColor.RED;
        }

        String displayName = diffColor + creatorName + "'s Lobby [" + difficulty + "]";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Players:");
        for (Player p : lobby.getPlayers()) {
            lore.add(ChatColor.GRAY + "- " + p.getName());
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && creator != null) {
            skullMeta.setOwningPlayer(creator);
            skullMeta.setDisplayName(displayName);
            skullMeta.setLore(lore);
            skullMeta.getPersistentDataContainer().set(lobbyIdKey, org.bukkit.persistence.PersistentDataType.STRING, lobby.getLobbyId());
            item.setItemMeta(skullMeta);
        } else {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(lobbyIdKey, org.bukkit.persistence.PersistentDataType.STRING, lobby.getLobbyId());
            item.setItemMeta(meta);
        }

        return new SimpleItem(item) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                // Join this lobby
                DungeonLobby targetLobby = lobbyManager.getLobby(lobby.getLobbyId());
                if (targetLobby != null) {
                    if (!targetLobby.isFull()) {
                        targetLobby.addPlayer(clickPlayer);
                        // No event.getView().close(), lobby.addPlayer() will update GUI automatically
                    } else {
                        clickPlayer.sendMessage(ChatColor.RED + "That lobby is full.");
                    }
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Lobby not found.");
                }
            }
        };
    }

    private Item createCreateLobbyItem(String dungeonId) {
        return new SimpleItem(new ItemBuilder(Material.LIME_WOOL)
                .setDisplayName(ChatColor.GREEN + "Create Lobby")
                .addLoreLines("",
                        ChatColor.GRAY + "Click to create a new lobby for this dungeon."))
        {
            @Override
            public void handleClick(org.bukkit.event.inventory.ClickType clickType, Player clickPlayer, org.bukkit.event.inventory.InventoryClickEvent event) {
                DungeonLobby lobby = lobbyManager.getOrCreateLobby(dungeonId, clickPlayer);
                // No need to close, lobby.addPlayer will open the lobby GUI
            }
        };
    }

    private Item createBackButton(Player player) {
        return new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.YELLOW + "Back")
                .addLoreLines("", ChatColor.GRAY + "Click to return to Dungeon Selection"))
        {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.getView().close();
                Bukkit.getScheduler().runTask(dungeonManager.getPlugin(), () -> {
                    new DungeonSelectionGUI(dungeonManager).open(clickPlayer);
                });
            }
        };
    }

    private Item getFillerItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
    }
}
