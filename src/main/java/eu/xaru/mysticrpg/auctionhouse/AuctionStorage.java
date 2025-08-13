package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.database.SaveHelper;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles local file-based storage of auction data in YAML format.
 * Replaces database storage with persistent local files.
 */
public class AuctionStorage {
    
    private final JavaPlugin plugin;
    private final File auctionsFile;
    private YamlConfiguration yaml;
    
    public AuctionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Create auctions folder if it doesn't exist
        File auctionsFolder = new File(plugin.getDataFolder(), "auctions");
        if (!auctionsFolder.exists()) {
            auctionsFolder.mkdirs();
        }
        
        this.auctionsFile = new File(auctionsFolder, "auctions.yml");
        this.yaml = new YamlConfiguration();
        
        // Load existing auctions from file
        if (auctionsFile.exists()) {
            try {
                yaml.load(auctionsFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load auctions.yml!");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Save an auction to the YAML file asynchronously.
     */
    public void saveAuction(Auction auction, Callback<Void> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String key = auction.getAuctionId().toString();
                
                // Save auction data to YAML
                yaml.set(key + ".sellerUUID", auction.getSellerUUID().toString());
                yaml.set(key + ".sellerName", auction.getSellerName());
                yaml.set(key + ".itemData", auction.getItemData());
                yaml.set(key + ".startingPrice", auction.getStartingPrice());
                yaml.set(key + ".currentBid", auction.getCurrentBid());
                yaml.set(key + ".highestBidder", auction.getHighestBidder() != null ? auction.getHighestBidder().toString() : null);
                yaml.set(key + ".endTime", auction.getEndTime());
                yaml.set(key + ".isBidItem", auction.isBidItem());
                yaml.set(key + ".createdTime", System.currentTimeMillis()); // Track creation time for sorting
                
                yaml.save(auctionsFile);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auction.getAuctionId() + " saved to file.", 0);
                
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to save auction " + auction.getAuctionId(), e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }
    
    /**
     * Load all auctions from the YAML file asynchronously.
     */
    public void loadAuctions(Callback<List<Auction>> callback) {
        CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            boolean fileModified = false;
            
            try {
                Set<String> keys = yaml.getKeys(false);
                
                for (String key : keys) {
                    try {
                        UUID auctionId = UUID.fromString(key);
                        UUID sellerUUID = UUID.fromString(yaml.getString(key + ".sellerUUID"));
                        String sellerName = yaml.getString(key + ".sellerName");
                        String itemData = yaml.getString(key + ".itemData");
                        int startingPrice = yaml.getInt(key + ".startingPrice");
                        int currentBid = yaml.getInt(key + ".currentBid");
                        String highestBidderStr = yaml.getString(key + ".highestBidder");
                        UUID highestBidder = highestBidderStr != null ? UUID.fromString(highestBidderStr) : null;
                        long endTime = yaml.getLong(key + ".endTime");
                        boolean isBidItem = yaml.getBoolean(key + ".isBidItem");
                        
                        // Check if auction has expired
                        if (currentTime > endTime) {
                            // Remove expired auction from file
                            yaml.set(key, null);
                            fileModified = true;
                            
                            // TODO: Handle expired auction - return item to seller or give to highest bidder
                            DebugLogger.getInstance().log(Level.INFO, "Removed expired auction: " + auctionId, 0);
                            continue;
                        }
                        
                        // Deserialize item
                        ItemStack item = SaveHelper.itemStackFromBase64(itemData);
                        if (item == null) {
                            DebugLogger.getInstance().log(Level.WARNING, "Failed to deserialize item for auction: " + auctionId, 0);
                            continue;
                        }
                        
                        // Create auction object
                        Auction auction = new Auction();
                        auction.setAuctionId(auctionId);
                        auction.setSellerUUID(sellerUUID);
                        auction.setSellerName(sellerName);
                        auction.setItemData(itemData);
                        auction.setItem(item);
                        auction.setStartingPrice(startingPrice);
                        auction.setCurrentBid(currentBid);
                        auction.setHighestBidder(highestBidder);
                        auction.setEndTime(endTime);
                        auction.setBidItem(isBidItem);
                        
                        auctions.add(auction);
                        
                    } catch (Exception e) {
                        DebugLogger.getInstance().error("Failed to load auction with key: " + key, e);
                        // Remove corrupted auction entry
                        yaml.set(key, null);
                        fileModified = true;
                    }
                }
                
                // Save file if any expired/corrupted auctions were removed
                if (fileModified) {
                    yaml.save(auctionsFile);
                }
                
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load auctions from file", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
                return auctions;
            }
            
            return auctions;
        }).thenAccept(auctions -> {
            if (callback != null) {
                callback.onSuccess(auctions);
            }
        }).exceptionally(throwable -> {
            if (callback != null) {
                callback.onFailure(throwable);
            }
            return null;
        });
    }
    
    /**
     * Delete an auction from the YAML file asynchronously.
     */
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String key = auctionId.toString();
                yaml.set(key, null);
                yaml.save(auctionsFile);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                DebugLogger.getInstance().log(Level.INFO, "Auction " + auctionId + " deleted from file.", 0);
                
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to delete auction " + auctionId, e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }
    
    /**
     * Save all auctions synchronously (for shutdown).
     */
    public void saveAll(Map<UUID, Auction> activeAuctions) {
        try {
            // Clear existing entries
            for (String key : yaml.getKeys(false)) {
                yaml.set(key, null);
            }
            
            // Save all active auctions
            for (Auction auction : activeAuctions.values()) {
                String key = auction.getAuctionId().toString();
                
                yaml.set(key + ".sellerUUID", auction.getSellerUUID().toString());
                yaml.set(key + ".sellerName", auction.getSellerName());
                yaml.set(key + ".itemData", auction.getItemData());
                yaml.set(key + ".startingPrice", auction.getStartingPrice());
                yaml.set(key + ".currentBid", auction.getCurrentBid());
                yaml.set(key + ".highestBidder", auction.getHighestBidder() != null ? auction.getHighestBidder().toString() : null);
                yaml.set(key + ".endTime", auction.getEndTime());
                yaml.set(key + ".isBidItem", auction.isBidItem());
                yaml.set(key + ".createdTime", System.currentTimeMillis());
            }
            
            yaml.save(auctionsFile);
            DebugLogger.getInstance().log(Level.INFO, "All auctions saved to file during shutdown.", 0);
            
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to save all auctions during shutdown", e);
        }
    }
}