package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.*;
import eu.xaru.mysticrpg.storage.database.SaveHelper;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AuctionHouseHelper {

    private final Map<UUID, Auction> activeAuctions;
    private final EconomyHelper economyHelper;
    private final AuctionStorage auctionStorage;
    private final PlayerDataCache playerDataCache;

    private boolean auctionsLoaded = false;
    private final MysticCore plugin;

    public AuctionHouseHelper(EconomyHelper economyHelper) {
        this.economyHelper = economyHelper;
        this.activeAuctions = new HashMap<>();

        this.playerDataCache = PlayerDataCache.getInstance();
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        
        // Initialize local file storage instead of database
        this.auctionStorage = new AuctionStorage(plugin);

        loadAuctionsFromStorage();
    }

    public boolean areAuctionsLoaded() {
        return auctionsLoaded;
    }

    private void loadAuctionsFromStorage() {
        auctionStorage.loadAuctions(new Callback<List<Auction>>() {
            @Override
            public void onSuccess(List<Auction> auctions) {
                for (Auction auction : auctions) {
                    activeAuctions.put(auction.getAuctionId(), auction);
                    DebugLogger.getInstance().log(Level.INFO, "Loaded auction with ID: " + auction.getAuctionId(), 0);
                }
                auctionsLoaded = true;
                DebugLogger.getInstance().log(Level.INFO, "Total auctions loaded: " + activeAuctions.size(), 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                auctionsLoaded = true;
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to load auctions from storage: ", throwable, throwable);
            }
        });
    }

    public UUID addAuction(UUID seller, CustomItem customItem, int price, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller, customItem, price, endTime);
        activeAuctions.put(auctionId, auction);
        DebugLogger.getInstance().log(Level.INFO, "Auction added to activeAuctions with ID: " + auctionId, 0);

        auctionStorage.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId +
                        " saved to storage.", 0);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = auction.getItem().getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has listed " + itemName + " for $" +
                            economyHelper.formatGold(price) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to save auction to database: ", throwable, throwable);
            }
        });

        return auctionId;
    }

    public UUID addSellOffer(UUID seller, ItemStack item, int price, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller, item, price, endTime, false); // false = sell offer, not auction
        activeAuctions.put(auctionId, auction);
        DebugLogger.getInstance().log(Level.INFO, "Sell offer added to activeAuctions with ID: " + auctionId, 0);

        auctionStorage.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Sell offer " + auctionId + " saved to storage.", 0);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = auction.getItem().getType().toString().replace("_", " ");
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has listed " + itemName + " for $" +
                            economyHelper.formatGold(price) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to save sell offer to storage: ", throwable, throwable);
            }
        });

        return auctionId;
    }

    public UUID addBidAuction(UUID seller, ItemStack item, int startingPrice, long duration) {
        long endTime = System.currentTimeMillis() + duration;
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction(auctionId, seller, item, startingPrice, endTime, true);
        activeAuctions.put(auctionId, auction);
        DebugLogger.getInstance().log(Level.INFO, "Bid auction added to activeAuctions with ID: " + auctionId, 0);

        auctionStorage.saveAuction(auction, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Bid auction " + auctionId + " saved to storage.", 0);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String itemName = auction.getItem().getType().toString();
                    Player sellerPlayer = Bukkit.getPlayer(seller);
                    String sellerName = sellerPlayer != null ? sellerPlayer.getName() : "A player";
                    Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                            sellerName + " has started an auction for " + itemName + " with a starting bid of $" +
                            economyHelper.formatGold(startingPrice) + "!"));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to save bid auction to database: ", throwable, throwable);
            }
        });

        return auctionId;
    }

    public void removeAuction(UUID auctionId) {
        activeAuctions.remove(auctionId);

        auctionStorage.deleteAuction(auctionId, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId +
                        " deleted from storage.", 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
            }
        });
    }

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

    public List<Auction> getPlayerAuctions(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        return activeAuctions.values().stream()
                .filter(a -> a.getSellerUUID().equals(playerUUID)
                        && a.getEndTime() > currentTime)
                .collect(Collectors.toList());
    }

    public Auction getAuctionById(UUID auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void cancelAuction(UUID auctionId, Player player) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getSellerUUID().equals(player.getUniqueId())) {
            activeAuctions.remove(auctionId);

            auctionStorage.deleteAuction(auctionId, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    player.sendMessage(Utils.getInstance().$("Auction canceled. The item has been returned to your inventory."));
                    DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId + " canceled by player " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
                }
            });

            player.getInventory().addItem(auction.getItem());
        } else {
            player.sendMessage(Utils.getInstance().$("You cannot cancel this auction."));
        }
    }

    public void placeBid(Player bidder, UUID auctionId, int bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.isBidItem()) {
            if (bidAmount > auction.getHighestBid()) {
                int bidderBalance = economyHelper.getBankGold(bidder);
                if (bidderBalance >= bidAmount) {
                    // Refund previous highest bidder
                    if (auction.getHighestBidder() != null) {
                        UUID previousBidderId = auction.getHighestBidder();
                        if (!previousBidderId.equals(bidder.getUniqueId())) {
                            Player previousBidder = Bukkit.getPlayer(previousBidderId);
                            if (previousBidder != null && previousBidder.isOnline()) {
                                // Refund by adding back bank gold
                                int prevBalance = economyHelper.getBankGold(previousBidder);
                                economyHelper.setBankGold(previousBidder, prevBalance + auction.getHighestBid());
                                previousBidder.sendMessage(Utils.getInstance().$("Your bid of $" + economyHelper.formatGold(auction.getHighestBid()) + " has been refunded."));
                                DebugLogger.getInstance().log(Level.INFO, "Refunded previous highest bidder {0} ${1} for auction ID: {2}",
                                        new Object[]{previousBidder.getName(), auction.getHighestBid(), auctionId});
                            } else {
                                // Previous bidder offline: pending balance
                                PlayerData previousBidderData = playerDataCache.getCachedPlayerData(previousBidderId);
                                if (previousBidderData != null) {
                                    int pendingBalance = previousBidderData.getPendingBalance();
                                    previousBidderData.setPendingBalance(pendingBalance + auction.getHighestBid());
                                    DebugLogger.getInstance().log(Level.INFO, "Previous highest bidder {0} is offline. Added ${1} to pending balance.",
                                            new Object[]{previousBidderId, auction.getHighestBid()});

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

                    // Deduct the bid amount from the bidder's bank gold
                    economyHelper.setBankGold(bidder, bidderBalance - bidAmount);

                    auction.setHighestBid(bidAmount);
                    auction.setHighestBidder(bidder.getUniqueId());
                    activeAuctions.put(auctionId, auction);

                    bidder.sendMessage(Utils.getInstance().$("You are now the highest bidder on auction " + auctionId));

                    Player seller = Bukkit.getPlayer(auction.getSellerUUID());
                    if (seller != null && seller.isOnline()) {
                        seller.sendMessage(Utils.getInstance().$(bidder.getName() + " has placed a bid of $" + economyHelper.formatGold(bidAmount) + " on your auction."));
                    }

                    auctionStorage.saveAuction(auction, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            DebugLogger.getInstance().log(Level.INFO, "Auction ID: {0} updated with new highest bid of ${1} by {2}",
                                    new Object[]{auctionId, bidAmount, bidder.getName()});
                            Bukkit.broadcastMessage(Utils.getInstance().$("&a[Auction House] &e" +
                                    bidder.getName() + " has placed a bid of $" + economyHelper.formatGold(bidAmount) +
                                    " on " + auction.getItem().getType().toString() + "."));
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            DebugLogger.getInstance().log(Level.SEVERE, "Failed to update auction ID: " + auctionId + " with new bid: ", throwable, throwable);
                            // Refund the bidder if saving fails
                            int currBalance = economyHelper.getBankGold(bidder);
                            economyHelper.setBankGold(bidder, currBalance + bidAmount);
                            bidder.sendMessage(Utils.getInstance().$("Failed to place your bid due to a server error. Your bid amount has been refunded."));
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

    public void buyAuction(Player buyer, UUID auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && !auction.isBidItem()) {
            int price = auction.getStartingPrice();
            DebugLogger.getInstance().log(Level.INFO, "Player {0} attempting to purchase auction ID: {1} for ${2}",
                    new Object[]{buyer.getName(), auctionId, price});

            int buyerBank = economyHelper.getBankGold(buyer);
            if (buyerBank < price) {
                buyer.sendMessage(Utils.getInstance().$("You do not have enough money to purchase this item."));
                DebugLogger.getInstance().log(Level.WARNING, "Player {0} has insufficient funds to purchase auction ID: {1}",
                        new Object[]{buyer.getName(), auctionId});
                return;
            }

            economyHelper.setBankGold(buyer, buyerBank - price);

            UUID sellerId = auction.getSellerUUID();
            int sellerCut = getSellerCut(sellerId, price);
            Player seller = Bukkit.getPlayer(sellerId);
            if (seller != null && seller.isOnline()) {
                int sellerBank = economyHelper.getBankGold(seller);
                economyHelper.setBankGold(seller, sellerBank + sellerCut);
                seller.sendMessage(Utils.getInstance().$("Your item has been sold to " + buyer.getName() + " for $" + economyHelper.formatGold(price)));
                if (sellerCut < price) {
                    int fee = price - sellerCut;
                    seller.sendMessage(Utils.getInstance().$("Auction house fee: $" + economyHelper.formatGold(fee)));
                }
            } else {
                PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                if (sellerData != null) {
                    int pendingBalance = sellerData.getPendingBalance();
                    sellerData.setPendingBalance(pendingBalance + sellerCut);
                    DebugLogger.getInstance().log(Level.INFO, "Seller {0} is offline. Added ${1} to pending balance.",
                            new Object[]{sellerId, sellerCut});

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

            buyer.getInventory().addItem(auction.getItem());
            buyer.sendMessage(Utils.getInstance().$("You have purchased " + auction.getItem().getType() + " for $" + economyHelper.formatGold(price)));
            DebugLogger.getInstance().log(Level.INFO, "Player {0} received item: {1}", new Object[]{buyer.getName(), auction.getItem().getType()});

            activeAuctions.remove(auctionId);
            DebugLogger.getInstance().log(Level.INFO, "Auction ID {0} removed from active auctions.", auctionId);

            auctionStorage.deleteAuction(auctionId, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "Auction ID {0} deleted from storage.", auctionId);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to delete auction from database: ", throwable, throwable);
                }
            });
        }
    }

    public void purchaseAuction(Player buyer, UUID auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && !auction.isBidItem()) {
            int price = auction.getStartingPrice();

            int buyerBank = economyHelper.getBankGold(buyer);
            if (buyerBank < price) {
                buyer.sendMessage(Utils.getInstance().$("You do not have enough money to purchase this item."));
                return;
            }

            economyHelper.setBankGold(buyer, buyerBank - price);

            UUID sellerId = auction.getSellerUUID();
            int sellerCut = getSellerCut(sellerId, price);
            Player seller = Bukkit.getPlayer(sellerId);
            if (seller != null && seller.isOnline()) {
                int sellerBank = economyHelper.getBankGold(seller);
                economyHelper.setBankGold(seller, sellerBank + sellerCut);
                if (sellerCut < price) {
                    int fee = price - sellerCut;
                    seller.sendMessage(Utils.getInstance().$("Item sold! Auction house fee: $" + economyHelper.formatGold(fee)));
                }
            } else {
                PlayerData sellerData = playerDataCache.getCachedPlayerData(sellerId);
                if (sellerData != null) {
                    int pendingBalance = sellerData.getPendingBalance();
                    sellerData.setPendingBalance(pendingBalance + sellerCut);
                    playerDataCache.savePlayerData(sellerId, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                        }
                    });
                }
            }

            CustomItem customItem = auction.getCustomItem();
            if (customItem != null) {
                ItemStack itemStack = customItem.toItemStack();
                buyer.getInventory().addItem(itemStack);
            } else {
                buyer.getInventory().addItem(auction.getItem());
            }

            buyer.sendMessage(Utils.getInstance().$("You have purchased " + auction.getItem().getType() + " for $" + economyHelper.formatGold(price)));

            activeAuctions.remove(auctionId);
            auctionStorage.deleteAuction(auctionId, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                }

                @Override
                public void onFailure(Throwable throwable) {
                }
            });
        } else {
            buyer.sendMessage(Utils.getInstance().$("This auction is not available for purchase."));
        }
    }
    
    /**
     * Calculate the fee for listing an auction (10% unless player has bypass permission)
     */
    private int calculateListingFee(Player player, int price) {
        if (player.hasPermission("mysticrpg.auction.nofee")) {
            return 0;
        }
        return (int) Math.ceil(price * 0.10); // 10% fee, rounded up
    }
    
    /**
     * Get the seller's cut after fees (90% unless they have bypass permission)
     */
    private int getSellerCut(UUID sellerUUID, int salePrice) {
        Player seller = Bukkit.getPlayer(sellerUUID);
        if (seller != null && seller.hasPermission("mysticrpg.auction.nofee")) {
            return salePrice; // No fee, full amount
        }
        return (int) Math.floor(salePrice * 0.90); // 90% of sale price
    }
    
    /**
     * Save all auctions to file (called on shutdown)
     */
    public void saveAllAuctions() {
        if (auctionStorage != null) {
            auctionStorage.saveAll(activeAuctions);
        }
    }
    
    /**
     * Check if player can afford to list an auction with the given price
     */
    public boolean canAffordListingFee(Player player, int price) {
        int fee = calculateListingFee(player, price);
        if (fee == 0) return true;
        
        int playerBalance = economyHelper.getBankGold(player);
        return playerBalance >= fee;
    }
    
    /**
     * Deduct listing fee from player
     */
    public boolean deductListingFee(Player player, int price) {
        int fee = calculateListingFee(player, price);
        if (fee == 0) return true;
        
        int playerBalance = economyHelper.getBankGold(player);
        if (playerBalance >= fee) {
            economyHelper.setBankGold(player, playerBalance - fee);
            if (fee > 0) {
                player.sendMessage(Utils.getInstance().$("Listing fee of $" + economyHelper.formatGold(fee) + " has been deducted."));
            }
            return true;
        }
        return false;
    }
}
