package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionDuration;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseHelper;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manage Bids GUI - allows players to view and manage their current bids
 * Features:
 * - View all auctions player is currently bidding on
 * - Increase bid option
 * - Cancel bid option  
 * - Claim won items
 */
public class ManageBidsGUI {

    private final AuctionHouseMainMenu mainGUI;
    private final EconomyHelper economyHelper;
    private final AuctionHouseHelper auctionHouseHelper;
    
    // Chat input tracking for bid amounts
    private static final Map<UUID, BidInputData> pendingBidInput = new HashMap<>();

    public ManageBidsGUI(AuctionHouseMainMenu mainGUI) {
        this.mainGUI = mainGUI;
        this.economyHelper = mainGUI.getEconomyHelper();
        this.auctionHouseHelper = mainGUI.getAuctionHouseHelper();
    }

    /**
     * Opens the Manage Bids GUI for the specified player.
     */
    public void openManageBidsGUI(Player player) {
        List<Auction> playerBids = getPlayerBids(player.getUniqueId());
        
        if (playerBids.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("You are not currently bidding on any auctions."));
            return;
        }

        // Convert auctions to display items
        List<Item> bidItems = playerBids.stream().map(auction -> {
            return createBidDisplayItem(auction, player);
        }).collect(Collectors.toList());

