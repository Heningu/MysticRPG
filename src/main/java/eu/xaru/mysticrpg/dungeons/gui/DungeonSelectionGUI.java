package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class DungeonSelectionGUI {

    private final DungeonManager dungeonManager;
    private final NamespacedKey dungeonIdKey;

    public DungeonSelectionGUI(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.dungeonIdKey = new NamespacedKey(dungeonManager.getPlugin(), "dungeon_id");
    }

    public void open(Player player) {
        List<DungeonConfig> configs = new ArrayList<>(dungeonManager.getConfigManager().getAllConfigs());

        // We'll create a 27-slot (3 rows of 9) GUI
        String[] structure = {
                "#########",
                "#########",
                "#########"
        };

        Gui gui = Gui.normal()
                .setStructure(structure)
                .addIngredient('#', getFillerItem())
                .build();

        // Place dungeon items
        for (int i = 0; i < configs.size() && i < 27; i++) {
            gui.setItem(i, createDungeonItem(configs.get(i)));
        }

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.BLUE + "Select a Dungeon")
                .build();

        window.open();
    }

    private Item createDungeonItem(DungeonConfig config) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Min Players: " + config.getMinPlayers());
        lore.add(ChatColor.GRAY + "Max Players: " + config.getMaxPlayers());
        lore.add(ChatColor.GRAY + "Difficulty: " + config.getDifficulty());

        ItemStack base = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + config.getName());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(dungeonIdKey, PersistentDataType.STRING, config.getId());
            base.setItemMeta(meta);
        }

        return new SimpleItem(base) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                // Create or get the lobby
                DungeonLobby lobby = dungeonManager.getLobbyManager().getOrCreateLobby(config.getId(), clickPlayer);
                event.getView().close();

                // Now open the DungeonLobbyGUI for the player and this lobby
                dungeonManager.getLobbyGUI().open(clickPlayer, lobby);
            }
        };
    }

    private Item getFillerItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
    }
}