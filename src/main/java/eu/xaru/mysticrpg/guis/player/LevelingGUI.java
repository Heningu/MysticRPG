package eu.xaru.mysticrpg.guis.player;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.LevelData;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LevelingGUI {
    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final PlayerStatModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;
    private final PlayerDataCache playerDataCache;

    // The inventory is 9x6 = 54 slots
    // Define the placeholders in your structure:
    // "1 2 3 # # # G H I"
    // "# # 4 # P # F # #"
    // "# # 5 # # # E # #"
    // "# # 6 7 8 9 D # #"
    //
    // Let's say:
    // Row and column indexing (0-based):
    // Row 0: # # # # # # # # #
    // Row 1: 1(9), 2(10), 3(11), #12, #13, #14, G(15), H(16), I(17)
    // Row 2: #18, #19, 4(20), #21, P(22), #23, F(24), #25, #26
    // Row 3: #27, #28, 5(29), #30, #31, #32, E(33), #34, #35
    // Row 4: #36, #37, 6(38), 7(39), 8(40), 9(41), D(42), #43, #44
    // Row 5: C(45), >(46), #47,#48,#49,#50,#51,#52,#53
    //
    // Extract the slot indexes for '1','2','3','4','5','6','7','8','9','D','E','F','G','H','I':
    // 1 -> slot 9
    // 2 -> slot 10
    // 3 -> slot 11
    // 4 -> slot 20
    // 5 -> slot 29
    // 6 -> slot 38
    // 7 -> slot 39
    // 8 -> slot 40
    // 9 -> slot 41
    // D -> slot 42
    // E -> slot 33
    // F -> slot 24
    // G -> slot 15
    // H -> slot 16
    // I -> slot 17
    //
    // Put them in an array in the order you want items to appear:
    private static final int[] CONTENT_SLOTS = {
            9, 10, 11, 20, 29, 38, 39, 40, 41, 42, 33, 24, 15, 16, 17
    };

    public LevelingGUI() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openLevelingGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Failed to retrieve your level data.");
            return;
        }

        int maxLevel = levelingModule.getMaxLevel();
        int playerLevel = playerData.getLevel();

        List<Item> levels = new ArrayList<>();
        for (int level = 1; level <= maxLevel; level++) {
            LevelData levelData = levelingModule.getLevelData(level);
            if (levelData == null) continue;

            Material material = playerLevel >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String displayName = ChatColor.GREEN + "Level " + level;

            ItemBuilder itemBuilder = new ItemBuilder(material)
                    .setDisplayName(displayName)
                    .addLoreLines(ChatColor.GRAY + "Required XP: " + levelData.getXpRequired())
                    .addLoreLines(ChatColor.YELLOW + "Rewards:");

            for (Map.Entry<String, Integer> rewardEntry : levelData.getRewards().entrySet()) {
                itemBuilder.addLoreLines(ChatColor.AQUA + rewardEntry.getKey() + ": " + rewardEntry.getValue());
            }

            if (playerLevel >= level) {
                itemBuilder.addEnchantment(Enchantment.UNBREAKING, 1, true)
                        .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            Item levelItem = new SimpleItem(itemBuilder) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                    // Handle level item click if needed
                }
            };
            levels.add(levelItem);
        }

        // Static items
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };

        Item controler = new ChangePageItem();
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").addAllItemFlags());
        Item playerHead = new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(ChatColor.GREEN + player.getName() + " - Level: " + playerLevel)
                .addAllItemFlags());

        int width = 9;
        int height = 6;

        // Create a PagedGui using ofItems. This will handle pagination.
        // items are placed in CONTENT_SLOTS in order.
        PagedGui<Item> pagedGui = PagedGui.ofItems(width, height, levels, CONTENT_SLOTS);

        // Now set static items by their slot indexes:
        // Borders (#) - fill all '#' placeholders with border if needed
        // According to our structure, we can place borders on all '#' slots.
        // For demonstration, let's place borders on all '#' placeholders:
        // '#' at positions: (just place border in all empty slots)
        for (int slot = 0; slot < width * height; slot++) {
            // If this slot is not in content slots and not reserved for a special item, we can fill it with border
            if (!isSpecialSlot(slot) && !isContentSlot(slot)) {
                pagedGui.setItem(slot, border);
            }
        }

        // Set Player Head (P) at slot 22 (from your structure analysis)
        pagedGui.setItem(22, playerHead);

        // Set Back (C) at slot 45
        pagedGui.setItem(45, back);

        // Set Controler (>) at slot 46
        pagedGui.setItem(46, controler);

        // If you have other static items like a border or something else in specific slots, set them similarly.

        // Bake the GUI after setting all items
        pagedGui.bake();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GREEN + "Character Leveling")
                .setGui(pagedGui)
                .build();
        window.open();
    }

    private boolean isContentSlot(int slot) {
        for (int s : CONTENT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private boolean isSpecialSlot(int slot) {
        // This method should return true if the slot is used for 'C','P','>','1','2', ... etc.
        // Since we handle them individually, just check if slot is any of your known static placeholders
        // For example: P(22), C(45), >(46)
        return slot == 22 || slot == 45 || slot == 46;
    }

    public class ChangePageItem extends ControlItem<PagedGui<?>> {
        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.RIGHT) {
                getGui().goForward();
            } else if (clickType == ClickType.LEFT) {
                getGui().goBack();
            }
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch Pages")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " of " + gui.getPageAmount(),
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addEnchantment(Enchantment.UNBREAKING, 1, true)
                    .addAllItemFlags();
        }
    }
}
