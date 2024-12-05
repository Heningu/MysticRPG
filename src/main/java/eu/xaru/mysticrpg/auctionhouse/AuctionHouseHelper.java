package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.*;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final DebugLoggerModule logger;
    private boolean auctionsLoaded = false;
    private final MysticCore plugin;

    public AuctionHouseHelper(EconomyHelper economyHelper) {
        this.economyHelper = economyHelper;
        this.activeAuctions = new HashMap<>();

        // Get SaveModule instance
        this.saveModule = ModuleManager.getInstance()
                .getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();

        // Get Logger instance
        this.logger = ModuleManager.getInstance()
                .getModuleInstance(DebugLoggerModule.class);

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
                    logger.log(Level.INFO, "Loaded auction with ID: " + auction.getAuctionId(), 0);
                }
                auctionsLoaded = true;
                logger.log(Level.INFO, "Total auctions loaded: " + activeAuctions.size(), 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                auctionsLoaded = true; // Prevent hanging
                logger.error("Failed to load auctions from the database: " + throwable.getMessage());
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
     */
    public UUID addAuction(UUID seller, ItemStack item,
                           double price, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller,
                item, price, endTime);
        activeAuctions.put(auctionId, auction);
        logger.log(Level.INFO, "Auction added to activeAuctions with ID: " + auctionId, 0);


        // Save auction to database
        saveModule.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Auction saved successfully
                logger.log(Level.INFO, "Auction " + auctionId +
                        " saved to database.", 0);

                // Notify players about the new auction
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = item.getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has listed " + itemName + " for $" +
                            economyHelper.formatBalance(price) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Failed to save auction to database: " +
                        throwable.getMessage());
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
     */
    public UUID addBidAuction(UUID seller, ItemStack item, double startingPrice, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller, item, startingPrice, endTime, true);
        activeAuctions.put(auctionId, auction);
        logger.log(Level.INFO, "Bid auction added to activeAuctions with ID: " + auctionId, 0);


        // Save auction to database
        saveModule.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.log(Level.INFO, "Bid auction " + auctionId + " saved to database.", 0);

                // Notify players about the new auction
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = item.getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has started an auction for " + itemName + " with a starting bid of $" +
                            economyHelper.formatBalance(startingPrice) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Failed to save bid auction to database: " + throwable.getMessage());
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
                // Auction deleted successfully
                logger.log(Level.INFO, "Auction " + auctionId +
                        " deleted from database.", 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Failed to delete auction from database: " +
                        throwable.getMessage());
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
                    logger.log(Level.INFO, "Auction ID: " + a.getAuctionId() + ", End Time: " + a.getEndTime() + ", Is Active: " + isActive, 0);
                    return isActive;
                })
                .collect(Collectors.toList());
        logger.log(Level.INFO, "getActiveAuctions: Found " + auctions.size() + " active auctions.", 0);
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
                    logger.log(Level.INFO, "Auction " + auctionId + " canceled by player " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.error("Failed to delete auction from database: " + throwable.getMessage());
                    player.sendMessage(Utils.getInstance().$("An error occurred while canceling the auction."));
                }
            });

            // Return the item to the player
            player.getInventory().addItem(auction.getItem());
        } else {
            player.sendMessage(Utils.getInstance().$("You cannot cancel this auction."));
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
            double buyerBalance = economyHelper.getBalance(buyer);
            double price = auction.getStartingPrice();

            if (buyerBalance >= price) {
                // Deduct money from buyer
                economyHelper.addBalance(buyer, -price);

                // Transfer money to seller
                UUID sellerId = auction.getSeller();
                Player seller = Bukkit.getPlayer(sellerId);
                if (seller != null && seller.isOnline()) {
                    economyHelper.addBalance(seller, price);
                    seller.sendMessage(Utils.getInstance().$("Your item has been sold to " + buyer.getName() + " for $" + economyHelper.formatBalance(price)));
                } else {
                    // Seller is offline, add to pending balance
                    PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                    if (sellerData != null) {
                        double pendingBalance = sellerData.getPendingBalance();
                        sellerData.setPendingBalance(pendingBalance + price);

                        // Save the updated PlayerData
                        playerDataCache.savePlayerData(sellerId, new Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                logger.log(Level.INFO, "Added pending balance to offline player " + sellerId, 0);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                logger.error("Failed to save pending balance for player " + sellerId + ": " + throwable.getMessage());
                            }
                        });
                    }
                }

                // Give item to buyer
                buyer.getInventory().addItem(auction.getItem());
                buyer.sendMessage(Utils.getInstance().$("You have purchased " + auction.getItem().getType() + " for $" + economyHelper.formatBalance(price)));

                // Remove auction
                activeAuctions.remove(auctionId);

                // Delete auction from database
                saveModule.deleteAuction(auctionId, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Auction deleted successfully
                        logger.log(Level.INFO, "Auction " + auctionId + " purchased by player " + buyer.getName(), 0);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("Failed to delete auction from database: " + throwable.getMessage());
                    }
                });
            } else {
                buyer.sendMessage(Utils.getInstance().$("You do not have enough money to purchase this item."));
            }
        } else {
            buyer.sendMessage(Utils.getInstance().$("This auction does not exist or cannot be bought directly."));
        }
    }

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
            if (bidAmount > auction.getCurrentBid()) {
                double bidderBalance = economyHelper.getBalance(bidder);
                if (bidderBalance >= bidAmount) {
                    // Refund previous highest bidder
                    if (auction.getHighestBidder() != null) {
                        UUID previousBidderId = auction.getHighestBidder();
                        if (!previousBidderId.equals(bidder.getUniqueId())) {
                            Player previousBidder = Bukkit.getPlayer(previousBidderId);
                            if (previousBidder != null && previousBidder.isOnline()) {
                                economyHelper.addBalance(previousBidder, auction.getCurrentBid());
                                previousBidder.sendMessage(Utils.getInstance().$("You have been outbid on auction " + auctionId));
                            } else {
                                // Add to pending balance for offline player
                                PlayerData bidderData = playerDataCache.getCachedPlayerData(previousBidderId);
                                if (bidderData != null) {
                                    double pendingBalance = bidderData.getPendingBalance();
                                    bidderData.setPendingBalance(pendingBalance + auction.getCurrentBid());
                                    playerDataCache.savePlayerData(previousBidderId, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            logger.log(Level.INFO, "Refunded previous bidder for auction " + auctionId, 0);
                                        }

                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            logger.error("Failed to refund previous bidder: " + throwable.getMessage());
                                        }
                                    });
                                }
                            }
                        }
                    }

                    // Deduct bid amount from bidder
                    economyHelper.addBalance(bidder, -bidAmount);

                    // Update auction with new bid
                    auction.setCurrentBid(bidAmount);
                    auction.setHighestBidder(bidder.getUniqueId());

                    bidder.sendMessage(Utils.getInstance().$("You are now the highest bidder on auction " + auctionId));

                    // Optionally notify seller
                    Player seller = Bukkit.getPlayer(auction.getSeller());
                    if (seller != null && seller.isOnline()) {
                        seller.sendMessage(Utils.getInstance().$(bidder.getName() + " has placed a bid of $" + economyHelper.formatBalance(bidAmount) + " on your auction."));
                    }

                    // Update auction in database
                    saveModule.saveAuction(auction, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            logger.log(Level.INFO, "Updated auction " + auctionId + " with new bid.", 0);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            logger.error("Failed to update auction with new bid: " + throwable.getMessage());
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
     * Checks for expired auctions and processes them.
     */
    public void checkExpiredAuctions() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Auction>> iterator = activeAuctions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Auction> entry = iterator.next();
            Auction auction = entry.getValue();
            if (auction.getEndTime() <= currentTime) {
                UUID sellerId = auction.getSeller();
                Player seller = Bukkit.getPlayer(sellerId);

                if (auction.isBidItem() && auction.getHighestBidder() != null) {
                    // Auction was a bid item and has a winner
                    UUID winnerId = auction.getHighestBidder();
                    Player winner = Bukkit.getPlayer(winnerId);

                    // Transfer money to seller
                    if (seller != null && seller.isOnline()) {
                        economyHelper.addBalance(seller, auction.getCurrentBid());
                        seller.sendMessage(Utils.getInstance().$("Your auction has ended. " +
                                "You sold " + auction.getItem().getType() + " for $" +
                                economyHelper.formatBalance(auction.getCurrentBid()) + "."));
                    } else {
                        // Seller is offline, add to pending balance
                        PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                        if (sellerData != null) {
                            double pendingBalance = sellerData.getPendingBalance();
                            sellerData.setPendingBalance(pendingBalance + auction.getCurrentBid());
                            playerDataCache.savePlayerData(sellerId, new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    logger.log(Level.INFO, "Added pending balance to offline seller " + sellerId, 0);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    logger.error("Failed to save pending balance for seller: " + throwable.getMessage());
                                }
                            });
                        }
                    }

                    // Give item to winner
                    if (winner != null && winner.isOnline()) {
                        winner.getInventory().addItem(auction.getItem());
                        winner.sendMessage(Utils.getInstance().$("You have won the auction for " +
                                auction.getItem().getType() + " with a bid of $" +
                                economyHelper.formatBalance(auction.getCurrentBid()) + "."));
                    } else {
                        // Winner is offline, add item to pending items
                        PlayerData winnerData = playerDataCache.getCachedPlayerData(winnerId);
                        if (winnerData != null) {
                            List<String> pendingItems = winnerData.getPendingItems();
                            String serializedItem = SaveHelper.itemStackToBase64(auction.getItem());
                            pendingItems.add(serializedItem);
                            winnerData.setPendingItems(pendingItems);
                            playerDataCache.savePlayerData(winnerId, new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    logger.log(Level.INFO, "Added auction item to pending items for winner " + winnerId, 0);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    logger.error("Failed to save pending items for winner: " + throwable.getMessage());
                                }
                            });
                        }
                    }

                } else {
                    // No bids, return item to seller
                    if (seller != null && seller.isOnline()) {
                        seller.getInventory().addItem(auction.getItem());
                        seller.sendMessage(Utils.getInstance().$("Your auction for " +
                                auction.getItem().getType() + " has expired and has been returned to you."));
                    } else {
                        // Seller is offline, add item to pending items
                        PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                        if (sellerData != null) {
                            List<String> pendingItems = sellerData.getPendingItems();
                            String serializedItem = SaveHelper.itemStackToBase64(auction.getItem());
                            pendingItems.add(serializedItem);
                            sellerData.setPendingItems(pendingItems);
                            playerDataCache.savePlayerData(sellerId, new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    logger.log(Level.INFO, "Added expired auction item to pending items for seller " + sellerId, 0);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    logger.error("Failed to save pending items for seller: " + throwable.getMessage());
                                }
                            });
                        }
                    }
                }

                // Remove auction from database
                saveModule.deleteAuction(auction.getAuctionId(), new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        logger.log(Level.INFO, "Expired auction " + auction.getAuctionId() + " removed from database.", 0);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("Failed to delete expired auction from database: " + throwable.getMessage());
                    }
                });

                iterator.remove();
            }
        }
    }
}
