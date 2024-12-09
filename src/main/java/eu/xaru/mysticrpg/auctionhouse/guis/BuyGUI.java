package eu.xaru.mysticrpg.auctionhouse.guis;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionsGUI;
import eu.xaru.mysticrpg.customs.items.Category;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles the Buy GUI, including displaying available auctions, category filtering, and pagination.
 */
public class BuyGUI {

    private final AuctionsGUI mainGUI;
    private static final Logger logger = Logger.getLogger(BuyGUI.class.getName());

    // Constants for GUI slot indices
    public static final int BUY_GUI_BACK_SLOT = 49;
    public static final int BUY_GUI_PAGE_INDICATOR_SLOT = 50;

    // Define inner slots where auction items will be placed
    private static final int[] AUCTION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    /**
     * Constructs the BuyGUI with a reference to the main AuctionsGUI.
     *
     * @param mainGUI The main AuctionsGUI instance.
     */
    public BuyGUI(AuctionsGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    /**
     * Opens the Buy GUI showing available auctions with pagination.
     *
     * @param player The player to open the GUI for.
     */
    public void openBuyGUI(Player player) {
        if (!mainGUI.getAuctionHouseHelper().areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$("Please wait, auctions are still loading."));
            return;
        }

        UUID playerId = player.getUniqueId();
        Category selectedCategory = mainGUI.getBuyGuiSelectedCategoryMap().get(playerId);
        if (selectedCategory == null) {
            selectedCategory = Category.EVERYTHING; // Default to EVERYTHING
            mainGUI.getBuyGuiSelectedCategoryMap().put(playerId, selectedCategory);
            DebugLogger.getInstance().log(Level.INFO, "Player {0} has no selected category. Defaulting to EVERYTHING.", player.getName());
        }

        // Retrieve all active auctions
        List<Auction> auctions = mainGUI.getAuctionHouseHelper().getActiveAuctions();

        // Filter auctions based on the selected category
        List<Auction> filteredAuctions;
        if (selectedCategory == Category.EVERYTHING) { // "Everything" selected
            filteredAuctions = auctions.stream()
                    .filter(auction -> CustomItemUtils.isCustomItem(auction.getItem()))
                    .collect(Collectors.toList());
        } else {
            Category finalSelectedCategory = selectedCategory;
            filteredAuctions = auctions.stream()
                    .filter(auction -> {
                        ItemStack item = auction.getItem();
                        Category category = CustomItemUtils.getCategory(item);
                        return category != null && category == finalSelectedCategory;
                    })
                    .collect(Collectors.toList());
        }

        if (filteredAuctions.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("There are currently no items for sale in this category."));
            DebugLogger.getInstance().log(Level.INFO, "No auctions available for player {0} in category {1}.", new Object[]{player.getName(), selectedCategory.name()});
            // Optionally, still open the GUI with category items
        }

