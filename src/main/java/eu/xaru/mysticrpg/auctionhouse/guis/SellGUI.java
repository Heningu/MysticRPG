package eu.xaru.mysticrpg.auctionhouse.guis;

import eu.xaru.mysticrpg.auctionhouse.AuctionsGUI;
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

/**
 * Handles the Sell GUI, including listing items for sale or auction with options to set price, duration, and type.
 */
public class SellGUI {

    private final AuctionsGUI mainGUI;

    // Constants for GUI slot indices
    public static final int SELL_GUI_BACK_SLOT = 49;
    public static final int SELL_GUI_DECREASE_PRICE_SLOT = 19;
    public static final int SELL_GUI_INCREASE_PRICE_SLOT = 25;
    public static final int SELL_GUI_CONFIRM_SLOT = 31;
    public static final int SELL_GUI_CHANGE_DURATION_SLOT = 13;
    public static final int SELL_GUI_PRICE_DISPLAY_SLOT = 28;
    public static final int SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT = 16;
    public static final int SELL_GUI_ITEM_SLOT = 22;

    /**
     * Constructs the SellGUI with a reference to the main AuctionsGUI.
     *
     * @param mainGUI The main AuctionsGUI instance.
     */
    public SellGUI(AuctionsGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    /**
     * Opens the Sell GUI for the player to list an item.
     *
     * @param player The player to open the GUI for.
     */
    public void openSellGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House - Sell"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 22 for the item to sell
        mainGUI.fillWithPlaceholdersBorderOnly(gui, Collections.singleton(SELL_GUI_ITEM_SLOT));

        // Set default price and duration if not set
        mainGUI.getPriceMap().putIfAbsent(player.getUniqueId(), 100.0); // Default price
        mainGUI.getDurationMap().putIfAbsent(player.getUniqueId(), 86400000L); // Default duration (24h)
        mainGUI.getBidMap().putIfAbsent(player.getUniqueId(), false); // Default to fixed price

        // Create buttons and placeholders
        ItemStack decreasePrice = mainGUI.createGuiItem(Material.REDSTONE_BLOCK, Utils.getInstance().$("Decrease Price"), null);
        ItemStack increasePrice = mainGUI.createGuiItem(Material.EMERALD_BLOCK, Utils.getInstance().$("Increase Price"), null);
        boolean isBidItem = mainGUI.getBidMap().get(player.getUniqueId());
        ItemStack confirm = mainGUI.createGuiItem(Material.GREEN_WOOL, Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")), null);
        ItemStack changeDuration = mainGUI.createGuiItem(Material.CLOCK, Utils.getInstance().$("Change Duration"),
                Collections.singletonList(Utils.getInstance().$("Current Duration: " +
                        formatDuration(mainGUI.getDurationMap().get(player.getUniqueId())))));
        ItemStack priceDisplay = mainGUI.createGuiItem(Material.PAPER, Utils.getInstance().$("Current Price: $" +
                        mainGUI.getEconomyHelper().formatBalance(mainGUI.getPriceMap().get(player.getUniqueId()))),
                Collections.singletonList(Utils.getInstance().$("Right-click to set custom price")));
        ItemStack toggleAuctionType = mainGUI.createGuiItem(Material.GOLDEN_HOE, Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")),
                Collections.singletonList(Utils.getInstance().$("Click to switch auction type")));

        // Add a back button
        ItemStack backButton = mainGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);

        // Place items
        gui.setItem(SELL_GUI_DECREASE_PRICE_SLOT, decreasePrice);
        gui.setItem(SELL_GUI_INCREASE_PRICE_SLOT, increasePrice);
        gui.setItem(SELL_GUI_CONFIRM_SLOT, confirm);
        gui.setItem(SELL_GUI_CHANGE_DURATION_SLOT, changeDuration);
        gui.setItem(SELL_GUI_PRICE_DISPLAY_SLOT, priceDisplay);
        gui.setItem(SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT, toggleAuctionType);
        gui.setItem(SELL_GUI_BACK_SLOT, backButton);

        // Place the stored item back into slot 22 if present
        ItemStack itemToSell = mainGUI.getSellingItems().get(player.getUniqueId());
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
     * Handles clicks within the Sell GUI.
     *
     * @param event  The InventoryClickEvent.
     * @param player The player who clicked.
     */
    public void handleSellGUIClick(InventoryClickEvent event, Player player) {
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
            Bukkit.getScheduler().runTaskLater(mainGUI.getPlugin(), () -> {
                ItemStack item = event.getView().getTopInventory().getItem(SELL_GUI_ITEM_SLOT);
                if (item != null && item.getType() != Material.AIR) {
                    mainGUI.getSellingItems().put(player.getUniqueId(), item.clone());
                } else {
                    mainGUI.getSellingItems().remove(player.getUniqueId());
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
                mainGUI.getSellingItems().put(player.getUniqueId(), itemToSell.clone());
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
                    mainGUI.getSellingItems().put(player.getUniqueId(), itemToSell.clone());
                }
                // Add to pendingPriceInput before closing inventory
                mainGUI.getPendingPriceInput().add(player.getUniqueId());
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
                    mainGUI.openMainGUI(player);
                    break;
                default:
                    break;
            }
        }

        event.setCancelled(true);
    }

    /**
     * Decreases the current price in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void decreasePrice(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        double currentPrice = mainGUI.getPriceMap().getOrDefault(playerUUID, 100.0);
        currentPrice = Math.max(0, currentPrice - 10); // Decrease by $10, minimum $0
        mainGUI.getPriceMap().put(playerUUID, currentPrice);

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
        double currentPrice = mainGUI.getPriceMap().getOrDefault(playerUUID, 100.0);
        currentPrice += 10; // Increase by $10
        mainGUI.getPriceMap().put(playerUUID, currentPrice);

        updatePriceDisplay(sellGui, currentPrice);
    }

    /**
     * Updates the price display in the Sell GUI.
     *
     * @param sellGui      The Sell GUI inventory.
     * @param currentPrice The current price to display.
     */
    private void updatePriceDisplay(Inventory sellGui, double currentPrice) {
        ItemStack priceDisplay = mainGUI.createGuiItem(Material.PAPER, Utils.getInstance().$("Current Price: $" +
                        mainGUI.getEconomyHelper().formatBalance(currentPrice)),
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
        double price = mainGUI.getPriceMap().getOrDefault(playerUUID, 100.0);
        long duration = mainGUI.getDurationMap().getOrDefault(playerUUID, 86400000L); // Default 24h
        boolean isBidItem = mainGUI.getBidMap().getOrDefault(playerUUID, false);

        // Remove the item from the GUI
        sellGui.setItem(SELL_GUI_ITEM_SLOT, null);

        // Remove the item from the sellingItems map
        mainGUI.getSellingItems().remove(playerUUID);

        // Add the auction
        if (isBidItem) {
            mainGUI.getAuctionHouseHelper().addBidAuction(playerUUID, itemToSell, price, duration);
        } else {
            mainGUI.getAuctionHouseHelper().addAuction(playerUUID, itemToSell, price, duration);
        }

        player.sendMessage(Utils.getInstance().$("Your item has been listed for " + (isBidItem ? "auction!" : "sale!")));

        // Close the GUI
        player.closeInventory();

        // Open "My Auctions" GUI
        Bukkit.getScheduler().runTask(mainGUI.getPlugin(), () -> mainGUI.openMainGUI(player));
    }

    /**
     * Changes the duration of the auction in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void changeDuration(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        long currentDuration = mainGUI.getDurationMap().getOrDefault(playerUUID, 86400000L);
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
        mainGUI.getDurationMap().put(playerUUID, currentDuration);

        // Update the duration display
        ItemStack changeDuration = mainGUI.createGuiItem(Material.CLOCK, Utils.getInstance().$("Change Duration"),
                Collections.singletonList(Utils.getInstance().$("Current Duration: " + formatDuration(currentDuration))));

        sellGui.setItem(SELL_GUI_CHANGE_DURATION_SLOT, changeDuration);
    }

    /**
     * Toggles the auction type between fixed price and bidding in the Sell GUI.
     *
     * @param player  The player who initiated the action.
     * @param sellGui The Sell GUI inventory.
     */
    private void toggleAuctionType(Player player, Inventory sellGui) {
        UUID playerUUID = player.getUniqueId();
        boolean isBidItem = !mainGUI.getBidMap().getOrDefault(playerUUID, false);
        mainGUI.getBidMap().put(playerUUID, isBidItem);

        // Update the toggle button
        ItemStack toggleAuctionType = mainGUI.createGuiItem(Material.GOLDEN_HOE,
                Utils.getInstance().$("Auction Type: " + (isBidItem ? "Bidding" : "Fixed Price")),
                Collections.singletonList(Utils.getInstance().$("Click to switch auction type")));
        sellGui.setItem(SELL_GUI_TOGGLE_AUCTION_TYPE_SLOT, toggleAuctionType);

        // Update the Confirm button display name to reflect auction type
        ItemStack confirm = mainGUI.createGuiItem(Material.GREEN_WOOL,
                Utils.getInstance().$("Confirm " + (isBidItem ? "Auction" : "Sale")),
                null);
        sellGui.setItem(SELL_GUI_CONFIRM_SLOT, confirm);
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
     * Updates the inner non-button slots with placeholders, excluding specified slots.
     *
     * @param inventory         The inventory to fill.
     * @param occupiedSlots     List of slot indices that are occupied by interactive buttons.
     * @param itemSlotExcluded  Slot index that should remain free (e.g., slot 22 in Sell GUI).
     */
    private void fillInnerPlaceholders(Inventory inventory, List<Integer> occupiedSlots, int itemSlotExcluded) {
        ItemStack placeholder = mainGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

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
     * Formats the duration from milliseconds to a human-readable string.
     *
     * @param millis The duration in milliseconds.
     * @return A formatted string representing the duration.
     */
    private String formatDuration(long millis) {
        long hours = millis / 3600000L;
        return hours + "h";
    }
}