        // Back button
        Item backButton = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Back")
                .addLoreLines(
                        "",
                        "Click to return to the main auction menu.",
                        ""
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                mainGUI.openAuctionsGUI(p);
            }
        };

        // Border items
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" "));

        Item pageController = new PageControllerItem();

        // Create paged GUI
        PagedGui<Item> pagedGui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #", 
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B > # # # # # # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', border)
                .addIngredient('B', backButton)
                .addIngredient('>', pageController)
                .setContent(bidItems)
                .build();

        // Create and open window
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.BLUE + "Manage Your Bids")
                .setGui(pagedGui)
                .build();
        window.open();
    }

    /**
     * Get all auctions where the player is the current highest bidder
     */
    private List<Auction> getPlayerBids(UUID playerUUID) {
        return auctionHouseHelper.getActiveAuctions().stream()
                .filter(auction -> auction.isBidItem())
                .filter(auction -> playerUUID.equals(auction.getHighestBidder()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a display item for a bid
     */
    private Item createBidDisplayItem(Auction auction, Player player) {
        ItemStack originalItem = auction.getItem().clone();
        ItemMeta meta = originalItem.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Add bid information to lore
        lore.add("");
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.YELLOW + auction.getSellerName());
        lore.add(ChatColor.GRAY + "Your bid: " + ChatColor.GOLD + economyHelper.formatGold(auction.getCurrentBid()) + " coins");
        
        long timeLeft = auction.getEndTime() - System.currentTimeMillis();
        String timeLeftStr = AuctionDuration.formatTimeRemaining(timeLeft);
        lore.add(ChatColor.GRAY + "Time left: " + ChatColor.WHITE + timeLeftStr);
        
        lore.add("");
        
        if (timeLeft <= 0) {
            // Auction has ended - player can claim
            lore.add(ChatColor.GREEN + "Auction ended! Left-click to claim item");
        } else {
            // Auction is still active
            lore.add(ChatColor.GREEN + "Left-click: Increase bid");
            lore.add(ChatColor.RED + "Right-click: Cancel bid");
        }

        meta.setLore(lore);
        originalItem.setItemMeta(meta);

        return new SimpleItem(new ItemBuilder(originalItem)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                long timeLeft = auction.getEndTime() - System.currentTimeMillis();
                
                if (timeLeft <= 0) {
                    // Auction ended - claim item if player won
                    claimWonItem(p, auction);
                } else if (clickType == ClickType.LEFT) {
                    // Increase bid
                    openIncreaseBidOptions(p, auction);
                } else if (clickType == ClickType.RIGHT) {
                    // Cancel bid
                    cancelBid(p, auction);
                }
            }
        };
    }

    /**
     * Opens increase bid options (50%, 100%, custom)
     */
    private void openIncreaseBidOptions(Player player, Auction auction) {
        int currentBid = auction.getCurrentBid();
        int bid50Percent = currentBid + (int) Math.ceil(currentBid * 0.5);
        int bid100Percent = currentBid * 2;

        // Create a small GUI for bid options
        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "));
        
        Item bid50 = new SimpleItem(new ItemBuilder(Material.GOLD_NUGGET)
                .setDisplayName(ChatColor.YELLOW + "Bid +50%")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "New bid: " + ChatColor.WHITE + economyHelper.formatGold(bid50Percent) + " coins",
                        "",
                        ChatColor.GREEN + "Click to place this bid"
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                placeBid(p, auction, bid50Percent);
                p.closeInventory();
            }
        };

        Item bid100 = new SimpleItem(new ItemBuilder(Material.GOLD_INGOT)
                .setDisplayName(ChatColor.YELLOW + "Bid +100%")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "New bid: " + ChatColor.WHITE + economyHelper.formatGold(bid100Percent) + " coins",
                        "",
                        ChatColor.GREEN + "Click to place this bid"
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                placeBid(p, auction, bid100Percent);
                p.closeInventory();
            }
        };

        Item customBid = new SimpleItem(new ItemBuilder(Material.BOOK)
                .setDisplayName(ChatColor.AQUA + "Custom Amount")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "Enter a custom bid amount",
                        ChatColor.GRAY + "Must be higher than current bid",
                        "",
                        ChatColor.GREEN + "Click to enter amount"
                ))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                p.closeInventory();
                pendingBidInput.put(p.getUniqueId(), new BidInputData(auction, ManageBidsGUI.this));
                p.sendMessage(Utils.getInstance().$("Enter your bid amount (must be higher than " + 
                        economyHelper.formatGold(currentBid) + "):"));
            }
        };

        Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Back")
                .addLoreLines("", "Return to manage bids", ""))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                p.closeInventory();
                openManageBidsGUI(p);
            }
        };

        xyz.xenondevs.invui.gui.Gui bidGui = xyz.xenondevs.invui.gui.Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # a # b # c # #",
                        "# # # # d # # # #"
                )
                .addIngredient('#', filler)
                .addIngredient('a', bid50)
                .addIngredient('b', bid100)
                .addIngredient('c', customBid)
                .addIngredient('d', back)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.YELLOW + "Increase Bid")
                .setGui(bidGui)
                .build();
        window.open();
    }

    /**
     * Places a bid on an auction
     */
    private void placeBid(Player player, Auction auction, int bidAmount) {
        auctionHouseHelper.placeBid(player, auction.getAuctionId(), bidAmount);
        // Reopen manage bids GUI to show updated information
        openManageBidsGUI(player);
    }

    /**
     * Cancels a player's bid
     */
    private void cancelBid(Player player, Auction auction) {
        // Refund the player's current bid
        int refundAmount = auction.getCurrentBid();
        int playerBalance = economyHelper.getBankGold(player);
        economyHelper.setBankGold(player, playerBalance + refundAmount);
        
        // Reset auction to starting bid with no bidder
        auction.setCurrentBid(auction.getStartingPrice());
        auction.setHighestBidder(null);
        
        player.sendMessage(Utils.getInstance().$("Bid cancelled. You have been refunded " + 
                economyHelper.formatGold(refundAmount) + " coins."));
        
        // Reopen manage bids GUI
        openManageBidsGUI(player);
    }

    /**
     * Claims a won auction item
     */
    private void claimWonItem(Player player, Auction auction) {
        // Give item to player
        player.getInventory().addItem(auction.getItem());
        
        // Remove auction from active auctions
        auctionHouseHelper.removeAuction(auction.getAuctionId());
        
        player.sendMessage(Utils.getInstance().$("Item claimed! " + auction.getItem().getType().toString().replace("_", " ") + 
                " has been added to your inventory."));
        
        // Reopen manage bids GUI
        openManageBidsGUI(player);
    }

    /**
     * Page controller for paginated display
     */
    private static class PageControllerItem extends ControlItem<PagedGui<?>> {
        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.LEFT) {
                getGui().goBack();
            } else if (clickType == ClickType.RIGHT) {
                getGui().goForward();
            }
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch pages")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " of " + gui.getPageAmount(),
                            ChatColor.GREEN + "Left-click: Next page",
                            ChatColor.RED + "Right-click: Previous page"
                    );
        }
    }

    // Static methods for handling chat input
    public static boolean isPendingBidInput(Player player) {
        return pendingBidInput.containsKey(player.getUniqueId());
    }

    public static void handleBidInput(Player player, String input) {
        BidInputData data = pendingBidInput.remove(player.getUniqueId());
        if (data == null) return;

        try {
            int bidAmount = Integer.parseInt(input);
            if (bidAmount <= data.auction.getCurrentBid()) {
                player.sendMessage(Utils.getInstance().$("Bid must be higher than the current bid of " + 
                        data.gui.economyHelper.formatGold(data.auction.getCurrentBid()) + " coins."));
            } else {
                data.gui.placeBid(player, data.auction, bidAmount);
                return; // placeBid will reopen the GUI
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Utils.getInstance().$("Invalid bid amount. Please enter a number."));
        }

        // Reopen manage bids GUI if bid failed
        data.gui.openManageBidsGUI(player);
    }

    /**
     * Helper class for storing bid input data
     */
    private static class BidInputData {
        final Auction auction;
        final ManageBidsGUI gui;

        BidInputData(Auction auction, ManageBidsGUI gui) {
            this.auction = auction;
            this.gui = gui;
        }
    }
}