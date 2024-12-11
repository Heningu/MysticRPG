package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.auctionhouse.AuctionsGUI;
import eu.xaru.mysticrpg.customs.items.Category;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.guis.admin.MobGUI;
import eu.xaru.mysticrpg.guis.globalbuttons.CategoryTabItem;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * This example places the pagination controls (next/prev page) in the outer TabGui,
 * and the inner GUIs (the tabs) are just pure PagedGui content without their own pagination items.
 *
 * The TabGui is 9x6. We have a border reducing the internal content area to 7x4.
 * The tabs themselves each return a PagedGui, but do NOT have their own pagination controls internally.
 * Instead, we control pagination from the TabGui level using a special ControlItem that references the TabGui,
 * finds the currently selected tab, which is a PagedGui, and calls goForward()/goBack() on it.
 *
 * This demonstrates how to "speak" to the current tab's pagination from the outer level.
 */
public class BuyGUI {

    private final AuctionsGUI mainGUI;
    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private PlayerStatModule playerStat;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;

    public BuyGUI(){
        this.mainGUI = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class).getAuctionsGUI();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openAuctionHouseBuyGUI(Player player){

        if (!mainGUI.getAuctionHouseHelper().areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Please wait, auctions are still loading."));
            return;
        }

        UUID playerId = player.getUniqueId();
        Category selectedCategory = mainGUI.getBuyGuiSelectedCategoryMap().get(playerId);
        if (selectedCategory == null) {
            selectedCategory = Category.EVERYTHING;
            mainGUI.getBuyGuiSelectedCategoryMap().put(playerId, selectedCategory);
        }

        // Border
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags());

