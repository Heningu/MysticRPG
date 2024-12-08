package eu.xaru.mysticrpg.auctionhouse.guis;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionsGUI;
import eu.xaru.mysticrpg.guis.PaginationHelper;
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
import java.util.stream.Collectors;

/**
 * Handles the Your Auctions GUI, allowing players to view and manage their active auctions with pagination.
 */
public class YourAuctionsGUI {

    private final AuctionsGUI mainGUI;

    // Constants for GUI slot indices
    public static final int YOUR_AUCTIONS_BACK_SLOT = 49;
    public static final int YOUR_AUCTIONS_PAGE_INDICATOR_SLOT = 50;

    // Define inner slots where auction items will be placed
    private static final int[] AUCTION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    /**
     * Constructs the YourAuctionsGUI with a reference to the main AuctionsGUI.
     *
     * @param mainGUI The main AuctionsGUI instance.
     */
    public YourAuctionsGUI(AuctionsGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    /**
     * Opens the player's active auctions GUI with pagination.
     *
     * @param player The player to open the GUI for.
     */
    public void openPlayerAuctionsGUI(Player player) {
        if (!mainGUI.getAuctionHouseHelper().areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$("Please wait, auctions are still loading."));
            return;
        }

        List<Auction> playerAuctions = mainGUI.getAuctionHouseHelper().getPlayerAuctions(player.getUniqueId());

        if (playerAuctions.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("You have no active auctions."));
            return;
        }

        // Convert player's auctions into ItemStacks
        List<ItemStack> auctionItems = playerAuctions.stream().map(auction -> {
            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GRAY + "Current Bid: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getCurrentBid()));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getStartingPrice())));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime() - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));
            lore.add(Utils.getInstance().$("Click to cancel this auction"));

            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }).collect(Collectors.toList());

        // Handle pagination for Your Auctions GUI
        PaginationHelper<ItemStack> paginationHelper = handleYourAuctionsGuiPagination(player, auctionItems);

        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Your Auctions"));

        // Fill the GUI with placeholders only on the outer border, excluding slot 49 for back button
        mainGUI.fillWithPlaceholdersBorderOnly(gui, Collections.singleton(YOUR_AUCTIONS_BACK_SLOT));

        // Get items for the current page
        List<ItemStack> pageItems = paginationHelper.getCurrentPageItems();

        // Place the auction items in the GUI
        for (int i = 0; i < pageItems.size() && i < AUCTION_SLOTS.length; i++) {
            int slot = AUCTION_SLOTS[i];
            gui.setItem(slot, pageItems.get(i));
        }

        // Create the page indicator with lore instructions
        ItemStack pageIndicator = mainGUI.createGuiItem(Material.PAPER,
                ChatColor.GREEN + "Page " + paginationHelper.getCurrentPage() + " of " + paginationHelper.getTotalPages(),
                Arrays.asList(
                        ChatColor.GRAY + "Left-click to go to the previous page",
                        ChatColor.GRAY + "Right-click to go to the next page"
                ));
        gui.setItem(YOUR_AUCTIONS_PAGE_INDICATOR_SLOT, pageIndicator); // Placed in slot 50

        // Add a back button in slot 49
        ItemStack backButton = mainGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", null);
        gui.setItem(YOUR_AUCTIONS_BACK_SLOT, backButton);

        player.openInventory(gui);
    }

    /**
     * Handles clicks within the Your Auctions GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    public void handleYourAuctionsGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        // Handle the back button click
        if (clickedItem.getType() == Material.ARROW && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Back")) {
            mainGUI.openMainGUI(player);
            return;
        }

        // Handle page indicator clicks
        if (clickedItem.getType() == Material.PAPER && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).startsWith("Page")) {
            int clickedSlot = event.getRawSlot();
            ClickType clickType = event.getClick();

            if (clickedSlot == YOUR_AUCTIONS_PAGE_INDICATOR_SLOT) { // Page Indicator slot
                PaginationHelper<ItemStack> paginationHelper = mainGUI.getYourAuctionsPaginationMap().get(player.getUniqueId());
                if (paginationHelper == null) return;

                boolean pageChanged = false;

                if (clickType.isLeftClick()) {
                    if (paginationHelper.hasPreviousPage()) {
                        paginationHelper.previousPage();
                        pageChanged = true;
                    } else {
                        player.sendMessage(Utils.getInstance().$("You are already on the first page."));
                    }
                } else if (clickType.isRightClick()) {
                    if (paginationHelper.hasNextPage()) {
                        paginationHelper.nextPage();
                        pageChanged = true;
                    } else {
                        player.sendMessage(Utils.getInstance().$("You are already on the last page."));
                    }
                }

                if (pageChanged) {
                    // Update the GUI in-place
                    openPlayerAuctionsGUI(player);
                }
                event.setCancelled(true);
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
                        mainGUI.getAuctionHouseHelper().cancelAuction(auctionId, player);
                        // Refresh the player's auctions GUI
                        Bukkit.getScheduler().runTask(mainGUI.getPlugin(), () -> openPlayerAuctionsGUI(player));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utils.getInstance().$("Invalid auction ID."));
                    }
                }
            }
        }
    }

    /**
     * Handles pagination for the Your Auctions GUI by creating or retrieving a PaginationHelper.
     *
     * @param player       The player for whom pagination is handled.
     * @param auctionItems The list of auction items to paginate.
     * @return The PaginationHelper instance for the current page.
     */
    private PaginationHelper<ItemStack> handleYourAuctionsGuiPagination(Player player, List<ItemStack> auctionItems) {
        PaginationHelper<ItemStack> paginationHelper;

        if (!mainGUI.getYourAuctionsPaginationMap().containsKey(player.getUniqueId())) {
            paginationHelper = new PaginationHelper<>(auctionItems, 28);
            mainGUI.getYourAuctionsPaginationMap().put(player.getUniqueId(), paginationHelper);
        } else {
            paginationHelper = mainGUI.getYourAuctionsPaginationMap().get(player.getUniqueId());
            // Update the items in the pagination helper in case the auctions have changed
            paginationHelper.updateItems(auctionItems);
        }

        return paginationHelper;
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
