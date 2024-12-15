package eu.xaru.mysticrpg.auctionhouse;

import java.util.Objects;
import java.util.UUID;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.storage.SaveHelper;

/**
 * Represents an auction in the auction house.
 */
public class Auction {

    private UUID auctionId;
    private UUID seller;
    private String itemData; // Serialized ItemStack
    private int startingPrice;
    private int currentBid;
    private UUID highestBidder;
    private long endTime;
    private boolean isBidItem; // Indicates if the auction allows bidding

    @BsonIgnore
    private ItemStack item; // Transient field, not stored directly in MongoDB

    @BsonIgnore
    private CustomItem customItem; // New field for CustomItem

    // Required for MongoDB POJO codec
    public Auction() {
    }

    // Constructor for fixed-price auction
    public Auction(UUID auctionId, UUID seller, ItemStack item, int price, long endTime) {
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
    public Auction(UUID auctionId, UUID seller, ItemStack item, int startingPrice, long endTime, boolean isBidItem) {
        this.auctionId = auctionId;
        this.seller = seller;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.endTime = endTime;
        this.itemData = serializeItemStack(item);
        this.isBidItem = isBidItem;
    }

    // New constructor for CustomItem
    public Auction(UUID auctionId, UUID seller, CustomItem customItem, int price, long endTime) {
        this.auctionId = auctionId;
        this.seller = seller;
        this.customItem = customItem;
        this.startingPrice = price;
        this.currentBid = price;
        this.endTime = endTime;
        this.isBidItem = false;
        this.item = customItem.toItemStack(); // Convert CustomItem to ItemStack
        this.itemData = serializeItemStack(this.item);
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

    public String getSellerName() {
        return Objects.requireNonNull(Bukkit.getPlayer(seller)).getName();
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

    public int getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(int startingPrice) {
        this.startingPrice = startingPrice;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
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

    // Getter and Setter for customItem
    public CustomItem getCustomItem() {
        if (customItem == null && item != null) {
            customItem = CustomItemUtils.fromItemStack(item);
        }
        return customItem;
    }

    public void setCustomItem(CustomItem customItem) {
        this.customItem = customItem;
        this.item = customItem.toItemStack();
        this.itemData = serializeItemStack(this.item);
    }

    // Serialization methods
    private String serializeItemStack(ItemStack item) {
        return SaveHelper.itemStackToBase64(item);
    }

    private ItemStack deserializeItemStack(String data) {
        return SaveHelper.itemStackFromBase64(data);
    }

    /**
     * **Added Methods for Consistency with placeBid Method**
     */

    /**
     * Gets the highest bid for the auction.
     *
     * @return The current highest bid.
     */
    public int getHighestBid() {
        return this.getCurrentBid();
    }

    /**
     * Sets the highest bid for the auction.
     *
     * @param highestBid The new highest bid amount.
     */
    public void setHighestBid(int highestBid) {
        this.setCurrentBid(highestBid);
    }
}
