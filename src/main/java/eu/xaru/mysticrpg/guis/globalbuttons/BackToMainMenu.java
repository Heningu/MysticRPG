//package eu.xaru.mysticrpg.guis.globalbuttons;
//
//import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
//import eu.xaru.mysticrpg.guis.MainMenu;
//import eu.xaru.mysticrpg.managers.ModuleManager;
//import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
//import eu.xaru.mysticrpg.player.leveling.LevelModule;
//import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
//import eu.xaru.mysticrpg.quests.QuestModule;
//import eu.xaru.mysticrpg.social.friends.FriendsModule;
//import eu.xaru.mysticrpg.social.party.PartyModule;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.enchantments.Enchantment;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.ClickType;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.jetbrains.annotations.NotNull;
//import xyz.xenondevs.invui.gui.structure.Structure;
//import xyz.xenondevs.invui.item.Item;
//import xyz.xenondevs.invui.item.builder.ItemBuilder;
//import xyz.xenondevs.invui.item.impl.SimpleItem;
//import xyz.xenondevs.invui.window.Window;
//
//public class BackToMainMenu {
//
//    private AuctionHouseModule auctionHouse;
//    private EquipmentModule equipmentModule;
//    private LevelModule levelingModule;
//    private PlayerStatModule playerStat;
//    private QuestModule questModule;
//    private FriendsModule friendsModule;
//    private PartyModule partyModule;
//
//    public BackToMainMenu() {
//
//        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
//        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
//        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
//        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
//        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
//        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
//        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
//    }
//
//
//    // BACK BUTTON
//
//
//    Structure.addGlobalIngredient('<', new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
//
//
//
//    Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
//            .setDisplayName(ChatColor.RED + "Go Back")
//            .addLoreLines(
//                    "",
//                    "Click to get back to the main menu.",
//                    ""
//            )
//            .addAllItemFlags()
//            .addEnchantment(Enchantment.UNBREAKING, 1, true)
//    )
//
//
//
//    {
//        @Override
//        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
//            // Close the current GUI before opening the Equipment GUI
//            Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
//                    (Window) event.getView().getTopInventory().getHolder() : null;
//            if (window != null) {
//                window.close();
//            }
//
//            MainMenu test = new MainMenu(auctionHouse,equipmentModule,levelingModule,playerStat,questModule,friendsModule,partyModule);
//            test.openGUI(player);
//        }
//    };
//
//}