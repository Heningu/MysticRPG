package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.customs.items.Category;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.guis.ChangePageItem;
import eu.xaru.mysticrpg.guis.globalbuttons.CategoryTabItem;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.stream.Collectors;

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

    private final AuctionHouseMainMenu mainGUI;

    public BuyGUI() {
        this.mainGUI = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class).getAuctionsGUI();
    }

    public void openAuctionHouseBuyGUI(Player player) {

        if (!mainGUI.getAuctionHouseHelper().areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Please wait, auctions are still loading."));
            return;
        }

        UUID playerId = player.getUniqueId();
        //Category selectedCategory = mainGUI.getBuyGuiSelectedCategoryMap().computeIfAbsent(playerId, k -> Category.EVERYTHING);

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
                AuctionHouseMainMenu mainMenu = new AuctionHouseMainMenu();
                mainMenu.openAuctionsGUI(p);
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
                .addIngredient('0', new CategoryTabItem(0,"WEAPON", Material.STICK))
                .addIngredient('1', new CategoryTabItem(1,"ARMOR", Material.DIAMOND_SWORD))
                .addIngredient('2', new CategoryTabItem(2,"MAGIC", Material.DIAMOND_HELMET))
                .addIngredient('3', new CategoryTabItem(3,"CONSUMABLE", Material.BLAZE_ROD))
                .addIngredient('4', new CategoryTabItem(4,"ACCESSORY", Material.GOLDEN_APPLE))
                .addIngredient('5', new CategoryTabItem(5,"TOOL", Material.HEART_OF_THE_SEA))
                .addIngredient('6', new CategoryTabItem(6,"ARTIFACT", Material.STONE_PICKAXE))
                .addIngredient('7', new CategoryTabItem(7,"EVERYTHING", Material.NETHER_STAR))
                .addIngredient('8', new CategoryTabItem(8,"QUEST", Material.ENCHANTED_BOOK))
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

    private PagedGui<Item> createPagedGuiForCategory(Category category) {
        List<Auction> allAuctions = mainGUI.getAuctionHouseHelper().getActiveAuctions();
        List<Item> auctionItems = new ArrayList<>();

        // Filter auctions by category
        for (Auction auction : allAuctions) {
            ItemStack auctionItem = auction.getItem();
            
            // Skip if not a custom item
            if (!CustomItemUtils.isCustomItem(auctionItem)) continue;
            
            Category itemCategory = CustomItemUtils.getCategory(auctionItem);
            
            // If category is EVERYTHING or matches the current category
            if (category == Category.EVERYTHING || category == itemCategory) {
                auctionItems.add(createAuctionDisplayItem(auction));
            }
        }

        return PagedGui.items()
                .setStructure(
                        "x x x x x x x",
                        "x x x x x x x",
                        "x x x x x x x",
                        "x x x x x x x"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(auctionItems)
                .build();
    }

    private Item createAuctionDisplayItem(Auction auction) {
        ItemStack originalItem = auction.getItem().clone();
        ItemMeta meta = originalItem.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        // Add auction information to lore
        lore.add("");
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.YELLOW + auction.getSellerName());
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + auction.getCurrentBid() + " coins");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to purchase!");

        meta.setLore(lore);
        originalItem.setItemMeta(meta);

        return new SimpleItem(new ItemBuilder(originalItem)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Handle purchase
                mainGUI.getAuctionHouseHelper().purchaseAuction(player, auction.getAuctionId());
                
                // Refresh the GUI to show updated auctions
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                openAuctionHouseBuyGUI(player);
            }
        };
    }
}
