package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.*;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Helper class for managing auctions in the auction house.
 */
public class AuctionHouseHelper {

    private final Map<UUID, Auction> activeAuctions;
    private final EconomyHelper economyHelper;
    private final SaveModule saveModule;
    private final PlayerDataCache playerDataCache;
    
    private boolean auctionsLoaded = false;
    private final MysticCore plugin;

    public AuctionHouseHelper(EconomyHelper economyHelper) {
        this.economyHelper = economyHelper;
        this.activeAuctions = new HashMap<>();

        // Get SaveModule instance
        this.saveModule = ModuleManager.getInstance()
                .getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();

        // Get plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        // Load auctions from the database
        loadAuctionsFromDatabase();
    }

    /**
     * Checks if auctions have been loaded.
     *
     * @return true if auctions are loaded, false otherwise.
     */
    public boolean areAuctionsLoaded() {
        return auctionsLoaded;
    }

    /**
     * Loads auctions from the database.
     */
    private void loadAuctionsFromDatabase() {
        saveModule.loadAuctions(new Callback<List<Auction>>() {
            @Override
            public void onSuccess(List<Auction> auctions) {
                for (Auction auction : auctions) {
                    // Deserialize the ItemStack
                    ItemStack item = SaveHelper.itemStackFromBase64(auction.getItemData());
                    auction.setItem(item);
                    activeAuctions.put(auction.getAuctionId(), auction);
                    DebugLogger.getInstance().log(Level.INFO, "Loaded auction with ID: " + auction.getAuctionId(), 0);
                }
                auctionsLoaded = true;
                DebugLogger.getInstance().log(Level.INFO, "Total auctions loaded: " + activeAuctions.size(), 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                auctionsLoaded = true; // Prevent hanging
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to load auctions from the database: ", throwable, throwable);
            }
        });
    }

    /**
     * Adds a new fixed-price auction to the auction house.
     *
     * @param seller   The UUID of the seller.
     * @param item     The item being auctioned.
     * @param price    The price of the item.
     * @param duration The duration in milliseconds.
     * @return The UUID of the newly created auction.
     */
    public UUID addAuction(UUID seller, ItemStack item,
                           double price, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller,
                item, price, endTime);
        activeAuctions.put(auctionId, auction);
        DebugLogger.getInstance().log(Level.INFO, "Auction added to activeAuctions with ID: " + auctionId, 0);


        // Save auction to database
        saveModule.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Auction saved successfully
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId +
                        " saved to database.", 0);

