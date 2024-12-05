package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.PaginationHelper;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages the Auction House GUI, including displaying available auctions,
 * allowing players to buy, sell, and manage their own auctions with pagination support.
 */
public class AuctionsGUI implements Listener {

    private final AuctionHouseHelper auctionHouseHelper;
    private final EconomyHelper economyHelper;
    private final DebugLoggerModule logger;
    private final JavaPlugin plugin;

    // Constants for GUI slot indices
    private static final int MAIN_GUI_BUY_SLOT = 20;
    private static final int MAIN_GUI_SELL_SLOT = 22;
    private static final int MAIN_GUI_MY_AUCTIONS_SLOT = 24;

    private static final int BUY_GUI_BACK_SLOT = 49;
    private static final int BUY_GUI_PAGE_INDICATOR_SLOT = 50;
    private static final int BUY_GUI_NEXT_PAGE_SLOT = 53;

    private static final int SELL_GUI_BACK_SLOT = 49;
    private static final int SELL_GUI_DECREASE_PRICE_SLOT = 19;
    private static final int SELL_GUI_INCREASE_PRICE_SLOT = 25;
    private static final int SELL_GUI_CONFIRM_SLOT = 31;
    private static final int SELL_GUI_CHANGE_DURATION_SLOT = 13;
    private static final int SELL_GUI_PRICE_DISPLAY_SLOT = 28;
    private static final int SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT = 16;
    private static final int SELL_GUI_ITEM_SLOT = 22;

    private static final int YOUR_AUCTIONS_BACK_SLOT = 49;
    private static final int YOUR_AUCTIONS_PAGE_INDICATOR_SLOT = 50;
    private static final int YOUR_AUCTIONS_NEXT_PAGE_SLOT = 53;

    // Temporary storage for price and duration settings per player
    private final Map<UUID, Double> priceMap = new HashMap<>();
    private final Map<UUID, Long> durationMap = new HashMap<>();
    private final Map<UUID, Boolean> bidMap = new HashMap<>();

    // Maps to store pending actions for players
    private final Map<UUID, UUID> pendingBids = new HashMap<>();
    private final Set<UUID> pendingPriceInput = new HashSet<>();

    // Map to store the item the player is selling
    private final Map<UUID, ItemStack> sellingItems = new HashMap<>();

    // Map to store PaginationHelpers per player for Buy GUI
    private final Map<UUID, PaginationHelper<ItemStack>> buyPaginationMap = new HashMap<>();

    // Map to store PaginationHelpers per player for Your Auctions GUI
    private final Map<UUID, PaginationHelper<ItemStack>> yourAuctionsPaginationMap = new HashMap<>();

    /**
     * Constructs the AuctionsGUI with necessary dependencies.
     *
     * @param auctionHouseHelper The AuctionHouseHelper instance.
     * @param economyHelper      The EconomyHelper instance.
     * @param logger             The DebugLoggerModule instance.
     * @param plugin             The main plugin instance.
     */
    public AuctionsGUI(AuctionHouseHelper auctionHouseHelper,
                       EconomyHelper economyHelper,
                       DebugLoggerModule logger,
                       JavaPlugin plugin) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
        this.logger = logger;
        this.plugin = plugin;
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the main Auction House GUI for the player.
     *
     * @param player The player to open the GUI for.
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House"));

        // Fill the GUI with placeholders on **all** slots
        fillAllWithPlaceholders(gui);

        // Create the "Buy Items" item
        ItemStack buyItems = createGuiItem(Material.EMERALD, Utils.getInstance().$("Buy Items"), null);

        // Create the "Sell Items" item
        ItemStack sellItems = createGuiItem(Material.CHEST, Utils.getInstance().$("Sell Items"), null);

        // Create the "My Auctions" item
        ItemStack myAuctions = createGuiItem(Material.BOOK, Utils.getInstance().$("My Auctions"), null);

        // Place the items in the GUI
        gui.setItem(MAIN_GUI_BUY_SLOT, buyItems);
        gui.setItem(MAIN_GUI_SELL_SLOT, sellItems);
        gui.setItem(MAIN_GUI_MY_AUCTIONS_SLOT, myAuctions);

        // Remove PaginationHelpers when opening Main GUI
        removePagination(player.getUniqueId());