        // Convert auctions into ItemStacks
        List<ItemStack> auctionItems = filteredAuctions.stream().map(auction -> {
            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GRAY + "Current Bid: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getCurrentBid()));
                lore.add(Utils.getInstance().$("Right-click to place a bid"));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getStartingPrice())));
                lore.add(Utils.getInstance().$("Left-click to buy now"));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime() - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));

            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }).collect(Collectors.toList());

        // Handle pagination for Buy GUI
       // PaginationHelper<ItemStack> paginationHelper = handleBuyGuiPagination(player, auctionItems, selectedCategory);
        DebugLogger.getInstance().log(Level.INFO, "PaginationHelper for player {0} in category {1}: Current Page {2} / Total Pages {3}",
                new Object[]{player.getName(), selectedCategory.name(), 1, 1});

        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House - Buy"));

        // Fill the GUI with placeholders only on the outer border, excluding slots for back button and categories
        mainGUI.fillWithPlaceholdersBorderOnly(gui, new HashSet<>(Arrays.asList(BUY_GUI_BACK_SLOT, 0,1,2,3,4,5,6,7,8)));

        // Create and place category items in the first row (slots 0-8)
        mainGUI.createAndPlaceCategoryItems(gui, player, selectedCategory);

        // Get items for the current page
        List<ItemStack> pageItems = auctionItems.subList(1, 1);

        // Place the auction items in the GUI
        for (int i = 0; i < pageItems.size() && i < AUCTION_SLOTS.length; i++) {
            int slot = AUCTION_SLOTS[i];
            gui.setItem(slot, pageItems.get(i));
        }

        // Create the page indicator with lore instructions
        ItemStack pageIndicator = mainGUI.createGuiItem(Material.PAPER,
                ChatColor.GREEN + "Page " + 1 + " of " + 1,
                Arrays.asList(
                        ChatColor.GRAY + "Left-click to go to the previous page",
                        ChatColor.GRAY + "Right-click to go to the next page"
                ));
        gui.setItem(BUY_GUI_PAGE_INDICATOR_SLOT, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        ItemStack backButton = mainGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);
        gui.setItem(BUY_GUI_BACK_SLOT, backButton);

        player.openInventory(gui);
    }


    /**
     * Handles clicks within the Buy GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    public void handleBuyGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        UUID playerId = player.getUniqueId();
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        // Check if the clicked item is a category item
        if (displayName.equals("Everything") || Arrays.stream(CustomItemUtils.getAllCategories())
                .anyMatch(category -> displayName.equals(category.name().replace("_", " ")))) {
            event.setCancelled(true);

            Category selectedCategory;
            if (!displayName.equals("Everything")) {
                // Find the Category by name
                selectedCategory = Arrays.stream(CustomItemUtils.getAllCategories())
                        .filter(category -> displayName.equals(category.name().replace("_", " ")))
                        .findFirst().orElse(Category.EVERYTHING); // Fallback to EVERYTHING
            } else {
                selectedCategory = Category.EVERYTHING;
            }

            // Update the selected category for the player
            mainGUI.getBuyGuiSelectedCategoryMap().put(playerId, selectedCategory);
            DebugLogger.getInstance().log(Level.INFO, "Player {0} switched to category: {1}", new Object[]{player.getName(), selectedCategory.name()});

            // Update the Buy GUI in place
            openBuyGUI(player);

            return;
        }

        // Handle back button click
        if (clickedItem.getType() == Material.ARROW && displayName.equals("Back")) {
            mainGUI.openMainGUI(player);
            event.setCancelled(true);
            return;
        }

        // Handle page indicator clicks
        if (clickedItem.getType() == Material.PAPER && displayName.startsWith("Page")) {
            int clickedSlot = event.getRawSlot();
            ClickType clickType = event.getClick();

            if (clickedSlot == BUY_GUI_PAGE_INDICATOR_SLOT) { // Page Indicator slot
                Category selectedCategory = mainGUI.getBuyGuiSelectedCategoryMap().get(playerId);
                if (selectedCategory == null) {
                    selectedCategory = Category.EVERYTHING; // Default to EVERYTHING
                }

                //PaginationHelper<ItemStack> paginationHelper = mainGUI.getExistingBuyGuiPaginationHelper(player, selectedCategory);

//                if (paginationHelper == null) {
//                    player.sendMessage(Utils.getInstance().$("An error occurred. Please try again."));
//                    DebugLogger.getInstance().log(Level.SEVERE, "PaginationHelper is null for player {0} in category {1}", new Object[]{player.getName(), selectedCategory.name()});
//                    event.setCancelled(true);
//                    return;
//                }

                boolean pageChanged = false;

                if (clickType.isLeftClick()) {
//                    if (paginationHelper.hasPreviousPage()) {
//                        paginationHelper.previousPage();
//                        pageChanged = true;
//                        DebugLogger.getInstance().log(Level.INFO, "Player {0} moved to previous page: {1} in category: {2}",
//                                new Object[]{player.getName(), paginationHelper.getCurrentPage(), selectedCategory.name()});
//                    } else {
//                        player.sendMessage(Utils.getInstance().$("You are already on the first page."));
//                        DebugLogger.getInstance().log(Level.INFO, "Player {0} attempted to go to previous page but is already on the first page in category: {1}",
//                                new Object[]{player.getName(), selectedCategory.name()});
//                    }
                } else if (clickType.isRightClick()) {
//                    if (paginationHelper.hasNextPage()) {
//                        paginationHelper.nextPage();
//                        pageChanged = true;
//                        DebugLogger.getInstance().log(Level.INFO, "Player {0} moved to next page: {1} in category: {2}",
//                                new Object[]{player.getName(), paginationHelper.getCurrentPage(), selectedCategory.name()});
//                    } else {
//                        player.sendMessage(Utils.getInstance().$("You are already on the last page."));
//                        DebugLogger.getInstance().log(Level.INFO, "Player {0} attempted to go to next page but is already on the last page in category: {1}",
//                                new Object[]{player.getName(), selectedCategory.name()});
//                    }
                } else {
                    player.sendMessage(Utils.getInstance().$("Unsupported click type for pagination."));
                    DebugLogger.getInstance().log(Level.WARNING, "Player {0} used unsupported click type: {1} for pagination.",
                            new Object[]{player.getName(), clickType.name()});
                }

                if (pageChanged) {
                    // Update the GUI in-place
                    openBuyGUI(player);
                }

                event.setCancelled(true);
                return;
            }
        }

        // Handle clicks on auction items
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                String auctionIdLine = lore.stream()
                        .filter(line -> ChatColor.stripColor(line).startsWith("Auction ID: "))
                        .findFirst()
                        .orElse(null);

                if (auctionIdLine != null) {
                    String auctionIdString = ChatColor.stripColor(auctionIdLine.replace("Auction ID: ", ""));
                    try {
                        UUID auctionId = UUID.fromString(auctionIdString);
                        Auction auction = mainGUI.getAuctionHouseHelper().getAuctionById(auctionId);
                        if (auction != null) {
                            if (auction.isBidItem()) {
                                ClickType clickType = event.getClick();

                                // Handle bidding
                                if (clickType == ClickType.RIGHT) {
                                    player.closeInventory();
                                    promptBidAmount(player, auctionId);
                                }
                            } else {
                                ClickType clickType = event.getClick();

                                // Handle buy now
                                if (clickType == ClickType.LEFT) {
                                    mainGUI.getAuctionHouseHelper().buyAuction(player, auctionId);
                                    // Refresh the Buy GUI
                                    Bukkit.getScheduler().runTask(mainGUI.getPlugin(), () -> openBuyGUI(player));
                                }
                            }
                        } else {
                            player.sendMessage(Utils.getInstance().$("Auction not found."));
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utils.getInstance().$("Invalid auction ID."));
                    }
                }
            }
        }
    }


//    /**
//     * Handles pagination for the Buy GUI by creating or retrieving a PaginationHelper.
//     *
//     * @param player       The player for whom pagination is handled.
//     * @param auctionItems The list of auction items to paginate.
//     * @param category     The currently selected category.
//     * @return The PaginationHelper instance for the current page.
//     */
//    private PaginationHelper<ItemStack> handleBuyGuiPagination(Player player, List<ItemStack> auctionItems, Category category) {
//        PaginationHelper<ItemStack> paginationHelper = mainGUI.getBuyGuiPaginationHelper(player, category, auctionItems);
//        return paginationHelper;
//    }

    /**
     * Prompts the player to enter a bid amount via chat.
     *
     * @param player    The player placing the bid.
     * @param auctionId The auction ID to bid on.
     */
    private void promptBidAmount(Player player, UUID auctionId) {
        // Add the player to a pending bids map before closing inventory
        mainGUI.getPendingBids().put(player.getUniqueId(), auctionId);

        // Prompt the player to enter a bid amount via chat
        player.sendMessage(Utils.getInstance().$("Enter your bid amount in chat:"));
    }

    /**
     * Formats the remaining time into a human-readable string.
     *
     * @param millis The time in milliseconds.
     * @return A formatted string representing the time left.
     */
    private String formatTimeLeft(long millis) {
        if (millis < 0) {
            return ChatColor.RED + "Expired";
        }
        long seconds = millis / 1000 % 60;
        long minutes = millis / (1000 * 60) % 60;
        long hours = millis / (1000 * 60 * 60) % 24;
        long days = millis / (1000 * 60 * 60 * 24);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }
}
