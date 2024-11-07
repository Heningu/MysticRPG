package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
    private final MysticCore plugin;

    // Temporary storage for price and duration settings per player
    private final Map<UUID, Double> priceMap = new HashMap<>();
    private final Map<UUID, Long> durationMap = new HashMap<>();
    private final Map<UUID, Boolean> bidMap = new HashMap<>();

    // Maps to store pending actions for players
    private final Map<UUID, UUID> pendingBids = new HashMap<>();
    private final Set<UUID> pendingPriceInput = new HashSet<>();

    public AuctionsGUI(AuctionHouseHelper auctionHouseHelper,
                       EconomyHelper economyHelper,
                       DebugLoggerModule logger) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
        this.logger = logger;
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    /**
     * Opens the main Auction House GUI.
     *
     * @param player The player to open the GUI for.
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_BLUE + "Auction House");

        // Fill the GUI with placeholders
        fillWithPlaceholders(gui);

        // Create the "Buy Items" item
        ItemStack buyItems = new ItemStack(Material.EMERALD);
        ItemMeta buyMeta = buyItems.getItemMeta();
        buyMeta.setDisplayName(ChatColor.GREEN + "Buy Items");
        buyItems.setItemMeta(buyMeta);

        // Create the "Sell Items" item
        ItemStack sellItems = new ItemStack(Material.CHEST);
        ItemMeta sellMeta = sellItems.getItemMeta();
        sellMeta.setDisplayName(ChatColor.GREEN + "Sell Items");
        sellItems.setItemMeta(sellMeta);

        // Create the "My Auctions" item
        ItemStack myAuctions = new ItemStack(Material.BOOK);
        ItemMeta myAuctionsMeta = myAuctions.getItemMeta();
        myAuctionsMeta.setDisplayName(ChatColor.GREEN + "My Auctions");
        myAuctions.setItemMeta(myAuctionsMeta);

        // Place the items in the GUI
        gui.setItem(20, buyItems);
        gui.setItem(22, sellItems);
        gui.setItem(24, myAuctions);

        player.openInventory(gui);
    }

    /**
     * Opens the Buy GUI showing available auctions.
     *
     * @param player The player to open the GUI for.
     */
    public void openBuyGUI(Player player) {
        if (!auctionHouseHelper.areAuctionsLoaded()) {
            player.sendMessage(ChatColor.RED + "Please wait, auctions are still loading.");
            return;
        }

        List<Auction> auctions = auctionHouseHelper.getActiveAuctions();
        logger.log(Level.INFO, "Opening Buy GUI for player " +
                player.getName() + ". Auctions available: " +
                auctions.size(), 0);

        if (auctions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are currently no items for sale.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_BLUE + "Auction House - Buy");

        // Add a back button
        addBackButton(gui);

        int slot = 0;
        for (Auction auction : auctions) {
            if (slot >= 45) {
                break; // Limit to 45 items to leave space for control buttons
            }

            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore()
                    : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GOLD + "Current Bid: $" +
                        economyHelper.formatBalance(auction.getCurrentBid()));
                lore.add(ChatColor.YELLOW + "Right-click to place a bid");
            } else {
                lore.add(ChatColor.GOLD + "Price: $" +
                        economyHelper.formatBalance(auction.getStartingPrice()));
                lore.add(ChatColor.GREEN + "Left-click to buy now");
            }
            lore.add(ChatColor.GRAY + "Time Left: " +
                    formatTimeLeft(auction.getEndTime()
                            - System.currentTimeMillis()));
            lore.add(ChatColor.DARK_GRAY + "Auction ID: " +
                    auction.getAuctionId());

            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            logger.log(Level.INFO, "Added auction item to GUI: Auction ID " + auction.getAuctionId(), 0);

            slot++;
        }

        player.openInventory(gui);
    }

    /**
     * Opens the Sell GUI for the player to list an item.
     *
     * @param player The player to open the GUI for.
     */
    public void openSellGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_BLUE + "Auction House - Sell");

        // Fill the GUI with placeholders
        fillWithPlaceholders(gui);

        // Set default price and duration if not set
        priceMap.putIfAbsent(player.getUniqueId(), 100.0); // Default price
        durationMap.putIfAbsent(player.getUniqueId(), 86400000L); // Default duration (24h)
        bidMap.putIfAbsent(player.getUniqueId(), false); // Default to fixed price

        // Create buttons and placeholders
        ItemStack decreasePrice = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta decreaseMeta = decreasePrice.getItemMeta();
        decreaseMeta.setDisplayName(ChatColor.RED + "Decrease Price");
        decreasePrice.setItemMeta(decreaseMeta);

        ItemStack increasePrice = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta increaseMeta = increasePrice.getItemMeta();
        increaseMeta.setDisplayName(ChatColor.GREEN + "Increase Price");
        increasePrice.setItemMeta(increaseMeta);

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        boolean isBidItem = bidMap.get(player.getUniqueId());
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm " + (isBidItem ? "Auction" : "Sale"));
        confirm.setItemMeta(confirmMeta);

        ItemStack changeDuration = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = changeDuration.getItemMeta();
        durationMeta.setDisplayName(ChatColor.YELLOW + "Change Duration");
        durationMeta.setLore(List.of(ChatColor.GRAY + "Current Duration: " +
                formatDuration(durationMap.get(player.getUniqueId()))));
        changeDuration.setItemMeta(durationMeta);

        // Create price display
        ItemStack priceDisplay = new ItemStack(Material.PAPER);
        ItemMeta priceMeta = priceDisplay.getItemMeta();
        priceMeta.setDisplayName(ChatColor.AQUA + "Current Price: $" +
                economyHelper.formatBalance(priceMap.get(player.getUniqueId())));
        priceMeta.setLore(List.of(ChatColor.GRAY + "Right-click to set custom price"));
        priceDisplay.setItemMeta(priceMeta);

        // Create "Toggle Auction Type" button
        ItemStack toggleAuctionType = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta toggleMeta = toggleAuctionType.getItemMeta();
        toggleMeta.setDisplayName(ChatColor.AQUA + "Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price"));
        toggleMeta.setLore(List.of(ChatColor.GRAY + "Click to switch auction type"));
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

        // Slot 22 is for the item to sell; leave it empty
        gui.setItem(22, null);

        player.openInventory(gui);
    }

    /**
     * Opens the player's active auctions GUI.
     *
     * @param player The player to open the GUI for.
     */
    public void openPlayerAuctionsGUI(Player player) {
        if (!auctionHouseHelper.areAuctionsLoaded()) {
            player.sendMessage(ChatColor.RED + "Please wait, auctions are still loading.");
            return;
        }

        List<Auction> playerAuctions = auctionHouseHelper.getPlayerAuctions(player.getUniqueId());

        if (playerAuctions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no active auctions.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_BLUE + "Your Auctions");

        // Add a back button
        addBackButton(gui);

        int slot = 0;
        for (Auction auction : playerAuctions) {
            if (slot >= 45) {
                break; // Limit to 45 items to leave space for control buttons
            }

            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore()
                    : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GOLD + "Current Bid: $" +
                        economyHelper.formatBalance(auction.getCurrentBid()));
            } else {
                lore.add(ChatColor.GOLD + "Price: $" +
                        economyHelper.formatBalance(auction.getStartingPrice()));
            }
            lore.add(ChatColor.GRAY + "Time Left: " +
                    formatTimeLeft(auction.getEndTime()
                            - System.currentTimeMillis()));
            lore.add(ChatColor.RED + "Click to cancel this auction");
            lore.add(ChatColor.DARK_GRAY + "Auction ID: " +
                    auction.getAuctionId());

            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            logger.log(Level.INFO, "Added player's auction item to GUI: Auction ID " + auction.getAuctionId(), 0);

            slot++;
        }

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
     * Fills the empty slots of the inventory with black stained glass panes.
     *
     * @param inventory The inventory to fill.
     */
    private void fillWithPlaceholders(Inventory inventory) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, placeholder);
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
        backMeta.setDisplayName(ChatColor.YELLOW + "Back");
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
            for (Integer slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    if (slot != 22) {
                        event.setCancelled(true);
                        return;
                    }
                }
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
        if (clickedItem.getType() == Material.ARROW) {
            openMainGUI(player);
            return;
        }

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
                                // Handle bidding
                                if (event.getClick() == ClickType.RIGHT) {
                                    player.closeInventory();
                                    promptBidAmount(player, auctionId);
                                }
                            } else {
                                // Handle buy now
                                if (event.getClick() == ClickType.LEFT) {
                                    auctionHouseHelper.buyAuction(player, auctionId);
                                    // Refresh the Buy GUI
                                    Bukkit.getScheduler().runTask(plugin, () -> openBuyGUI(player));
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid auction ID.");
                    }
                }
            }
        }
    }

    private void handleSellGUIClick(InventoryClickEvent event, Player player) {
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getRawSlot();

        if (event.isShiftClick()) {
            // Handle shift-clicks
            if (event.getClickedInventory().equals(player.getInventory())) {
                // Shift-clicking from player inventory to GUI
                Inventory sellGui = event.getView().getTopInventory();
                if (sellGui.getItem(22) == null || sellGui.getItem(22).getType() == Material.AIR) {
                    // Place the item into slot 22
                    sellGui.setItem(22, event.getCurrentItem().clone());
                    event.getClickedInventory().setItem(event.getSlot(), null);
                }
                event.setCancelled(true);
            } else {
                // Prevent shift-clicking in the GUI
                event.setCancelled(true);
            }
        } else if (slot == 22) {
            // Allow normal clicking in slot 22
            event.setCancelled(false);
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
                if (event.getClick() == ClickType.RIGHT) {
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
        if (clickedItem.getType() == Material.ARROW) {
            openMainGUI(player);
            return;
        }

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
                        player.sendMessage(ChatColor.RED + "Invalid auction ID.");
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
        priceMeta.setDisplayName(ChatColor.AQUA + "Current Price: $" + economyHelper.formatBalance(currentPrice));
        priceMeta.setLore(List.of(ChatColor.GRAY + "Right-click to set custom price"));
        priceDisplay.setItemMeta(priceMeta);

        sellGui.setItem(28, priceDisplay);
    }

    private void confirmSale(Player player, Inventory sellGui) {
        ItemStack itemToSell = sellGui.getItem(22);
        if (itemToSell == null || itemToSell.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must place an item in the center slot to sell.");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        double price = priceMap.getOrDefault(playerUUID, 100.0);
        long duration = durationMap.getOrDefault(playerUUID, 86400000L); // Default 24h
        boolean isBidItem = bidMap.getOrDefault(playerUUID, false);

        // Remove the item from the player's inventory
        sellGui.setItem(22, null);

        // Add the auction
        if (isBidItem) {
            auctionHouseHelper.addBidAuction(playerUUID, itemToSell, price, duration);
        } else {
            auctionHouseHelper.addAuction(playerUUID, itemToSell, price, duration);
        }

        player.sendMessage(ChatColor.GREEN + "Your item has been listed for " + (isBidItem ? "auction!" : "sale!"));

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
            durationMeta.setLore(List.of(ChatColor.GRAY + "Current Duration: " + formatDuration(currentDuration)));
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
            toggleMeta.setDisplayName(ChatColor.AQUA + "Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price"));
            toggleMeta.setLore(List.of(ChatColor.GRAY + "Click to switch auction type"));
            toggleAuctionType.setItemMeta(toggleMeta);
            sellGui.setItem(16, toggleAuctionType);
        }

        // Update the Confirm button lore to reflect auction type
        ItemStack confirm = sellGui.getItem(31);
        if (confirm != null) {
            ItemMeta confirmMeta = confirm.getItemMeta();
            confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm " + (isBidItem ? "Auction" : "Sale"));
            confirm.setItemMeta(confirmMeta);
            sellGui.setItem(31, confirm);
        }
    }

    private void promptBidAmount(Player player, UUID auctionId) {
        // Prompt the player to enter a bid amount via chat
        player.sendMessage(ChatColor.YELLOW + "Enter your bid amount in chat:");

        // Add the player to a pending bids map
        pendingBids.put(player.getUniqueId(), auctionId);

        // Optionally, set a timeout to remove the player from pending bids after a certain time
    }

    private void promptCustomPrice(Player player) {
        // Prompt the player to enter a custom price via chat
        player.sendMessage(ChatColor.YELLOW + "Enter your custom price in chat:");

        // Add the player to a pending price input set
        pendingPriceInput.add(player.getUniqueId());
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
                player.sendMessage(ChatColor.RED + "Invalid bid amount. Please enter a number.");
            }
        } else if (pendingPriceInput.contains(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            pendingPriceInput.remove(playerId);

            try {
                double customPrice = Double.parseDouble(message);
                if (customPrice < 0) {
                    player.sendMessage(ChatColor.RED + "Price cannot be negative.");
                    return;
                }
                priceMap.put(playerId, customPrice);
                player.sendMessage(ChatColor.GREEN + "Custom price set to $" + economyHelper.formatBalance(customPrice));
                // Reopen Sell GUI
                Bukkit.getScheduler().runTask(plugin, () -> openSellGUI(player));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price. Please enter a number.");
            }
        }
    }
}