        player.openInventory(gui);
    }

    /**
     * Opens the Buy GUI showing available auctions with pagination.
     *
     * @param player The player to open the GUI for.
     */
    public void openBuyGUI(Player player) {
        if (!auctionHouseHelper.areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$("Please wait, auctions are still loading."));
            return;
        }

        List<Auction> auctions = auctionHouseHelper.getActiveAuctions();
        logger.log(Level.INFO, "Opening Buy GUI for player " +
                player.getName() + ". Auctions available: " +
                auctions.size(), 0);

        if (auctions.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("There are currently no items for sale."));
            return;
        }



        // Convert auctions into ItemStacks
        List<ItemStack> auctionItems = new ArrayList<>();
        for (Auction auction : auctions) {
            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GRAY + "Current Bid: $" +
                        economyHelper.formatBalance(auction.getCurrentBid()));
                lore.add(Utils.getInstance().$("Right-click to place a bid"));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        economyHelper.formatBalance(auction.getStartingPrice())));
                lore.add(Utils.getInstance().$("Left-click to buy now"));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime()
                            - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));

            meta.setLore(lore);
            item.setItemMeta(meta);

            auctionItems.add(item);
        }


        // Get or create the PaginationHelper for the player
        PaginationHelper<ItemStack> paginationHelper = buyPaginationMap.get(player.getUniqueId());
        if (buyPaginationMap.containsKey(player.getUniqueId())) {
            logger.log("Creating new PaginationHelper for player IZSBDFOIUHBNSDFIUNSDFIUSBDF" + player.getName());
            paginationHelper = new PaginationHelper<>(auctionItems, 28);
            buyPaginationMap.put(player.getUniqueId(), paginationHelper);
        } else {
            // Update the items in the pagination helper in case the auctions have changed
            paginationHelper.updateItems(auctionItems);
        }

        // dump paginationHelper and everythinjg
        logger.log("PaginationHelper: " + paginationHelper.toString());
        logger.log("PaginationHelper: " + paginationHelper.getCurrentPageItems().toString());
        logger.log("PaginationHelper: " + paginationHelper.getTotalPages());
        logger.log("PaginationHelper: " + paginationHelper.getCurrentPage());


        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House - Buy"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 49 for back button
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(BUY_GUI_BACK_SLOT));

        // Get items for the current page
        List<ItemStack> pageItems = paginationHelper.getCurrentPageItems();

        // Define the inner slots where items will be placed
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        // Place the auction items in the GUI
        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            int slot = slots[i];
            gui.setItem(slot, pageItems.get(i));
        }

        // Create the page indicator with lore instructions
        ItemStack pageIndicator = createGuiItem(Material.PAPER,
                ChatColor.GREEN + "Page " + paginationHelper.getCurrentPage() + " of " + paginationHelper.getTotalPages(),
                Arrays.asList(
                        ChatColor.GRAY + "Left-click to go to the previous page",
                        ChatColor.GRAY + "Right-click to go to the next page"
                ));
        gui.setItem(BUY_GUI_PAGE_INDICATOR_SLOT, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        ItemStack backButton = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);
        gui.setItem(BUY_GUI_BACK_SLOT, backButton);

        player.openInventory(gui);
    }

    /**
     * Opens the Sell GUI for the player to list an item.
     *
     * @param player The player to open the GUI for.
     */
    public void openSellGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House - Sell"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 22 for the item to sell
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(SELL_GUI_ITEM_SLOT));

        // Set default price and duration if not set
        priceMap.putIfAbsent(player.getUniqueId(), 100.0); // Default price
        durationMap.putIfAbsent(player.getUniqueId(), 86400000L); // Default duration (24h)
        bidMap.putIfAbsent(player.getUniqueId(), false); // Default to fixed price

        // Create buttons and placeholders
        ItemStack decreasePrice = createGuiItem(Material.REDSTONE_BLOCK, Utils.getInstance().$("Decrease Price"), null);
        ItemStack increasePrice = createGuiItem(Material.EMERALD_BLOCK, Utils.getInstance().$("Increase Price"), null);
        boolean isBidItem = bidMap.get(player.getUniqueId());
        ItemStack confirm = createGuiItem(Material.GREEN_WOOL, Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")), null);
        ItemStack changeDuration = createGuiItem(Material.CLOCK, Utils.getInstance().$("Change Duration"),
                Collections.singletonList(Utils.getInstance().$("Current Duration: " +
                        formatDuration(durationMap.get(player.getUniqueId())))));
        ItemStack priceDisplay = createGuiItem(Material.PAPER, Utils.getInstance().$("Current Price: $" +
                        economyHelper.formatBalance(priceMap.get(player.getUniqueId()))),
                Collections.singletonList(Utils.getInstance().$("Right-click to set custom price")));
        ItemStack toggleAuctionType = createGuiItem(Material.GOLDEN_HOE, Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")),
                Collections.singletonList(Utils.getInstance().$("Click to switch auction type")));

        // Add a back button
        ItemStack backButton = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);

        // Place items
        gui.setItem(SELL_GUI_DECREASE_PRICE_SLOT, decreasePrice);
        gui.setItem(SELL_GUI_INCREASE_PRICE_SLOT, increasePrice);
        gui.setItem(SELL_GUI_CONFIRM_SLOT, confirm);
        gui.setItem(SELL_GUI_CHANGE_DURATION_SLOT, changeDuration);
        gui.setItem(SELL_GUI_PRICE_DISPLAY_SLOT, priceDisplay);
        gui.setItem(SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT, toggleAuctionType);
        gui.setItem(SELL_GUI_BACK_SLOT, backButton);

        // Place the stored item back into slot 22 if present
        ItemStack itemToSell = sellingItems.get(player.getUniqueId());
        if (itemToSell != null) {
            gui.setItem(SELL_GUI_ITEM_SLOT, itemToSell);
        }

        // Fill the inner slots (excluding slot 22) with placeholders if they are empty
        fillInnerPlaceholders(gui, Arrays.asList(
                SELL_GUI_DECREASE_PRICE_SLOT, SELL_GUI_INCREASE_PRICE_SLOT,
                SELL_GUI_CONFIRM_SLOT, SELL_GUI_CHANGE_DURATION_SLOT,
                SELL_GUI_PRICE_DISPLAY_SLOT, SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT,
                SELL_GUI_BACK_SLOT
        ), SELL_GUI_ITEM_SLOT);

        player.openInventory(gui);
    }

    /**
     * Opens the player's active auctions GUI with pagination.
     *
     * @param player The player to open the GUI for.
     */
    public void openPlayerAuctionsGUI(Player player) {
        if (!auctionHouseHelper.areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$("Please wait, auctions are still loading."));
            return;
        }

        List<Auction> playerAuctions = auctionHouseHelper.getPlayerAuctions(player.getUniqueId());

        if (playerAuctions.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("You have no active auctions."));
            return;
        }

        // Convert player's auctions into ItemStacks
        List<ItemStack> auctionItems = new ArrayList<>();
        for (Auction auction : playerAuctions) {
            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GRAY + "Current Bid: $" +
                        economyHelper.formatBalance(auction.getCurrentBid()));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        economyHelper.formatBalance(auction.getStartingPrice())));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime()
                            - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));
            lore.add(Utils.getInstance().$("Click to cancel this auction"));

            meta.setLore(lore);
            item.setItemMeta(meta);

            auctionItems.add(item);
        }

        // Get or create the PaginationHelper for the player
        PaginationHelper<ItemStack> paginationHelper = yourAuctionsPaginationMap.get(player.getUniqueId());
        if (paginationHelper == null) {
            paginationHelper = new PaginationHelper<>(auctionItems, 28);
            yourAuctionsPaginationMap.put(player.getUniqueId(), paginationHelper);
        } else {
            // Update the items in the pagination helper in case the auctions have changed
            paginationHelper.updateItems(auctionItems);
        }

        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Your Auctions"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 49 for back button
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(YOUR_AUCTIONS_BACK_SLOT));

        // Get items for the current page
        List<ItemStack> pageItems = paginationHelper.getCurrentPageItems();

        // Define the inner slots where items will be placed
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        // Place the auction items in the GUI
        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            int slot = slots[i];
            gui.setItem(slot, pageItems.get(i));
        }

        // Create the page indicator with lore instructions
        ItemStack pageIndicator = createGuiItem(Material.PAPER,
                ChatColor.GREEN + "Page " + paginationHelper.getCurrentPage() + " of " + paginationHelper.getTotalPages(),
                Arrays.asList(
                        ChatColor.GRAY + "Left-click to go to the previous page",
                        ChatColor.GRAY + "Right-click to go to the next page"
                ));
        gui.setItem(YOUR_AUCTIONS_PAGE_INDICATOR_SLOT, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        ItemStack backButton = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);
        gui.setItem(YOUR_AUCTIONS_BACK_SLOT, backButton);

        player.openInventory(gui);
    }

    /**
     * Handles clicks within the Auction House GUIs.
     *
     * @param event The InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        ItemStack clickedItem = event.getCurrentItem();

        switch (inventoryTitle) {
            case "Auction House":
                logger.log("Player " + player.getName() + " clicked in the Auction House menu.");
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                handleMainGUIClick(event, player, clickedItem);
                break;
            case "Auction House - Buy":
                logger.log("Player " + player.getName() + " clicked in the Auction House - Buy menu.");
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                handleBuyGUIClick(event, player, clickedItem);
                break;
            case "Auction House - Sell":
                logger.log("Player " + player.getName() + " clicked in the Auction House - Sell menu.");
                handleSellGUIClick(event, player);
                break;
            case "Your Auctions":
                logger.log("Player " + player.getName() + " clicked in the Your Auctions menu.");
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                handlePlayerAuctionsGUIClick(event, player, clickedItem);
                break;
            default:
                break;
        }
    }

    /**
     * Handles dragging items within the Auction House GUIs.
     *
     * @param event The InventoryDragEvent.
     */
    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent event) {
        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        if ("Auction House - Sell".equals(inventoryTitle)) {
            // Allow dragging only if the drag involves slot 22
            if (event.getRawSlots().contains(SELL_GUI_ITEM_SLOT)) {
                // Update the sellingItems map after the event has processed
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ItemStack item = event.getView().getTopInventory().getItem(SELL_GUI_ITEM_SLOT);
                    if (item != null && item.getType() != Material.AIR) {
                        sellingItems.put(event.getWhoClicked().getUniqueId(), item.clone());
                    } else {
                        sellingItems.remove(event.getWhoClicked().getUniqueId());
                    }
                }, 1L); // Delay by 1 tick to ensure the event has processed
            } else {
                event.setCancelled(true);
            }
        } else {
            // Cancel dragging in other Auction House GUIs
            event.setCancelled(true);
        }
    }

    /**
     * Handles player chat input for bidding and custom price setting.
     *
     * @param event The AsyncPlayerChatEvent.
     */
    @EventHandler
    public void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (pendingBids.containsKey(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            UUID auctionId = pendingBids.remove(playerId);

            try {
                double bidAmount = Double.parseDouble(message);
                auctionHouseHelper.placeBid(player, auctionId, bidAmount);
                // Reopen Buy GUI
                Bukkit.getScheduler().runTask(plugin, () -> openBuyGUI(player));
            } catch (NumberFormatException e) {
                player.sendMessage(Utils.getInstance().$("Invalid bid amount. Please enter a number."));
            }
        } else if (pendingPriceInput.contains(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            pendingPriceInput.remove(playerId);

            try {
                double customPrice = Double.parseDouble(message);
                if (customPrice < 0) {
                    player.sendMessage(Utils.getInstance().$("Price cannot be negative."));
                    return;
                }
                priceMap.put(playerId, customPrice);
                player.sendMessage(Utils.getInstance().$("Custom price set to $" + economyHelper.formatBalance(customPrice)));
                // Reopen Sell GUI
                Bukkit.getScheduler().runTask(plugin, () -> openSellGUI(player));
            } catch (NumberFormatException e) {
                player.sendMessage(Utils.getInstance().$("Invalid price. Please enter a number."));
            }
        }
    }

    /**
     * Handles clicks on the main Auction House GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleMainGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        if ("Buy Items".equalsIgnoreCase(displayName)) {
            openBuyGUI(player);
        } else if ("Sell Items".equalsIgnoreCase(displayName)) {
            openSellGUI(player);
        } else if ("My Auctions".equalsIgnoreCase(displayName)) {
            openPlayerAuctionsGUI(player);
        }
    }

    /**
     * Handles clicks on the Buy GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleBuyGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        // Handle the back button click
        if (clickedItem.getType() == Material.ARROW && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Back")) {
            openMainGUI(player);
            return;
        }

        // Handle page indicator clicks
        if (clickedItem.getType() == Material.PAPER && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).startsWith("Page")) {
            int clickedSlot = event.getRawSlot();
            ClickType clickType = event.getClick();

            if (clickedSlot == BUY_GUI_PAGE_INDICATOR_SLOT) { // Page Indicator slot
                PaginationHelper<ItemStack> paginationHelper = buyPaginationMap.get(player.getUniqueId());
                if (paginationHelper == null) {
                    logger.log("Player " + player.getName() + " clicked on Buy GUI pagination without a PaginationHelper.");
                    player.sendMessage(Utils.getInstance().$("An error occurred. Please try again."));

                    return;
                }

                // **Debug Logging for Click Types**
                logger.log("Player " + player.getName() + " clicked on Buy GUI pagination with ClickType: " + clickType.name());

                if (clickType.isLeftClick()) {
                    logger.log("Handling LEFT click for pagination in Buy GUI.");
                    if (paginationHelper.hasPreviousPage()) {
                        paginationHelper.previousPage();
                        logger.log("PaginationHelper updated to previous page: " + paginationHelper.getCurrentPage());
                        openBuyGUI(player);
                    } else {
                        logger.log("Player " + player.getName() + " is already on the first page in Buy GUI.");
                        player.sendMessage(Utils.getInstance().$("You are already on the first page."));
                    }
                } else if (clickType.isRightClick()) {
                    logger.log("Handling RIGHT click for pagination in Buy GUI.");
                    if (paginationHelper.hasNextPage()) {
                        paginationHelper.nextPage();
                        logger.log("PaginationHelper updated to next page: " + paginationHelper.getCurrentPage());
                        openBuyGUI(player);
                    } else {
                        logger.log("Player " + player.getName() + " is already on the last page in Buy GUI.");
                        player.sendMessage(Utils.getInstance().$("You are already on the last page."));
                    }
                } else {
                    logger.log("Clicked pagination item with unsupported ClickType: " + clickType.name() + " in Buy GUI.");
                    player.sendMessage(Utils.getInstance().$("Unsupported click type for pagination."));
                }

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
                        Auction auction = auctionHouseHelper.getAuctionById(auctionId);
                        if (auction != null) {
                            if (auction.isBidItem()) {
                                ClickType clickType = event.getClick();

                                // Handle bidding
                                if (clickType == ClickType.RIGHT) {
                                    logger.log("Player " + player.getName() + " is placing a bid on auction ID: " + auctionId);
                                    player.closeInventory();
                                    promptBidAmount(player, auctionId);
                                }
                            } else {
                                ClickType clickType = event.getClick();

                                // Handle buy now
                                if (clickType == ClickType.LEFT) {
                                    logger.log("Player " + player.getName() + " is buying now auction ID: " + auctionId);
                                    auctionHouseHelper.buyAuction(player, auctionId);
                                    // Refresh the Buy GUI
                                    Bukkit.getScheduler().runTask(plugin, () -> openBuyGUI(player));
                                }
                            }
                        } else {
                            logger.log("Auction not found for ID: " + auctionId + " clicked by player " + player.getName());
                            player.sendMessage(Utils.getInstance().$("Auction not found."));
                        }
                    } catch (IllegalArgumentException e) {
                        logger.log("Player " + player.getName() + " clicked an auction with invalid ID: " + auctionIdLine);
                        player.sendMessage(Utils.getInstance().$("Invalid auction ID."));
                    }
                }
            }
        }
    }

    /**
     * Handles clicks on the Sell GUI.
     *
     * @param event  The InventoryClickEvent.
     * @param player The player who clicked.
     */
    private void handleSellGUIClick(InventoryClickEvent event, Player player) {
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        // Prevent interacting with the GUI if it's not the top inventory
        if (!event.getView().getTopInventory().equals(clickedInventory)) return;

        if (slot == SELL_GUI_ITEM_SLOT) {
            // Allow normal clicking in slot 22 (item to sell)
            event.setCancelled(false);
            // Update the sellingItems map after the event has processed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack item = event.getView().getTopInventory().getItem(SELL_GUI_ITEM_SLOT);
                if (item != null && item.getType() != Material.AIR) {
                    sellingItems.put(player.getUniqueId(), item.clone());
                } else {
                    sellingItems.remove(player.getUniqueId());
                }
            }, 1L); // Delay by 1 tick to ensure the event has processed
            return;
        }

        // Handle shift-clicks from player inventory to GUI
        if (event.isShiftClick() && clickedInventory.equals(player.getInventory())) {
            ItemStack itemToSell = event.getCurrentItem();
            if (itemToSell != null && itemToSell.getType() != Material.AIR) {
                // Place the item into slot 22
                event.getView().getTopInventory().setItem(SELL_GUI_ITEM_SLOT, itemToSell.clone());
                player.getInventory().removeItem(itemToSell.clone());

                // Update the sellingItems map
                sellingItems.put(player.getUniqueId(), itemToSell.clone());
            }
            event.setCancelled(true);
            return;
        }

        // Handle clicks on GUI items
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            event.setCancelled(true);
            return;
        }

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        if (displayName == null) return;

        // Check for dynamic display names
        if (displayName.startsWith("Current Price: $")) {
            if (clickType == ClickType.RIGHT) {
                // Store the item in slot 22
                ItemStack itemToSell = event.getView().getTopInventory().getItem(SELL_GUI_ITEM_SLOT);
                if (itemToSell != null && itemToSell.getType() != Material.AIR) {
                    sellingItems.put(player.getUniqueId(), itemToSell.clone());
                }
                // Add to pendingPriceInput before closing inventory
                pendingPriceInput.add(player.getUniqueId());
                player.closeInventory();
                promptCustomPrice(player);
            }
        } else {
            switch (displayName) {
                case "Decrease Price":
                    decreasePrice(player, event.getView().getTopInventory());
                    break;
                case "Increase Price":
                    increasePrice(player, event.getView().getTopInventory());
                    break;
                case "Confirm Sale":
                case "Confirm Auction":
                    confirmSale(player, event.getView().getTopInventory());
                    break;
                case "Change Duration":
                    changeDuration(player, event.getView().getTopInventory());
                    break;
                case "Auction Type: Fixed Price":
                case "Auction Type: Bidding":
                    toggleAuctionType(player, event.getView().getTopInventory());
                    break;
                case "Back":
                    openMainGUI(player);
                    break;
                default:
                    break;
            }
        }

        event.setCancelled(true);
    }

    /**
     * Handles clicks on the Player's Auctions GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handlePlayerAuctionsGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        // Handle the back button click
        if (clickedItem.getType() == Material.ARROW && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Back")) {
            openMainGUI(player);
            return;
        }

        // Handle page indicator clicks
        if (clickedItem.getType() == Material.PAPER && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).startsWith("Page")) {
            int clickedSlot = event.getRawSlot();
            ClickType clickType = event.getClick();

            if (clickedSlot == YOUR_AUCTIONS_PAGE_INDICATOR_SLOT) { // Page Indicator slot
                PaginationHelper<ItemStack> paginationHelper = yourAuctionsPaginationMap.get(player.getUniqueId());
                if (paginationHelper == null) return;

                if (clickType.isLeftClick()) {
                    if (paginationHelper.hasPreviousPage()) {
                        paginationHelper.previousPage();
                        openPlayerAuctionsGUI(player);
                    } else {
                        player.sendMessage(Utils.getInstance().$("You are already on the first page."));
                    }
                } else if (clickType.isRightClick()) {
                    if (paginationHelper.hasNextPage()) {
                        paginationHelper.nextPage();
                        openPlayerAuctionsGUI(player);
                    } else {
                        player.sendMessage(Utils.getInstance().$("You are already on the last page."));
                    }
                }
                return;
            }
        }

        // Handle clicks on auction items to cancel them
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
                        // Cancel the auction
                        auctionHouseHelper.cancelAuction(auctionId, player);
                        // Refresh the player's auctions GUI
                        Bukkit.getScheduler().runTask(plugin, () -> openPlayerAuctionsGUI(player));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utils.getInstance().$("Invalid auction ID."));
                    }
                }
            }
        }
    }

    /**
     * Decreases the current price in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void decreasePrice(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        double currentPrice = priceMap.getOrDefault(playerUUID, 100.0);
        currentPrice = Math.max(0, currentPrice - 10); // Decrease by $10, minimum $0
        priceMap.put(playerUUID, currentPrice);

        updatePriceDisplay(sellGui, currentPrice);
    }

    /**
     * Increases the current price in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void increasePrice(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        double currentPrice = priceMap.getOrDefault(playerUUID, 100.0);
        currentPrice += 10; // Increase by $10
        priceMap.put(playerUUID, currentPrice);

        updatePriceDisplay(sellGui, currentPrice);
    }

    /**
     * Updates the price display in the Sell GUI.
     *
     * @param sellGui      The Sell GUI inventory.
     * @param currentPrice The current price to display.
     */
    private void updatePriceDisplay(Inventory sellGui, double currentPrice) {
        ItemStack priceDisplay = createGuiItem(Material.PAPER, Utils.getInstance().$("Current Price: $" +
                        economyHelper.formatBalance(currentPrice)),
                Collections.singletonList(Utils.getInstance().$("Right-click to set custom price")));

        sellGui.setItem(SELL_GUI_PRICE_DISPLAY_SLOT, priceDisplay);
    }

    /**
     * Confirms the sale or auction in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void confirmSale(Player player, Inventory sellGui) {
        ItemStack itemToSell = sellGui.getItem(SELL_GUI_ITEM_SLOT);
        if (itemToSell == null || itemToSell.getType() == Material.AIR) {
            player.sendMessage(Utils.getInstance().$("You must place an item in the center slot to sell."));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        double price = priceMap.getOrDefault(playerUUID, 100.0);
        long duration = durationMap.getOrDefault(playerUUID, 86400000L); // Default 24h
        boolean isBidItem = bidMap.getOrDefault(playerUUID, false);

        // Remove the item from the GUI
        sellGui.setItem(SELL_GUI_ITEM_SLOT, null);

        // Remove the item from the sellingItems map
        sellingItems.remove(playerUUID);

        // Add the auction
        if (isBidItem) {
            auctionHouseHelper.addBidAuction(playerUUID, itemToSell, price, duration);
        } else {
            auctionHouseHelper.addAuction(playerUUID, itemToSell, price, duration);
        }

        player.sendMessage(Utils.getInstance().$("Your item has been listed for " + (isBidItem ? "auction!" : "sale!")));

        // Close the GUI
        player.closeInventory();

        // Open "My Auctions" GUI
        Bukkit.getScheduler().runTask(plugin, () -> openPlayerAuctionsGUI(player));
    }

    /**
     * Changes the duration of the auction in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void changeDuration(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        long currentDuration = durationMap.getOrDefault(playerUUID, 86400000L);
        // Cycle through durations: 1h, 6h, 12h, 24h
        if (currentDuration == 3600000L) {
            currentDuration = 21600000L; // 6h
        } else if (currentDuration == 21600000L) {
            currentDuration = 43200000L; // 12h
        } else if (currentDuration == 43200000L) {
            currentDuration = 86400000L; // 24h
        } else {
            currentDuration = 3600000L; // 1h
        }
        durationMap.put(playerUUID, currentDuration);

        // Update the duration display
        ItemStack changeDuration = createGuiItem(Material.CLOCK, Utils.getInstance().$("Change Duration"),
                Collections.singletonList(Utils.getInstance().$("Current Duration: " + formatDuration(currentDuration))));

        sellGui.setItem(SELL_GUI_CHANGE_DURATION_SLOT, changeDuration);
    }

    /**
     * Formats the duration from milliseconds to a human-readable string.
     *
     * @param millis The duration in milliseconds.
     * @return A formatted string representing the duration.
     */
    private String formatDuration(long millis) {
        long hours = millis / 3600000L;
        return hours + "h";
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

    /**
     * Toggles the auction type between fixed price and bidding in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void toggleAuctionType(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        boolean isBidItem = !bidMap.getOrDefault(playerUUID, false);
        bidMap.put(playerUUID, isBidItem);

        // Update the toggle button
        ItemStack toggleAuctionType = createGuiItem(Material.GOLDEN_HOE,
                Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")),
                Collections.singletonList(Utils.getInstance().$("Click to switch auction type")));
        sellGui.setItem(SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT, toggleAuctionType);

        // Update the Confirm button display name to reflect auction type
        ItemStack confirm = createGuiItem(Material.GREEN_WOOL,
                Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")),
                null);
        sellGui.setItem(SELL_GUI_CONFIRM_SLOT, confirm);
    }

    /**
     * Prompts the player to enter a bid amount via chat.
     *
     * @param player    The player placing the bid.
     * @param auctionId The auction ID to bid on.
     */
    private void promptBidAmount(Player player, UUID auctionId) {
        // Add the player to a pending bids map before closing inventory
        pendingBids.put(player.getUniqueId(), auctionId);

        // Prompt the player to enter a bid amount via chat
        player.sendMessage(Utils.getInstance().$("Enter your bid amount in chat:"));
    }

    /**
     * Prompts the player to enter a custom price via chat.
     *
     * @param player The player setting a custom price.
     */
    private void promptCustomPrice(Player player) {
        // Player is already added to pendingPriceInput before this method is called

        // Prompt the player to enter a custom price via chat
        player.sendMessage(Utils.getInstance().$("Enter your custom price in chat:"));
    }

    /**
     * Creates a generic GUI item with a display name and optional lore.
     *
     * @param material    The material of the item.
     * @param displayName The display name of the item.
     * @param lore        The lore of the item (can be null).
     * @return The customized ItemStack.
     */
    private ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        if (displayName != null) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Fills the entire inventory with black stained glass panes as placeholders.
     *
     * @param inventory The inventory to fill.
     */
    private void fillAllWithPlaceholders(Inventory inventory) {
        ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Fills the inventory with placeholders only on the outer border.
     *
     * @param inventory    The inventory to fill.
     * @param excludeSlots Set of slot indices to exclude from being filled with placeholders.
     */
    private void fillWithPlaceholdersBorderOnly(Inventory inventory, Set<Integer> excludeSlots) {
        ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Define border slots
        Set<Integer> borderSlots = new HashSet<>();

        // Top and Bottom rows
        for (int i = 0; i < 9; i++) {
            borderSlots.add(i); // Top row
            borderSlots.add(45 + i); // Bottom row
        }

        // Left and Right columns for middle rows
        for (int row = 1; row <= 4; row++) {
            borderSlots.add(row * 9); // Left column
            borderSlots.add(row * 9 + 8); // Right column
        }

        for (int i : borderSlots) {
            if (!excludeSlots.contains(i) && (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir())) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Fills the inner non-button slots with placeholders, excluding specified slots.
     *
     * @param inventory         The inventory to fill.
     * @param occupiedSlots     List of slot indices that are occupied by interactive buttons.
     * @param itemSlotExcluded  Slot index that should remain free (e.g., slot 22 in Sell GUI).
     */
    private void fillInnerPlaceholders(Inventory inventory, List<Integer> occupiedSlots, int itemSlotExcluded) {
        ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Define inner slots (excluding border)
        List<Integer> innerSlots = new ArrayList<>();
        for (int i = 9; i < 45; i++) { // Middle 4 rows
            if ((i % 9 != 0) && (i % 9 != 8)) { // Exclude left and right borders
                innerSlots.add(i);
            }
        }

        for (int slot : innerSlots) {
            if (slot != itemSlotExcluded && !occupiedSlots.contains(slot)) {
                if (inventory.getItem(slot) == null || inventory.getItem(slot).getType().isAir()) {
                    inventory.setItem(slot, placeholder);
                }
            }
        }
    }

    /**
     * Removes PaginationHelpers associated with a player.
     *
     * @param playerId The UUID of the player.
     */
    public void removePagination(UUID playerId) {
        buyPaginationMap.remove(playerId);
        yourAuctionsPaginationMap.remove(playerId);
    }

    /**
     * Checks if a player is pending a custom price input.
     *
     * @param playerId The UUID of the player.
     * @return True if the player is pending a custom price input, false otherwise.
     */
    public boolean isPendingPriceInput(UUID playerId) {
        return pendingPriceInput.contains(playerId);
    }

    /**
     * Removes the selling item associated with a player.
     *
     * @param playerId The UUID of the player.
     */
    public void removeSellingItem(UUID playerId) {
        sellingItems.remove(playerId);
    }
}
