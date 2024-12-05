package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
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

public class AuctionsGUI {

    private final AuctionHouseHelper auctionHouseHelper;
    private final EconomyHelper economyHelper;
    private final DebugLoggerModule logger;
    private MysticCore plugin;

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
    private final Map<UUID, PaginationHelper> buyPaginationMap = new HashMap<>();

    // Map to store PaginationHelpers per player for Your Auctions GUI
    private final Map<UUID, PaginationHelper> yourAuctionsPaginationMap = new HashMap<>();

    public AuctionsGUI(AuctionHouseHelper auctionHouseHelper,
                       EconomyHelper economyHelper,
                       DebugLoggerModule logger) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
        this.logger = logger;
    }

    public void setPlugin(MysticCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main Auction House GUI.
     *
     * @param player The player to open the GUI for.
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Utils.getInstance().$("Auction House"));

        // Fill the GUI with placeholders on **all** slots
        fillAllWithPlaceholders(gui);

        // Create the "Buy Items" item
        ItemStack buyItems = new ItemStack(Material.EMERALD);
        ItemMeta buyMeta = buyItems.getItemMeta();
        buyMeta.setDisplayName(Utils.getInstance().$("Buy Items"));
        buyItems.setItemMeta(buyMeta);

        // Create the "Sell Items" item
        ItemStack sellItems = new ItemStack(Material.CHEST);
        ItemMeta sellMeta = sellItems.getItemMeta();
        sellMeta.setDisplayName(Utils.getInstance().$("Sell Items"));
        sellItems.setItemMeta(sellMeta);

        // Create the "My Auctions" item
        ItemStack myAuctions = new ItemStack(Material.BOOK);
        ItemMeta myAuctionsMeta = myAuctions.getItemMeta();
        myAuctionsMeta.setDisplayName(Utils.getInstance().$("My Auctions"));
        myAuctions.setItemMeta(myAuctionsMeta);

        // Place the items in the GUI
        gui.setItem(20, buyItems);
        gui.setItem(22, sellItems);
        gui.setItem(24, myAuctions);

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
            List<String> lore = meta.hasLore() ? meta.getLore()
                    : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add("Current Bid: $" +
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
        PaginationHelper paginationHelper = buyPaginationMap.get(player.getUniqueId());
        if (paginationHelper == null) {
            paginationHelper = new PaginationHelper(auctionItems, 28);
            buyPaginationMap.put(player.getUniqueId(), paginationHelper);
        } else {
            // Update the items in the pagination helper in case the auctions have changed
            paginationHelper.updateItems(auctionItems);
        }

        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54,
                Utils.getInstance().$("Auction House - Buy"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 49 for back button
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(49));

        // Get items for the current page
        List<ItemStack> pageItems = paginationHelper.getPageItems();

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
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.setDisplayName(Utils.getInstance().$("Page " + (paginationHelper.getCurrentPage() + 1) + " of " + paginationHelper.getTotalPages()));
        pageMeta.setLore(Arrays.asList(
                Utils.getInstance().$("Left-click to go to the previous page"),
                Utils.getInstance().$("Right-click to go to the next page")
        ));
        pageIndicator.setItemMeta(pageMeta);
        gui.setItem(50, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        addBackButton(gui);

        player.openInventory(gui);
    }

    /**
     * Opens the Sell GUI for the player to list an item.
     *
     * @param player The player to open the GUI for.
     */
    public void openSellGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Utils.getInstance().$("Auction House - Sell"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 22 for the item to sell
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(22));

        // Set default price and duration if not set
        priceMap.putIfAbsent(player.getUniqueId(), 100.0); // Default price
        durationMap.putIfAbsent(player.getUniqueId(), 86400000L); // Default duration (24h)
        bidMap.putIfAbsent(player.getUniqueId(), false); // Default to fixed price

        // Create buttons and placeholders
        ItemStack decreasePrice = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta decreaseMeta = decreasePrice.getItemMeta();
        decreaseMeta.setDisplayName(Utils.getInstance().$("Decrease Price"));
        decreasePrice.setItemMeta(decreaseMeta);

        ItemStack increasePrice = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta increaseMeta = increasePrice.getItemMeta();
        increaseMeta.setDisplayName(Utils.getInstance().$("Increase Price"));
        increasePrice.setItemMeta(increaseMeta);

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        boolean isBidItem = bidMap.get(player.getUniqueId());
        confirmMeta.setDisplayName(Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")));
        confirm.setItemMeta(confirmMeta);

        ItemStack changeDuration = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = changeDuration.getItemMeta();
        durationMeta.setDisplayName(Utils.getInstance().$("Change Duration"));
        durationMeta.setLore(List.of(Utils.getInstance().$("Current Duration: " +
                formatDuration(durationMap.get(player.getUniqueId())))));
        changeDuration.setItemMeta(durationMeta);

        // Create price display
        ItemStack priceDisplay = new ItemStack(Material.PAPER);
        ItemMeta priceMeta = priceDisplay.getItemMeta();
        priceMeta.setDisplayName(Utils.getInstance().$("Current Price: $" +
                economyHelper.formatBalance(priceMap.get(player.getUniqueId()))));
        priceMeta.setLore(List.of(Utils.getInstance().$("Right-click to set custom price")));
        priceDisplay.setItemMeta(priceMeta);

        // Create "Toggle Auction Type" button
        ItemStack toggleAuctionType = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta toggleMeta = toggleAuctionType.getItemMeta();
        toggleMeta.setDisplayName(Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")));
        toggleMeta.setLore(List.of(Utils.getInstance().$("Click to switch auction type")));
        toggleAuctionType.setItemMeta(toggleMeta);

        // Add a back button
        addBackButton(gui);

        // Place items
        gui.setItem(19, decreasePrice);
        gui.setItem(25, increasePrice);
        gui.setItem(31, confirm);
        gui.setItem(13, changeDuration);
        gui.setItem(28, priceDisplay);
        gui.setItem(16, toggleAuctionType);

        // Place the stored item back into slot 22 if present
        ItemStack itemToSell = sellingItems.get(player.getUniqueId());
        if (itemToSell != null) {
            gui.setItem(22, itemToSell);
        }
        // Fill the inner slots (excluding slot 22) with placeholders if they are empty
        fillInnerPlaceholders(gui, Arrays.asList(
                19, 25, 31, 13, 28, 16, 49 // Slots occupied by buttons and back button
        ), 22);

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
            List<String> lore = meta.hasLore() ? meta.getLore()
                    : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(Utils.getInstance().$("Current Bid: $" +
                        economyHelper.formatBalance(auction.getCurrentBid())));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        economyHelper.formatBalance(auction.getStartingPrice())));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime()
                            - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Click to cancel this auction"));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));

            meta.setLore(lore);
            item.setItemMeta(meta);

            auctionItems.add(item);
        }

        // Get or create the PaginationHelper for the player
        PaginationHelper paginationHelper = yourAuctionsPaginationMap.get(player.getUniqueId());
        if (paginationHelper == null) {
            paginationHelper = new PaginationHelper(auctionItems, 28);
            yourAuctionsPaginationMap.put(player.getUniqueId(), paginationHelper);
        } else {
            // Update the items in the pagination helper in case the auctions have changed
            paginationHelper.updateItems(auctionItems);
        }

        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54,
                Utils.getInstance().$("Your Auctions"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 49 for back button
        fillWithPlaceholdersBorderOnly(gui, Collections.singleton(49));

        // Get items for the current page
        List<ItemStack> pageItems = paginationHelper.getPageItems();

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
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.setDisplayName(Utils.getInstance().$("Page " + (paginationHelper.getCurrentPage() + 1) + " of " + paginationHelper.getTotalPages()));
        pageMeta.setLore(Arrays.asList(
                Utils.getInstance().$("Left-click to go to the previous page"),
                Utils.getInstance().$("Right-click to go to the next page")
        ));
        pageIndicator.setItemMeta(pageMeta);
        gui.setItem(50, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        addBackButton(gui);

        player.openInventory(gui);
    }

    /**
     * Formats the remaining time into a human-readable string.
     *
     * @param millis The time in milliseconds.
     * @return A formatted string representing the time left.
     */
    private String formatTimeLeft(long millis) {
        if (millis < 0) {
            return "Expired";
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
     * Fills the entire inventory with black stained glass panes as placeholders.
     * Use this for the Main GUI where all inner slots should have placeholders.
     *
     * @param inventory The inventory to fill.
     */
    private void fillAllWithPlaceholders(Inventory inventory) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            // Avoid overwriting special buttons/items by checking if the slot is already occupied
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Fills the empty slots of the inventory with black stained glass panes only on the outer border.
     * Use this for Buy, Sell, and Your Auctions GUIs where inner slots are meant for dynamic content.
     * Optionally excludes specific slots from being filled with placeholders.
     *
     * @param inventory    The inventory to fill.
     * @param excludeSlots Set of slot indices to exclude from being filled with placeholders.
     */
    private void fillWithPlaceholdersBorderOnly(Inventory inventory, Set<Integer> excludeSlots) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

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

        for (int i = 0; i < inventory.getSize(); i++) {
            if (borderSlots.contains(i) && !excludeSlots.contains(i) && (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir())) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Fills the inner non-button slots with placeholders, excluding specified slots.
     *
     * @param inventory        The inventory to fill.
     * @param occupiedSlots    List of slot indices that are occupied by interactive buttons.
     * @param itemSlotExcluded Slot index that should remain free (e.g., slot 22 in Sell GUI).
     */
    private void fillInnerPlaceholders(Inventory inventory, List<Integer> occupiedSlots, int itemSlotExcluded) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

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
     * Adds a back button to the inventory.
     *
     * @param inventory The inventory to add the back button to.
     */
    private void addBackButton(Inventory inventory) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(Utils.getInstance().$("Back"));
        backButton.setItemMeta(backMeta);

        inventory.setItem(49, backButton); // Slot 49 is bottom center in a 54-slot inventory
    }

    /**
     * Handles clicks within the Auction House GUIs.
     *
     * @param event The InventoryClickEvent.
     */
    public void onInventoryClick(InventoryClickEvent event) {
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
    public void onInventoryDrag(InventoryDragEvent event) {
        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        if ("Auction House - Sell".equals(inventoryTitle)) {
            // Allow dragging only if the drag involves slot 22
            if (event.getRawSlots().contains(22)) {
                // Update the sellingItems map after the event has processed
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ItemStack item = event.getView().getTopInventory().getItem(22);
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

    private void handleBuyGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        // Handle the back button click
        if (clickedItem.getType() == Material.ARROW && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Back")) {
            logger.log("Player " + player.getName() + " clicked the Back button in Buy GUI.");
            openMainGUI(player);
            return;
        }

        // Handle page indicator clicks
        if (clickedItem.getType() == Material.PAPER && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).startsWith("Page")) {
            int clickedSlot = event.getRawSlot();
            ClickType clickType = event.getClick();

            if (clickedSlot == 50) { // Page Indicator slot
                PaginationHelper paginationHelper = buyPaginationMap.get(player.getUniqueId());
                if (paginationHelper == null) {
                    logger.log("PaginationHelper is null for player " + player.getName() + " in Buy GUI.");
                    player.sendMessage(Utils.getInstance().$("An error occurred while handling pagination. Please try again."));
                    return;
                }

                // **Debug Logging for Click Types**
                logger.log("Player " + player.getName() + " clicked on Buy GUI pagination in slot 50 with ClickType: " + clickType.name());

                if (clickType.isLeftClick()) {
                    logger.log("Handling LEFT click for pagination in Buy GUI.");
                    if (paginationHelper.hasPreviousPage()) {
                        paginationHelper.previousPage();
                        logger.log("PaginationHelper updated to previous page: " + (paginationHelper.getCurrentPage() + 1));
                        openBuyGUI(player);
                    } else {
                        logger.log("Player " + player.getName() + " is already on the first page in Buy GUI.");
                        player.sendMessage(Utils.getInstance().$("You are already on the first page."));
                    }
                } else if (clickType.isRightClick()) {
                    logger.log("Handling RIGHT click for pagination in Buy GUI.");
                    if (paginationHelper.hasNextPage()) {
                        paginationHelper.nextPage();
                        logger.log("PaginationHelper updated to next page: " + (paginationHelper.getCurrentPage() + 1));
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


    private void handleSellGUIClick(InventoryClickEvent event, Player player) {
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        if (event.isShiftClick()) {
            // Handle shift-clicks
            if (event.getClickedInventory().equals(player.getInventory())) {
                // Shift-clicking from player inventory to GUI
                Inventory sellGui = event.getView().getTopInventory();
                if (sellGui.getItem(22) == null || sellGui.getItem(22).getType() == Material.AIR) {
                    // Place the item into slot 22
                    sellGui.setItem(22, event.getCurrentItem().clone());
                    event.getClickedInventory().setItem(event.getSlot(), null);

                    // Update the sellingItems map
                    sellingItems.put(player.getUniqueId(), event.getCurrentItem().clone());
                }
                event.setCancelled(true);
            } else {
                // Prevent shift-clicking in the GUI
                event.setCancelled(true);
            }
        } else if (slot == 22) {
            // Allow normal clicking in slot 22
            event.setCancelled(false);
            // Update the sellingItems map after the event has processed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack item = event.getInventory().getItem(22);
                if (item != null && item.getType() != Material.AIR) {
                    sellingItems.put(player.getUniqueId(), item.clone());
                } else {
                    sellingItems.remove(player.getUniqueId());
                }
            }, 1L); // Delay by 1 tick to ensure the event has processed
        } else if (event.getClickedInventory().equals(player.getInventory())) {
            // Allow normal interaction with player inventory
            event.setCancelled(false);
        } else {
            // Handle clicks on GUI items
            event.setCancelled(true);
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }
            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            if (displayName == null) return;

            // Check for dynamic display names
            if (displayName.startsWith("Current Price: $")) {
                if (clickType == ClickType.RIGHT) {
                    // Store the item in slot 22
                    ItemStack itemToSell = event.getInventory().getItem(22);
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
                        decreasePrice(player, event.getInventory());
                        break;
                    case "Increase Price":
                        increasePrice(player, event.getInventory());
                        break;
                    case "Confirm Sale":
                    case "Confirm Auction":
                        confirmSale(player, event.getInventory());
                        break;
                    case "Change Duration":
                        changeDuration(player, event.getInventory());
                        break;
                    case "Auction Type: Fixed Price":
                    case "Auction Type: Bidding":
                        toggleAuctionType(player, event.getInventory());
                        break;
                    case "Back":
                        openMainGUI(player);
                        break;
                    default:
                        break;
                }
            }
        }
    }

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

            if (clickedSlot == 50) { // Page Indicator slot
                PaginationHelper paginationHelper = yourAuctionsPaginationMap.get(player.getUniqueId());
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

    private void decreasePrice(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        double currentPrice = priceMap.getOrDefault(playerUUID, 100.0);
        currentPrice = Math.max(0, currentPrice - 10); // Decrease by $10, minimum $0
        priceMap.put(playerUUID, currentPrice);

        updatePriceDisplay(sellGui, currentPrice);
    }

    private void increasePrice(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        double currentPrice = priceMap.getOrDefault(playerUUID, 100.0);
        currentPrice += 10; // Increase by $10
        priceMap.put(playerUUID, currentPrice);

        updatePriceDisplay(sellGui, currentPrice);
    }

    private void updatePriceDisplay(Inventory sellGui, double currentPrice) {
        ItemStack priceDisplay = new ItemStack(Material.PAPER);
        ItemMeta priceMeta = priceDisplay.getItemMeta();
        priceMeta.setDisplayName(Utils.getInstance().$("Current Price: $" + economyHelper.formatBalance(currentPrice)));
        priceMeta.setLore(List.of(Utils.getInstance().$("Right-click to set custom price")));
        priceDisplay.setItemMeta(priceMeta);

        sellGui.setItem(28, priceDisplay);
    }

    private void confirmSale(Player player, Inventory sellGui) {
        ItemStack itemToSell = sellGui.getItem(22);
        if (itemToSell == null || itemToSell.getType() == Material.AIR) {
            player.sendMessage(Utils.getInstance().$("You must place an item in the center slot to sell."));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        double price = priceMap.getOrDefault(playerUUID, 100.0);
        long duration = durationMap.getOrDefault(playerUUID, 86400000L); // Default 24h
        boolean isBidItem = bidMap.getOrDefault(playerUUID, false);

        // Remove the item from the GUI
        sellGui.setItem(22, null);

        // Remove the item from the sellingItems map
        sellingItems.remove(player.getUniqueId());

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
        ItemStack changeDuration = sellGui.getItem(13);
        if (changeDuration != null) {
            ItemMeta durationMeta = changeDuration.getItemMeta();
            List<String> newLore = new ArrayList<>();
            if (durationMeta.hasLore()) {
                newLore.add(Utils.getInstance().$("Current Duration: ") + formatDuration(currentDuration));
            } else {
                newLore.add(Utils.getInstance().$("Current Duration: ") + formatDuration(currentDuration));
            }
            durationMeta.setLore(newLore);
            changeDuration.setItemMeta(durationMeta);

            sellGui.setItem(13, changeDuration);
        }
    }

    private String formatDuration(long millis) {
        long hours = millis / 3600000L;
        return hours + "h";
    }

    private void toggleAuctionType(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        boolean isBidItem = !bidMap.getOrDefault(playerUUID, false);
        bidMap.put(playerUUID, isBidItem);

        // Update the toggle button
        ItemStack toggleAuctionType = sellGui.getItem(16);
        if (toggleAuctionType != null) {
            ItemMeta toggleMeta = toggleAuctionType.getItemMeta();
            toggleMeta.setDisplayName(Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")));
            toggleMeta.setLore(List.of(Utils.getInstance().$("Click to switch auction type")));
            toggleAuctionType.setItemMeta(toggleMeta);
            sellGui.setItem(16, toggleAuctionType);
        }

        // Update the Confirm button lore to reflect auction type
        ItemStack confirm = sellGui.getItem(31);
        if (confirm != null) {
            ItemMeta confirmMeta = confirm.getItemMeta();
            confirmMeta.setDisplayName(Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")));
            confirm.setItemMeta(confirmMeta);
            sellGui.setItem(31, confirm);
        }
    }

    private void promptBidAmount(Player player, UUID auctionId) {
        // Add the player to a pending bids map before closing inventory
        pendingBids.put(player.getUniqueId(), auctionId);

        // Prompt the player to enter a bid amount via chat
        player.sendMessage(Utils.getInstance().$("Enter your bid amount in chat:"));
    }

    private void promptCustomPrice(Player player) {
        // Player is already added to pendingPriceInput before this method is called

        // Prompt the player to enter a custom price via chat
        player.sendMessage(Utils.getInstance().$("Enter your custom price in chat:"));
    }

    /**
     * Handles player chat input for bidding and custom price setting.
     *
     * @param event The AsyncPlayerChatEvent.
     */
    public void onPlayerChat(AsyncPlayerChatEvent event) {
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

    // Additional helper methods for inventory close handling
    public boolean isPendingPriceInput(UUID playerId) {
        return pendingPriceInput.contains(playerId);
    }

    public void removeSellingItem(UUID playerId) {
        sellingItems.remove(playerId);
    }

    public void removePagination(UUID playerId) {
        buyPaginationMap.remove(playerId);
        yourAuctionsPaginationMap.remove(playerId);
    }

    public void registerEvents(JavaPlugin pluginInstance) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInventoryClickEvent(InventoryClickEvent event) {
                onInventoryClick(event);
            }

            @EventHandler
            public void onInventoryDragEvent(InventoryDragEvent event) {
                onInventoryDrag(event);
            }

            @EventHandler
            public void onPlayerChatEvent(AsyncPlayerChatEvent event) {
                onPlayerChat(event);
            }
        }, pluginInstance);
    }
}
