package eu.xaru.mysticrpg.dungeons.gui;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
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

import static org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
import static org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES;

public class DungeonSelectionGUI {

    private final DungeonManager dungeonManager;
    private final NamespacedKey dungeonIdKey;
    private final PlayerDataCache playerDataCache;

    public DungeonSelectionGUI(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.dungeonIdKey = new NamespacedKey(dungeonManager.getPlugin(), "dungeon_id");
        this.playerDataCache = PlayerDataCache.getInstance(); // Ensure that PlayerDataCache is accessible
    }

    public void open(Player player) {
        List<DungeonConfig> configs = new ArrayList<>(dungeonManager.getConfigManager().getAllConfigs());

        String[] structure = {
                "# # # # # # # # #",
                "# # # # # # # # #",
                "# # # # # # # # #"
        };

        Gui gui = Gui.normal()
                .setStructure(structure)
                .addIngredient('#', getFillerItem())
                .build();

        for (int i = 0; i < configs.size() && i < 27; i++) {
            gui.setItem(i, createDungeonItem(configs.get(i)));
        }

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.BLUE + "Dungeon Browser")
                .build();

        window.open();
    }

    private Item createDungeonItem(DungeonConfig config) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Min Players: " + config.getMinPlayers());
        lore.add(ChatColor.GRAY + "Max Players: " + config.getMaxPlayers());
        lore.add(ChatColor.GRAY + "Difficulty: " + config.getDifficulty());
        lore.add(ChatColor.GRAY + "Level Requirement: " + config.getLevelRequirement());

        ChatColor diffColor = ChatColor.BLUE;
        String diff = config.getDifficulty().toLowerCase();
        switch (diff) {
            case "easy" -> diffColor = ChatColor.GREEN;
            case "normal" -> diffColor = ChatColor.BLUE;
            case "hard" -> diffColor = ChatColor.GOLD;
            case "deadly" -> diffColor = ChatColor.RED;
        }

        ItemStack base = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            String displayName = diffColor + config.getName() + " [" + config.getDifficulty() + "]";
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(dungeonIdKey, PersistentDataType.STRING, config.getId());
            base.setItemMeta(meta);
        }

        return new SimpleItem(base) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                int requiredLevel = config.getLevelRequirement();

                // Fetch player level from PlayerDataCache
                PlayerData pd = playerDataCache.getCachedPlayerData(clickPlayer.getUniqueId());
                int playerLevel = (pd != null) ? pd.getLevel() : 1;

                if (playerLevel < requiredLevel) {
                    clickPlayer.sendMessage(ChatColor.RED + "You do not meet the level requirement (" + requiredLevel + ") for this dungeon.");
                    event.getView().close();
                    return;
                }

                // Create or get the lobby
                DungeonLobby lobby = dungeonManager.getLobbyManager().getOrCreateLobby(config.getId(), clickPlayer);
                event.getView().close();
                dungeonManager.getLobbyGUI().open(clickPlayer, lobby);
            }
        };
    }

    private Item getFillerItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
    }
}