        // Back button
        Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines(
                        "",
                        "Click to get back to the main menu.",
                        ""
                )
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(p);
            }
        };

        // Create a PagedGui for each category
        List<Gui> categoryGuis = new ArrayList<>();
        for (Category cat : Category.values()) {
            PagedGui<Item> pg = createPagedGuiForCategory(cat);
            categoryGuis.add(pg);
        }

        // Create the TabGui
        // We have a 9x6 GUI with a border, so inside is a 7x4 area for the tabs.
        // Pagination controls are placed outside in the TabGui layer:
        // We'll place two control items somewhere in the bottom row of the main TabGui.
        // Example structure:
        // 9x6 layout:
        // Row 1: 0 1 2 3 4 5 6 7 8
        // Row 2: # x x x x x x x #
        // Row 3: # x x x x x x x #
        // Row 4: # x x x x x x x #
        // Row 5: # x x x x x x x #
        // Row 6: B # # < # > # # #
        //
        // '<' and '>' are placed at the bottom row for page navigation of the current tab.

        ChangePageItemBack backPage = new ChangePageItemBack();
        ChangePageItemForward forwardPage = new ChangePageItemForward();
        ChangePageItem changePageItem = new ChangePageItem();
        TabGui buyGUI = TabGui.normal()
                .setStructure(
                        "0 1 2 3 4 5 6 7 8",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B # # # s # # # #"
                )
                .addIngredient('#', border)
                .addIngredient('0', new CategoryTabItem(0,"Everything", Material.STICK))
                .addIngredient('1', new CategoryTabItem(1,"Weapons", Material.DIAMOND_SWORD))
                .addIngredient('2', new CategoryTabItem(2,"Armor", Material.DIAMOND_HELMET))
                .addIngredient('3', new CategoryTabItem(3,"Magic", Material.BLAZE_ROD))
                .addIngredient('4', new CategoryTabItem(4,"CONSUMABLE", Material.GOLDEN_APPLE))
                .addIngredient('5', new CategoryTabItem(5,"ACCESSORY", Material.HEART_OF_THE_SEA))
                .addIngredient('6', new CategoryTabItem(6,"TOOL", Material.STONE_PICKAXE))
                .addIngredient('7', new CategoryTabItem(7,"ARTIFACT", Material.NETHER_STAR))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('B', back)
                .addIngredient('s', changePageItem)
                .setTabs(categoryGuis)
                .build();

        // We must set the TabGui as the GUI for these control items so they can access the selected tab
        changePageItem.setGui(buyGUI);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.translateAlternateColorCodes('&', "&cAuction House - Buy"))
                .setGui(buyGUI)
                .build();

        window.open();
    }

    /**
     * Creates a PagedGui for the category with random items, 7x4 area:
     * "x x x x x x x"
     * "x x x x x x x"
     * "x x x x x x x"
     * "x x x x x x x"
     * No pagination controls here. We rely on the outer GUI to handle paging.
     */
    private PagedGui<Item> createPagedGuiForCategory(Category category){
        int totalItems = 50;
        List<Item> auctionItems = generateRandomItemsForCategory(category, totalItems);

        // Just content slots (28 slots)
        PagedGui<Item> pagedGui = PagedGui.items()
                .setStructure(
                        "x x x x x x x",
                        "x x x x x x x",
                        "x x x x x x x",
                        "x x x x x x x"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(auctionItems)
                .build();

        return pagedGui;
    }

    /**
     * Generate random items for demonstration
     */
    private List<Item> generateRandomItemsForCategory(Category category, int count) {
        List<Item> items = new ArrayList<>(count);
        Random random = new Random(category.ordinal());
        Material[] mats = Material.values();

        for (int i = 0; i < count; i++) {
            Material m;
            do {
                m = mats[random.nextInt(mats.length)];
            } while (m.isAir() || !m.isItem());

            ItemBuilder builder = new ItemBuilder(m)
                    .setDisplayName(ChatColor.YELLOW + category.name() + " Item #" + (i+1))
                    .addLoreLines(ChatColor.GRAY + "A random item for " + category.name(),
                            ChatColor.GRAY + "Item number " + (i+1),
                            ChatColor.GRAY + "Page demonstration item.");
            items.add(new SimpleItem(builder));
        }

        return items;
    }

    /**
     * ControlItem for going to the previous page of the currently selected tab (PagedGui).
     * This item controls the pagination from the outer TabGui layer.
     */
    public class ChangePageItemBack extends ControlItem<TabGui> {

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            TabGui tabGui = getGui();
            Gui currentTab = tabGui.getTabs().get(tabGui.getCurrentTab());

            // Current tab is a PagedGui
            if (currentTab instanceof PagedGui<?> paged) {
                if (paged.hasPreviousPage()) {
                    paged.goBack();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                } else {
                    player.sendMessage(ChatColor.RED + "No previous page!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                }
                notifyWindows(); // Refresh the item if needed
            }
        }

        @Override
        public ItemProvider getItemProvider(TabGui gui) {
            // The displayed page info: we must reflect the current tab's page info
            Gui currentTab = gui.getTabs().get(gui.getCurrentTab());
            String lore;
            if (currentTab instanceof PagedGui<?> paged) {
                if (!paged.hasPreviousPage()) {
                    lore = ChatColor.GRAY + "You're on the first page.";
                } else {
                    lore = ChatColor.GRAY + "Click to go back to page " + (paged.getCurrentPage());
                }
            } else {
                lore = ChatColor.GRAY + "No paging available.";
            }

            return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setDisplayName(ChatColor.RED + "Previous Page")
                    .addLoreLines(lore)
                    .addAllItemFlags();
        }
    }

    /**
     * ControlItem for going to the next page of the currently selected tab (PagedGui).
     */
    public class ChangePageItemForward extends ControlItem<TabGui> {

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            TabGui tabGui = getGui();
            Gui currentTab = tabGui.getTabs().get(tabGui.getCurrentTab());

            if (currentTab instanceof PagedGui<?> paged) {
                if (paged.hasNextPage()) {
                    paged.goForward();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                } else {
                    player.sendMessage(ChatColor.RED + "No next page!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                }
                notifyWindows(); // Refresh the item if needed
            }
        }

        @Override
        public ItemProvider getItemProvider(TabGui gui) {
            Gui currentTab = gui.getTabs().get(gui.getCurrentTab());
            String lore;
            if (currentTab instanceof PagedGui<?> paged) {
                if (!paged.hasNextPage()) {
                    lore = ChatColor.GRAY + "There are no more pages.";
                } else {
                    lore = ChatColor.GRAY + "Click to go forward to page " + (paged.getCurrentPage() + 2);
                }
            } else {
                lore = ChatColor.GRAY + "No paging available.";
            }

            return new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                    .setDisplayName(ChatColor.GREEN + "Next Page")
                    .addLoreLines(lore)
                    .addAllItemFlags();
        }
    }

    /**
     * Handles pagination controls - Left-click to go back, Right-click to go forward.
     * Currently, pagination isn't implemented, so these actions will notify the player.
     */
    public class ChangePageItem extends ControlItem<TabGui> {

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            TabGui tabGui = getGui();
            Gui currentTab = tabGui.getTabs().get(tabGui.getCurrentTab());

            if (currentTab instanceof PagedGui<?> paged) {
                if(clickType == ClickType.LEFT) {
                    if (paged.hasPreviousPage()) {
                        paged.goBack();
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    } else {
                        player.sendMessage(ChatColor.RED + "No previous page!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                    }
                } else if(clickType == ClickType.RIGHT) {
                    if (paged.hasNextPage()) {
                        paged.goForward();
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    } else {
                        player.sendMessage(ChatColor.RED + "No next page!");
                    }
                }
                notifyWindows();
            }
        }

        @Override
        public ItemProvider getItemProvider(TabGui gui) {
            Gui currentTab = gui.getTabs().get(gui.getCurrentTab());
            String lore = "";
            if (currentTab instanceof PagedGui<?> paged) {
                lore +=
                        ChatColor.GRAY + "Current page: " + (paged.getCurrentPage() + 1) + " from " + (paged.getPageAmount()) + " pages\n" +
                        ChatColor.GREEN + "Left-click to go forward\n" +
                        ChatColor.RED + "Right-click to go back\n";
            } else {
                lore = ChatColor.GRAY + "No paging available.";
            }

            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch pages")
                    .addLoreLines(
                            lore
                    )
                    .addEnchantment(Enchantment.UNBREAKING,1,true)
                    .addAllItemFlags();
        }

    }

}
