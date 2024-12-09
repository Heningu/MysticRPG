package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.invui.gui.Gui;
import eu.xaru.mysticrpg.guis.invui.item.Item;
import eu.xaru.mysticrpg.guis.invui.item.ItemProvider;
import eu.xaru.mysticrpg.guis.invui.item.builder.ItemBuilder;
import eu.xaru.mysticrpg.guis.invui.item.impl.CommandItem;
import eu.xaru.mysticrpg.guis.invui.item.impl.SimpleItem;
import eu.xaru.mysticrpg.guis.invui.window.Window;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.logging.Level;

/**
 * Represents the Main Menu GUI for the MysticRPG plugin using InvUI.
 */
public class MainMenu {

    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final PlayerStatModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;

    private final Gui gui;

    /**
     * Constructor for MainMenu. Initializes module references and builds the GUI.
     *
     * @param auctionHouse    The AuctionHouseModule instance.
     * @param equipmentModule The EquipmentModule instance.
     * @param levelingModule  The LevelModule instance.
     * @param playerStat      The PlayerStatModule instance.
     * @param questModule     The QuestModule instance.
     * @param friendsModule   The FriendsModule instance.
     * @param partyModule     The PartyModule instance.
     */
    public MainMenu(
            AuctionHouseModule auctionHouse,
            EquipmentModule equipmentModule,
            LevelModule levelingModule,
            PlayerStatModule playerStat,
            QuestModule questModule,
            FriendsModule friendsModule,
            PartyModule partyModule
    ) {
        this.auctionHouse = auctionHouse;
        this.equipmentModule = equipmentModule;
        this.levelingModule = levelingModule;
        this.playerStat = playerStat;
        this.questModule = questModule;
        this.friendsModule = friendsModule;
        this.partyModule = partyModule;

        this.gui = buildGui();
    }

    /**
     * Builds the Main Menu GUI using InvUI's GuiBuilder.
     *
     * @return The constructed Gui instance.
     */
    public Gui buildGui() {

        // Define the structure using InvUI's Structure-like approach
        // For simplicity, we'll manually set items in specific slots

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));


        Gui builder = Gui.normal().setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # # # # # # #")
                .addIngredient('#', border)
                .build();

        return builder;

    }
}

//        // Create and add interactive items
//        // Slot indices: 0-53 (9x6 grid)
//
//        // Auctions - Slot 11
//        CommandItem auctionsItem = new CommandItem(new ItemStack(Material.CHEST), (player, clickType) -> {
//            auctionHouse.openAuctionGUI(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Auctions GUI from Main Menu.", 0);
//        });
//        auctionsItem.setDisplayName(ChatColor.GREEN + "Auctions");
//        auctionsItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to access the Auctions",
//                ChatColor.GRAY + "house and manage your listings."
//        ));
//        builder.setItem(11, auctionsItem);
//
//        // Equipment - Slot 15
//        CommandItem equipmentItem = new CommandItem(new ItemStack(Material.DIAMOND_CHESTPLATE), (player, clickType) -> {
//            equipmentModule.getEquipmentManager().getEquipmentGUI().open(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Equipment GUI from Main Menu.", 0);
//        });
//        equipmentItem.setDisplayName(ChatColor.AQUA + "Equipment");
//        equipmentItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to manage your",
//                ChatColor.GRAY + "equipment and gear."
//        ));
//        builder.setItem(15, equipmentItem);
//
//        // Leveling - Slot 13
//        CommandItem levelingItem = new CommandItem(new ItemStack(Material.EXPERIENCE_BOTTLE), (player, clickType) -> {
//            levelingModule.getLevelingMenu().openLevelingMenu(player, 1);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Leveling GUI from Main Menu.", 0);
//        });
//        levelingItem.setDisplayName(ChatColor.LIGHT_PURPLE + "Leveling");
//        levelingItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to view and",
//                ChatColor.GRAY + "manage your leveling progress."
//        ));
//        builder.setItem(13, levelingItem);
//
//        // Stats - Slot 22
//        CommandItem statsItem = new CommandItem(new ItemStack(Material.BOOK), (player, clickType) -> {
//            playerStat.getPlayerStatMenu().openStatMenu(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Stats GUI from Main Menu.", 0);
//        });
//        statsItem.setDisplayName(ChatColor.BLUE + "Stats");
//        statsItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to view and",
//                ChatColor.GRAY + "enhance your attributes."
//        ));
//        builder.setItem(22, statsItem);
//
//        // Quests - Slot 16
//        CommandItem questsItem = new CommandItem(new ItemStack(Material.WRITABLE_BOOK), (player, clickType) -> {
//            questModule.openQuestGUI(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Quest GUI from Main Menu.", 0);
//        });
//        questsItem.setDisplayName(ChatColor.GOLD + "Quests");
//        questsItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to view and",
//                ChatColor.GRAY + "manage your quests."
//        ));
//        builder.setItem(16, questsItem);
//
//        // Friends - Slot 19
//        CommandItem friendsItem = new CommandItem(new ItemStack(Material.PLAYER_HEAD), (player, clickType) -> {
//            friendsModule.openFriendsGUI(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Friends GUI from Main Menu.", 0);
//        });
//        friendsItem.setDisplayName(ChatColor.YELLOW + "Friends");
//        friendsItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to view and",
//                ChatColor.GRAY + "manage your friends."
//        ));
//        builder.setItem(19, friendsItem);
//
//        // Party - Slot 25
//        CommandItem partyItem = new CommandItem(new ItemStack(Material.CAKE), (player, clickType) -> {
//            partyModule.openPartyGUI(player);
//            DebugLogger.getInstance().log(Level.INFO, player.getName() + " opened the Party GUI from Main Menu.", 0);
//        });
//        partyItem.setDisplayName(ChatColor.PINK + "Party");
//        partyItem.setLore(Arrays.asList(
//                ChatColor.GRAY + "Click to view and",
//                ChatColor.GRAY + "manage your parties."
//        ));
//        builder.setItem(25, partyItem);
//
//        // Fill remaining slots with placeholders
//        // Already handled by setBackground in builder
//
//        return builder.build();
//    }
//
//    /**
//     * Opens the Main Menu GUI for the specified player.
//     *
//     * @param player The player to open the GUI for.
//     */
//    public void open(Player player) {
//        gui.open(player);
//    }
