package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
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
public class AuctionHouseMainMenu {

    private final Gui gui;

    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private PlayerStatModule playerStat;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;



    public AuctionHouseMainMenu() {

        this.gui = buildGui();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    /**
     * Builds the Main Menu GUI using InvUI's GuiBuilder.
     *
     * @return The constructed Gui instance.
     */
    public Gui buildGui() {


        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));

        Item buy = new SimpleItem(new ItemBuilder(Material.CHEST)
                .setDisplayName(ChatColor.GREEN + "Browse Auctions")
                .addLoreLines(
                        "",
                        "Look for items you may be interested in.",
                        ""
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
                BuyGUI buyGUI = new BuyGUI();
                buyGUI.openAuctionHouseBuyGUI(player);
            }
        }; // Ingredient 1

        Item sell = new SimpleItem(new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(ChatColor.GREEN + "Sell your Item")
                .addLoreLines(
                        "",
                        "Start selling your item here.",
                        ""
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
                // Open the Equipment GUI
                // equipmentModule.getEquipmentManager().getEquipmentGUI().openEquipmentGUI(player);
            }
        }; // Ingredient 2

        Item currentoffers = new SimpleItem(new ItemBuilder(Material.BOOKSHELF)
                .setDisplayName(ChatColor.GREEN + "Your current offers")
                .addLoreLines(
                        "",
                        "Check what items you currently sell.",
                        ""
                )
                .addAllItemFlags()
        );


            // BACK BUTTON

        Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines(
                        "",
                        "Click to get back to the main menu.",
                        ""
                )
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true)
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

                MainMenu test = new MainMenu(auctionHouse,equipmentModule,levelingModule,playerStat,questModule,friendsModule,partyModule);
                test.openGUI(player);
            }
        }; // Ingredient B





        Gui builder = Gui.normal().setStructure(
                        "# # # # # # # # #",
                        "# 1 # # 2 # # 3 #",
                        "# # # # # # # # #",
                        "# # # # B # # # #"



                )
                .addIngredient('#', filler)
                .addIngredient('1', buy)
                .addIngredient('2', sell)
                .addIngredient('3', currentoffers)
                .addIngredient('B', back)

                .build();

        return builder;

    }

    public void openAuctionsGUI(Player player) {

        Gui builder = buildGui();

        Window window = Window.single()
                .setViewer(player)
                .setTitle("Auction house")
                .setGui(builder)
                .build();

        window.open();
    }


}

