package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.storage.SaveHelper;
import org.bukkit.inventory.ItemStack;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.UUID;

/**
 * Represents an auction in the auction house.
 */
public class Auction {

    private UUID auctionId;
    private UUID seller;
    private String itemData; // Serialized ItemStack
    private double startingPrice;
    private double currentBid;
    private UUID highestBidder;
    private long endTime;
    private boolean isBidItem; // Indicates if the auction allows bidding

    @BsonIgnore
    private ItemStack item; // Transient field, not stored directly in MongoDB

    // Required for MongoDB POJO codec
    public Auction() {
    }

    // Constructor for fixed-price auction
    public Auction(UUID auctionId, UUID seller, ItemStack item, double price, long endTime) {
        this.auctionId = auctionId;
        this.seller = seller;
        this.item = item;
        this.startingPrice = price;
        this.currentBid = price;
        this.endTime = endTime;
        this.itemData = serializeItemStack(item);
        this.isBidItem = false;
    }

    // Constructor for bidding auction
    public Auction(UUID auctionId, UUID seller, ItemStack item, double startingPrice, long endTime, boolean isBidItem) {
        this.auctionId = auctionId;
        this.seller = seller;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.endTime = endTime;
        this.itemData = serializeItemStack(item);
        this.isBidItem = isBidItem;
    }

    // Getters and setters

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getSeller() {
        return seller;
    }

    public void setSeller(UUID seller) {
        this.seller = seller;
    }

    public ItemStack getItem() {
        if (item == null && itemData != null) {
            item = deserializeItemStack(itemData);
        }
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        this.itemData = serializeItemStack(item);
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isBidItem() {
        return isBidItem;
    }

    public void setBidItem(boolean bidItem) {
        isBidItem = bidItem;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
        this.item = deserializeItemStack(itemData);
    }

    // Serialization methods
    private String serializeItemStack(ItemStack item) {
        return SaveHelper.itemStackToBase64(item);
    }

    private ItemStack deserializeItemStack(String data) {
        return SaveHelper.itemStackFromBase64(data);
    }
}
