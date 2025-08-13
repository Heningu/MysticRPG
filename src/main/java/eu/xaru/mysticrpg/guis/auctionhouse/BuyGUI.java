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

    private AuctionHouseMainMenu mainGUI = null;

    public BuyGUI(AuctionHouseMainMenu mainGUI) {
        this.mainGUI = mainGUI;
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
            
            // Include all items, not just custom items
            Category itemCategory = CustomItemUtils.isCustomItem(auctionItem) ? 
                CustomItemUtils.getCategory(auctionItem) : getCategoryFromMaterial(auctionItem.getType());
            
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
        
        if (auction.isBidItem()) {
            lore.add(ChatColor.GRAY + "Current bid: " + ChatColor.GOLD + auction.getCurrentBid() + " coins");
            lore.add(ChatColor.BLUE + "Type: Auction");
        } else {
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + auction.getCurrentBid() + " coins");
            lore.add(ChatColor.GREEN + "Type: Buy Now");
        }
        
        // Add time remaining
        long timeLeft = auction.getEndTime() - System.currentTimeMillis();
        String timeLeftStr = formatTimeRemaining(timeLeft);
        lore.add(ChatColor.GRAY + "Time left: " + ChatColor.WHITE + timeLeftStr);
        
        lore.add("");
        if (auction.isBidItem()) {
            lore.add(ChatColor.YELLOW + "Click to place bid!");
        } else {
            lore.add(ChatColor.YELLOW + "Click to purchase!");
        }

        meta.setLore(lore);
        originalItem.setItemMeta(meta);

        return new SimpleItem(new ItemBuilder(originalItem)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (auction.isBidItem()) {
                    // Open bid interface for auctions
                    openBidInterface(player, auction);
                } else {
                    // Direct purchase for sell offers
                    mainGUI.getAuctionHouseHelper().purchaseAuction(player, auction.getAuctionId());
                    
                    // Refresh the GUI to show updated auctions
                    Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                            (Window) event.getView().getTopInventory().getHolder() : null;
                    if (window != null) {
                        window.close();
                    }
                    openAuctionHouseBuyGUI(player);
                }
            }
        };
    }

    /**
     * Opens a simple bid interface for auction items
     */
    private void openBidInterface(Player player, Auction auction) {
        // Simple bid interface with preset amounts
        int currentBid = auction.getCurrentBid();
        int bid10Percent = currentBid + (int) Math.ceil(currentBid * 0.1); // 10% increase
        int bid50Percent = currentBid + (int) Math.ceil(currentBid * 0.5); // 50% increase
        
        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "));
        
        Item bid10 = new SimpleItem(new ItemBuilder(Material.GOLD_NUGGET)
                .setDisplayName(ChatColor.YELLOW + "Bid +10%")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "New bid: " + ChatColor.WHITE + bid10Percent + " coins",
                        "",
                        ChatColor.GREEN + "Click to place this bid"
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                mainGUI.getAuctionHouseHelper().placeBid(p, auction.getAuctionId(), bid10Percent);
                p.closeInventory();
                openAuctionHouseBuyGUI(p);
            }
        };

        Item bid50 = new SimpleItem(new ItemBuilder(Material.GOLD_INGOT)
                .setDisplayName(ChatColor.YELLOW + "Bid +50%")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "New bid: " + ChatColor.WHITE + bid50Percent + " coins",
                        "",
                        ChatColor.GREEN + "Click to place this bid"
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                mainGUI.getAuctionHouseHelper().placeBid(p, auction.getAuctionId(), bid50Percent);
                p.closeInventory();
                openAuctionHouseBuyGUI(p);
            }
        };

        Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Back")
                .addLoreLines("", "Return to auction browser", ""))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                p.closeInventory();
                openAuctionHouseBuyGUI(p);
            }
        };

        Gui bidGui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # a # b # # # #",
                        "# # # # c # # # #"
                )
                .addIngredient('#', filler)
                .addIngredient('a', bid10)
                .addIngredient('b', bid50)
                .addIngredient('c', back)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.YELLOW + "Place Bid")
                .setGui(bidGui)
                .build();
        window.open();
    }

    /**
     * Helper method to format time remaining
     */
    private String formatTimeRemaining(long millis) {
        if (millis <= 0) {
            return ChatColor.RED + "Expired";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Helper method to determine category for non-custom items
     */
    private Category getCategoryFromMaterial(Material material) {
        String name = material.name();
        
        // Weapons
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || name.contains("TRIDENT")) {
            return Category.WEAPONS;
        }
        
        // Armor  
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || 
            name.contains("LEGGINGS") || name.contains("BOOTS")) {
            return Category.ARMOR;
        }
        
        // Tools
        if (name.contains("PICKAXE") || name.contains("SHOVEL") || name.contains("HOE")) {
            return Category.TOOLS;
        }
        
        // Consumables
        if (material.isEdible() || name.contains("POTION") || name.contains("APPLE")) {
            return Category.CONSUMABLES;
        }
        
        // Blocks
        if (material.isBlock()) {
            return Category.BLOCKS;
        }
        
        // Default to miscellaneous/tools
        return Category.TOOLS;
    }
}