                // Notify players about the new auction
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = auction.getItem().getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has listed " + itemName + " for $" +
                            economyHelper.formatBalance(price) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to save auction to database: ", throwable, throwable);
            }
        });

        return auctionId;
    }

    /**
     * Adds a new bidding auction to the auction house.
     *
     * @param seller        The UUID of the seller.
     * @param item          The item being auctioned.
     * @param startingPrice The starting price of the auction.
     * @param duration      The duration in milliseconds.
     * @return The UUID of the newly created bidding auction.
     */
    public UUID addBidAuction(UUID seller, ItemStack item, double startingPrice, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller, item, startingPrice, endTime, true);
        activeAuctions.put(auctionId, auction);
        DebugLogger.getInstance().log(Level.INFO, "Bid auction added to activeAuctions with ID: " + auctionId, 0);


        // Save auction to database
        saveModule.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Bid auction " + auctionId + " saved to database.", 0);

                // Notify players about the new auction
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = auction.getItem().getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has started an auction for " + itemName + " with a starting bid of $" +
                            economyHelper.formatBalance(startingPrice) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to save bid auction to database: ", throwable, throwable);
            }
        });

        return auctionId;
    }

    /**
     * Removes an auction from the auction house.
     *
     * @param auctionId The UUID of the auction to remove.
     */
    public void removeAuction(UUID auctionId) {
        activeAuctions.remove(auctionId);

        // Delete auction from database
        saveModule.deleteAuction(auctionId, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId +
                        " deleted from database.", 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
            }
        });
    }

    /**
     * Gets a list of all active auctions.
     *
     * @return List of active auctions.
     */
    public List<Auction> getActiveAuctions() {
        long currentTime = System.currentTimeMillis();
        List<Auction> auctions = activeAuctions.values().stream()
                .filter(a -> {
                    boolean isActive = a.getEndTime() > currentTime;
                    DebugLogger.getInstance().log(Level.INFO, "Auction ID: " + a.getAuctionId() + ", End Time: " + a.getEndTime() + ", Is Active: " + isActive, 0);
                    return isActive;
                })
                .collect(Collectors.toList());
        DebugLogger.getInstance().log(Level.INFO, "getActiveAuctions: Found " + auctions.size() + " active auctions.", 0);
        return auctions;
    }


    /**
     * Retrieves a list of auctions created by a specific player.
     *
     * @param playerUUID The UUID of the player.
     * @return A list of the player's active auctions.
     */
    public List<Auction> getPlayerAuctions(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        return activeAuctions.values().stream()
                .filter(a -> a.getSeller().equals(playerUUID)
                        && a.getEndTime() > currentTime)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an auction by its ID.
     *
     * @param auctionId The UUID of the auction.
     * @return The Auction object, or null if not found.
     */
    public Auction getAuctionById(UUID auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Cancels an auction and returns the item to the player.
     *
     * @param auctionId The UUID of the auction to cancel.
     * @param player    The player canceling the auction.
     */
    public void cancelAuction(UUID auctionId, Player player) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getSeller().equals(player.getUniqueId())) {
            // Remove the auction
            activeAuctions.remove(auctionId);

            // Delete auction from database
            saveModule.deleteAuction(auctionId, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Auction deleted successfully
                    player.sendMessage(Utils.getInstance().$("Auction canceled. The item has been returned to your inventory."));
                    DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId + " canceled by player " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
                }
            });

            // Return the item to the player
            player.getInventory().addItem(auction.getItem());
        } else {
            player.sendMessage(Utils.getInstance().$("You cannot cancel this auction."));
        }
    }

    /**
     * Places a bid on an auction.
     *
     * @param bidder    The player placing the bid.
     * @param auctionId The UUID of the auction.
     * @param bidAmount The amount of the bid.
     */
    /**
     * Places a bid on an auction.
     *
     * @param bidder    The player placing the bid.
     * @param auctionId The UUID of the auction.
     * @param bidAmount The amount of the bid.
     */
    public void placeBid(Player bidder, UUID auctionId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.isBidItem()) {
            if (bidAmount > auction.getHighestBid()) {
                double bidderBalance = economyHelper.getBalance(bidder);
                if (bidderBalance >= bidAmount) {
                    // Refund previous highest bidder
                    if (auction.getHighestBidder() != null) {
                        UUID previousBidderId = auction.getHighestBidder();
                        if (!previousBidderId.equals(bidder.getUniqueId())) {
                            Player previousBidder = Bukkit.getPlayer(previousBidderId);
                            if (previousBidder != null && previousBidder.isOnline()) {
                                boolean refunded = economyHelper.depositBalance(previousBidder, auction.getHighestBid());
                                if (!refunded) {
                                    // If refund fails, log the error
                                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to refund previous highest bidder {0} for auction ID: {1}",
                                            new Object[]{previousBidder.getName(), auctionId});
                                } else {
                                    previousBidder.sendMessage(Utils.getInstance().$("Your bid of $" + economyHelper.formatBalance(auction.getHighestBid()) + " has been refunded."));
                                    DebugLogger.getInstance().log(Level.INFO, "Refunded previous highest bidder {0} ${1} for auction ID: {2}",
                                            new Object[]{previousBidder.getName(), auction.getHighestBid(), auctionId});
                                }
                            } else {
                                // Previous bidder is offline, add to pending balance
                                PlayerData previousBidderData = playerDataCache.getCachedPlayerData(previousBidderId);
                                if (previousBidderData != null) {
                                    double pendingBalance = previousBidderData.getPendingBalance();
                                    previousBidderData.setPendingBalance(pendingBalance + auction.getHighestBid());
                                    DebugLogger.getInstance().log(Level.INFO, "Previous highest bidder {0} is offline. Added ${1} to pending balance.",
                                            new Object[]{previousBidderId, auction.getHighestBid()});

                                    // Save the updated PlayerData
                                    playerDataCache.savePlayerData(previousBidderId, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            DebugLogger.getInstance().log(Level.INFO, "Successfully saved pending balance for previous bidder {0}", previousBidderId);
                                        }

                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            DebugLogger.getInstance().log(Level.SEVERE, "Failed to save pending balance for previous bidder " + previousBidderId + ": ", throwable, throwable);
                                        }
                                    });
                                }
                            }
                        }
                    }

                    // Withdraw the bid amount from the bidder
                    boolean withdrawn = economyHelper.withdrawBalance(bidder, bidAmount);
                    if (!withdrawn) {
                        bidder.sendMessage(Utils.getInstance().$("Failed to deduct your bid amount."));
                        DebugLogger.getInstance().log(Level.SEVERE, "Failed to withdraw ${0} from bidder {1} for auction ID: {2}",
                                new Object[]{bidAmount, bidder.getName(), auctionId});
                        return;
                    }

                    // Update the auction with the new highest bid and bidder
                    auction.setHighestBid(bidAmount);
                    auction.setHighestBidder(bidder.getUniqueId());
                    activeAuctions.put(auctionId, auction); // Update the map

                    bidder.sendMessage(Utils.getInstance().$("You are now the highest bidder on auction " + auctionId));

                    // Optionally notify seller
                    Player seller = Bukkit.getPlayer(auction.getSeller());
                    if (seller != null && seller.isOnline()) {
                        seller.sendMessage(Utils.getInstance().$(bidder.getName() + " has placed a bid of $" + economyHelper.formatBalance(bidAmount) + " on your auction."));
                    }

                    // Save the updated auction to the database
                    saveModule.saveAuction(auction, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            DebugLogger.getInstance().log(Level.INFO, "Auction ID: {0} updated with new highest bid of ${1} by {2}",
                                    new Object[]{auctionId, bidAmount, bidder.getName()});
                            // Notify all players about the new highest bid
                            Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                                    bidder.getName() + " has placed a bid of $" + economyHelper.formatBalance(bidAmount) +
                                    " on " + auction.getItem().getType().toString() + "."));
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            DebugLogger.getInstance().log(Level.SEVERE, "Failed to update auction ID: " + auctionId + " with new bid: ", throwable, throwable);
                            // Optionally, refund the bidder if saving fails
                            boolean refunded = economyHelper.depositBalance(bidder, bidAmount);
                            if (refunded) {
                                bidder.sendMessage(Utils.getInstance().$("Failed to place your bid due to a server error. Your bid amount has been refunded."));
                            } else {
                                bidder.sendMessage(Utils.getInstance().$("Failed to place your bid due to a server error. Please contact an administrator."));
                            }
                        }
                    });
                } else {
                    bidder.sendMessage(Utils.getInstance().$("You do not have enough money to place this bid."));
                }
            } else {
                bidder.sendMessage(Utils.getInstance().$("Your bid must be higher than the current bid."));
            }
        } else {
            bidder.sendMessage(Utils.getInstance().$("This auction does not exist or does not accept bids."));
        }
    }






    /**
     * Attempts to purchase an auction.
     *
     * @param buyer     The player buying the item.
     * @param auctionId The UUID of the auction.
     */
    public void buyAuction(Player buyer, UUID auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && !auction.isBidItem()) {
            double price = auction.getStartingPrice();
            DebugLogger.getInstance().log(Level.INFO, "Player {0} attempting to purchase auction ID: {1} for ${2}",
                    new Object[]{buyer.getName(), auctionId, price});

            // Attempt to withdraw money from buyer
            boolean withdrawn = economyHelper.withdrawBalance(buyer, price);
            if (!withdrawn) {
                buyer.sendMessage(Utils.getInstance().$("You do not have enough money to purchase this item."));
                DebugLogger.getInstance().log(Level.WARNING, "Player {0} has insufficient funds to purchase auction ID: {1}",
                        new Object[]{buyer.getName(), auctionId});
                return;
            }

            DebugLogger.getInstance().log(Level.INFO, "Player {0} successfully withdrew ${1} for auction ID: {2}",
                    new Object[]{buyer.getName(), price, auctionId});

            // Transfer money to seller
            UUID sellerId = auction.getSeller();
            Player seller = Bukkit.getPlayer(sellerId);
            if (seller != null && seller.isOnline()) {
                boolean deposited = economyHelper.depositBalance(seller, price);
                if (!deposited) {
                    // Refund the buyer if deposit fails
                    economyHelper.depositBalance(buyer, price);
                    buyer.sendMessage(Utils.getInstance().$("Transaction failed: Unable to credit seller."));
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to deposit ${0} to seller {1}. Refunding buyer {2}.",
                            new Object[]{price, seller.getName(), buyer.getName()});
                    return;
                }

                DebugLogger.getInstance().log(Level.INFO, "Player {0} successfully deposited ${1} to seller {2} for auction ID: {3}",
                        new Object[]{buyer.getName(), price, seller.getName(), auctionId});
                seller.sendMessage(Utils.getInstance().$("Your item has been sold to " + buyer.getName() + " for $" + economyHelper.formatBalance(price)));
            } else {
                // Seller is offline, add to pending balance
                PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                if (sellerData != null) {
                    double pendingBalance = sellerData.getPendingBalance();
                    sellerData.setPendingBalance(pendingBalance + price);
                    DebugLogger.getInstance().log(Level.INFO, "Seller {0} is offline. Added ${1} to pending balance.",
                            new Object[]{sellerId, price});

                    // Save the updated PlayerData
                    playerDataCache.savePlayerData(sellerId, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            DebugLogger.getInstance().log(Level.INFO, "Successfully saved pending balance for seller {0}", sellerId);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            DebugLogger.getInstance().log(Level.SEVERE, "Failed to save pending balance for seller " + sellerId + ": ", throwable, throwable);
                        }
                    });
                }
            }

            // Give item to buyer
            buyer.getInventory().addItem(auction.getItem());
            buyer.sendMessage(Utils.getInstance().$("You have purchased " + auction.getItem().getType() + " for $" + economyHelper.formatBalance(price)));
            DebugLogger.getInstance().log(Level.INFO, "Player {0} received item: {1}", new Object[]{buyer.getName(), auction.getItem().getType()});

            // Remove auction
            activeAuctions.remove(auctionId);
            DebugLogger.getInstance().log(Level.INFO, "Auction ID {0} removed from active auctions.", auctionId);

            // Delete auction from database
            saveModule.deleteAuction(auctionId, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Auction deleted successfully
                    DebugLogger.getInstance().log(Level.INFO, "Auction ID {0} deleted from database.", auctionId);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
                }
            });
        }

    }
}
