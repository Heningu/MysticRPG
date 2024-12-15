package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;

import eu.xaru.mysticrpg.guis.auctionhouse.AuctionHouseMainMenu;
import eu.xaru.mysticrpg.guis.player.social.FriendsGUI;
import eu.xaru.mysticrpg.guis.player.social.PartyGUI;
import eu.xaru.mysticrpg.guis.player.stats.CharacterGUI;
import eu.xaru.mysticrpg.guis.player.EquipmentGUI;
import eu.xaru.mysticrpg.guis.player.stats.LevelingGUI;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

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


        // TBI means To be implemented

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));

        // Left part of the GUI

        Item equip = new SimpleItem(new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .setDisplayName(ChatColor.AQUA + "Equipment")
                .addLoreLines("Manage your equipment here.")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING,1,true)
        )



        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                EquipmentGUI equip = new EquipmentGUI();
                equip.openEquipmentGUI(player);
            }
        }; // Ingredient E


        Item buffs = new SimpleItem(new ItemBuilder(Material.REDSTONE)
                .setDisplayName("Buffs")
                .addLoreLines(
                        "See your current buffs or simply",
                        "apply a buff to help your journey.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient F

        Item pets = new SimpleItem(new ItemBuilder(Material.PIGLIN_HEAD)
                .setDisplayName("Pets")
                .addLoreLines(
                        "Need a companion? Click here to see",
                        "who can join and help you while",
                        "exploring the Mystic Realm."
                )
                .addAllItemFlags()
        ); // Ingredient G

        Item mounts = new SimpleItem(new ItemBuilder(Material.DRAGON_HEAD)
                .setDisplayName("Mounts")
                .addLoreLines(
                        "Need a ride? Check your summons to",
                        "explore the world differently.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient H

        Item professions = new SimpleItem(new ItemBuilder(Material.STONE_PICKAXE)
                .setDisplayName("Professions")
                .addLoreLines(
                        "Mining? Chopping monsters? or just",
                        "sit and fish for some goodies?",
                        "Check how well you mastered your",
                        "favorite hobby.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient I

        Item skills = new SimpleItem(new ItemBuilder(Material.DIAMOND_SWORD)
                .setDisplayName("Skills")
                .addLoreLines(
                        "Depending on what you skilled, you will get",
                        "different extras on your professions or while",
                        "fighting. Choose wisely!",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient D

        Item reputation = new SimpleItem(new ItemBuilder(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE)
                .setDisplayName("Reputation")
                .addLoreLines(
                        "You like to converse with npcs? Huh?,",
                        "suddenly they offer me discounts or new",
                        "items. Hmmm, maybe i should do more quests",
                        "and trade with them as they started to like me...",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient J

        Item storage = new SimpleItem(new ItemBuilder(Material.ENDER_CHEST)
                .setDisplayName("Storage")
                .addLoreLines(
                        "Store all your items directly in your storage.",
                        "You can access everything from wherever you are",
                        "but keep in mind that you cant be in combat.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient B

        // Right part of the GUI

        Item character = new SimpleItem(new ItemBuilder(Material.BOOK)
                .setDisplayName(ChatColor.GREEN + "Character")
                .addLoreLines(
                        "",
                        "See your attributes and use your",
                        "attribute points to let your",
                        "character grow. What trait do",
                        "you prefer?",
                        ""
                )
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING,1,true)
        )



        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
//                LevelingGUI leveling = new LevelingGUI();
//                leveling.openLevelingGUI(player);
                CharacterGUI chargui = new CharacterGUI();
                chargui.openCharacterGUI(player);
            }
        };// Ingredient A

        Item leveling = new SimpleItem(new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setDisplayName(ChatColor.GREEN + "Leveling")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "Keep track of your character leveling.",
                        ChatColor.GRAY + "See what level you are, what levels are",
                        ChatColor.GRAY + "infront of you and what benefits you gain",
                        ""
                )
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING,1,true)
        )



        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                LevelingGUI leveling = new LevelingGUI();
                leveling.openLevelingGUI(player);
            }
        };

        Item collections = new SimpleItem(new ItemBuilder(Material.ITEM_FRAME)
                .setDisplayName("Collections")
                .addLoreLines(
                        "Here are all your items, mobs and bosses",
                        "that you discovered. To not lose track of",
                        "them, you can find information here",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient C

        Item housing = new SimpleItem(new ItemBuilder(Material.IRON_DOOR)
                .setDisplayName("Housing")
                .addLoreLines(
                        "Here you can manage your property.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient 9

        Item auctions = new SimpleItem(new ItemBuilder(Material.CHEST)
                .setDisplayName("Auction House")
                .addLoreLines(
                        "You can find here the Auction house.",
                        "To avoid transport fees, go to your",
                        "local Auction Manager in your area."
                )
                .addAllItemFlags()
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                // Open the Equipment GUI
                AuctionHouseMainMenu ahmain = new AuctionHouseMainMenu();
                ahmain.openAuctionsGUI(player);
            }
        }; // Ingredient 8

        Item events = new SimpleItem(new ItemBuilder(Material.FIREWORK_ROCKET)
                .setDisplayName("Events")
                .addLoreLines(
                        "Check all ongoing events.",
                        "Events can be Boss-spawns, drops or",
                        "time-limited content",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient 7

        Item fasttravel = new SimpleItem(new ItemBuilder(Material.REPEATER)
                .setDisplayName("Fast Travel")
                .addLoreLines(
                        "Check your unlocked Fast Travel locations.",
                        "Keep in mind that fast traveling requires you",
                        "to hold a specific gold amount as fees",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient 6

        Item quests = new SimpleItem(new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName("Quests")
                .addLoreLines(
                        "Click here to see all of your quests.",
                        "Current pinned Quest: TBI"
                )
                .addAllItemFlags()
        )
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                questModule.openQuestGUI(player);


            }
        }; // Ingredient 5

        // Middle of the GUI

        Item friends = new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName("Your Friends")
                .addLoreLines(
                        "Click here to manage your friends.",
                        "Friends: TBI",
                        "Currently online: TBI"
                )
                .addAllItemFlags()
        )        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                FriendsGUI friendsGUI = new FriendsGUI();
                friendsGUI.openFriendsGUI(player);


            }
        }; // Ingredient 4

        Item guilds = new SimpleItem(new ItemBuilder(Material.WHITE_BANNER)
                .setDisplayName("Guilds")
                .addLoreLines(
                        "Click here to manage your guild or find one.",
                        "Will be implemented in the Beta"
                )
                .addAllItemFlags()
        ); // Ingredient 3

        Item party = new SimpleItem(new ItemBuilder(Material.CAKE)
                .setDisplayName("Party")
                .addLoreLines(
                        "Click here to manage your current Party."
                )
                .addAllItemFlags()
        )
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                PartyGUI partygui = new PartyGUI(partyModule.getPartyHelper());
                partygui.openPartyGUI(player);


            }
        };



        // Ingredient 2

        // Header Item

        Item info = new SimpleItem(new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(ChatColor.GOLD + "MysticRealm")
                .addLoreLines(
                        ">----------------------------------------<",
                        ChatColor.DARK_PURPLE + "This is the mystic realm!",
                        ChatColor.GRAY + "Here you can find all the important menus!",
                        ">----------------------------------------<"
                )
                .addEnchantment(Enchantment.UNBREAKING,1,true)
                .addAllItemFlags()
        ); // Ingredient 1



        Gui builder = Gui.normal().setStructure(
                        "# # # # 1 # # # #",
                        "# E F # # # A L #",
                        "# G H # 4 # C 9 #",
                        "# I D # 3 # 8 7 #",
                        "# J B # 2 # 6 5 #",
                        "# # # # # # # # #"



                )
                .addIngredient('#', border)
                .addIngredient('1', info)
                .addIngredient('2', party)
                .addIngredient('3', guilds)
                .addIngredient('4', friends)
                .addIngredient('5', quests)
                .addIngredient('6', fasttravel)
                .addIngredient('7', events)
                .addIngredient('8', auctions)
                .addIngredient('9', housing)
                .addIngredient('C', collections)
                .addIngredient('L', leveling)
                .addIngredient('A', character)
                .addIngredient('B', storage)
                .addIngredient('D', skills)
                .addIngredient('E', equip)
                .addIngredient('F', buffs)
                .addIngredient('G', pets)
                .addIngredient('H', mounts)
                .addIngredient('I', professions)
                .addIngredient('J', reputation)

                .build();

        return builder;

    }

    public void openGUI(Player player) {

        Gui builder = buildGui();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GOLD + "Main Menu")
                .setGui(builder)
                .build();

        window.open();
    }


}

