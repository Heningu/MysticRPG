//package eu.xaru.mysticrpg.guis.quests;
//
//import eu.xaru.mysticrpg.guis.admin.MobGUI;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.enchantments.Enchantment;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.ClickType;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.jetbrains.annotations.NotNull;
//import xyz.xenondevs.invui.gui.PagedGui;
//import xyz.xenondevs.invui.gui.structure.Markers;
//import xyz.xenondevs.invui.item.Item;
//import xyz.xenondevs.invui.item.ItemProvider;
//import xyz.xenondevs.invui.item.builder.ItemBuilder;
//import xyz.xenondevs.invui.item.impl.SimpleItem;
//import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
//import xyz.xenondevs.invui.window.Window;
//
//public class QuestGUI {
//
//
//    public QuestGUI() {
//
//    }
//
//
//    public void openQuestGUI(Player player){
//
//
//
//
//        Item controler = new ChangePageItem();
//
//
//        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
//                .setDisplayName(" ")
//                .addAllItemFlags());
//
//
//        Item info = new SimpleItem(new ItemBuilder(Material.WRITABLE_BOOK)
//                .setDisplayName(ChatColor.GOLD + "Quests")
//                .addAllItemFlags()
//                .addEnchantment(Enchantment.UNBREAKING,1,true)
//                .addLoreLines(
//                        "",
//                        ChatColor.DARK_PURPLE + "Here you can find all your completed",
//                        ChatColor.DARK_PURPLE + "and ongoing quests.",
//                        ""
//                )
//        );
//
//
//
//
//
//        PagedGui<Item> pagedGui = PagedGui.items()
//                .setStructure(
//                        "# # # # I # # # #",
//                        "# x x x x x x x #",
//                        "# x x x x x x x #",
//                        "# x x x x x x x #",
//                        "# x x x x x x x #",
//                        "> # # # # # # # #"
//                )
//                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be put
//                .addIngredient('#', border)
//                .addIngredient('>', controler)
//                .addIngredient('I',info)
//                .setContent(QUESTS)
//                .build();
//
//        // Create and open the Window with the paged GUI
//        Window window = Window.single()
//                .setViewer(player)
//                .setTitle(ChatColor.RED + "Loaded custom mobs")
//                .setGui(pagedGui)
//                .build();
//        window.open();
//
//
//    }
//
//    public class ChangePageItem extends ControlItem<PagedGui<?>> {
//
//        @Override
//        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
//            if (clickType == ClickType.LEFT) {
//                getGui().goBack();
//            } else if (clickType == ClickType.RIGHT) {
//                getGui().goForward();
//            }
//        }
//
//        @Override
//        public ItemProvider getItemProvider(PagedGui<?> gui) {
//            return new ItemBuilder(Material.ARROW)
//                    .setDisplayName("Switch pages")
//                    .addLoreLines(
//                            "",
//                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " from " + (gui.getPageAmount()) + " pages",
//                            ChatColor.GREEN + "Left-click to go forward",
//                            ChatColor.RED + "Right-click to go back"
//                    )
//                    .addEnchantment(Enchantment.UNBREAKING,1,true)
//                    .addAllItemFlags();
//        }
//
//    }
//
//
//
//}
