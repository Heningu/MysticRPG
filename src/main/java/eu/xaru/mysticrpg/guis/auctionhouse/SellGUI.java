package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.AuctionDuration;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseHelper;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Sell GUI that matches the exact requirements:
 * - Item selection slot in middle of second top row
 * - Auction type toggle (Auction vs Sell Offer)
 * - Price entry with chat input
 * - Duration selector with predefined options
 * - Confirm & Cancel buttons
 */
public class SellGUI {

    private final AuctionHouseHelper auctionHouseHelper;
    private final EconomyHelper economyHelper;
    
    // Player data storage
    private final Map<UUID, ItemStack> selectedItems = new HashMap<>();
    private final Map<UUID, Integer> priceMap = new HashMap<>();
    private final Map<UUID, Integer> durationIndexMap = new HashMap<>(); // Index in AuctionDuration arrays
    private final Map<UUID, Boolean> isAuctionMap = new HashMap<>(); // true = auction, false = sell offer
    
    // Chat input tracking
    private static final Map<UUID, SellGUI> pendingPriceInput = new HashMap<>();

    public SellGUI(AuctionHouseHelper auctionHouseHelper, EconomyHelper economyHelper) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
    }

    public void openSellGUI(Player player) {
        // Initialize defaults if not present
        priceMap.putIfAbsent(player.getUniqueId(), 100);
        durationIndexMap.putIfAbsent(player.getUniqueId(), 3); // Default to 24 hours
        isAuctionMap.putIfAbsent(player.getUniqueId(), false); // Default to sell offer

        Gui gui = buildGui(player);
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GREEN + "Create Auction")
                .setGui(gui)
                .build();

        window.open();
    }

    private Gui buildGui(Player player) {
        // Black glass pane filler
        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "));
        
        // Main items
        Item itemSelectionSlot = new ItemSelectionSlot(player);
        Item auctionTypeToggle = new AuctionTypeToggleItem(player);
        Item priceEntryItem = new PriceEntryItem(player);
        Item durationSelector = new DurationSelectorItem(player);
        Item confirmButton = new ConfirmItem(player);
        Item cancelButton = new CancelItem(player);

        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # # x # # # #", // x = item selection slot (middle of second row)
                        "# # t # p # d # #", // t = auction type, p = price, d = duration
                        "# # # c # n # # #"  // c = confirm, n = cancel
                )
                .addIngredient('#', filler)
                .addIngredient('x', itemSelectionSlot)
                .addIngredient('t', auctionTypeToggle)
                .addIngredient('p', priceEntryItem)
                .addIngredient('d', durationSelector)
                .addIngredient('c', confirmButton)
                .addIngredient('n', cancelButton)
                .build();
    }

    /**
     * Item Selection Slot - middle of second top row
     */
    private class ItemSelectionSlot extends AbstractItem {
        private final Player player;

        public ItemSelectionSlot(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            ItemStack selectedItem = selectedItems.get(player.getUniqueId());
            
            if (selectedItem != null) {
                return new ItemBuilder(selectedItem.clone())
                        .addLoreLines("", ChatColor.YELLOW + "Click to deselect and return to inventory");
            } else {
                return new ItemBuilder(Material.BARRIER)
                        .setDisplayName(ChatColor.RED + "No item selected")
                        .addLoreLines("", ChatColor.GRAY + "Click an item in your inventory to select");
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            ItemStack selectedItem = selectedItems.get(player.getUniqueId());
            
            // If item is selected, return it to inventory
            if (selectedItem != null) {
                player.getInventory().addItem(selectedItem);
                selectedItems.remove(player.getUniqueId());
                player.sendMessage(Utils.getInstance().$("Item returned to your inventory."));
                notifyWindows();
            }
            
            event.setCancelled(true);
        }
    }

    /**
     * Auction Type Toggle Button
     */
    private class AuctionTypeToggleItem extends AbstractItem {
        private final Player player;

        public AuctionTypeToggleItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            boolean isAuction = isAuctionMap.getOrDefault(player.getUniqueId(), false);
            
            if (isAuction) {
                return new ItemBuilder(Material.ENCHANTED_BOOK)
                        .setDisplayName(ChatColor.GOLD + "Auction Mode")
                        .addLoreLines(
                                "",
                                ChatColor.GRAY + "Players can bid on your item",
                                ChatColor.GRAY + "Highest bidder wins",
                                "",
                                ChatColor.YELLOW + "Click to switch to Sell Offer"
                        );
            } else {
                return new ItemBuilder(Material.EMERALD)
                        .setDisplayName(ChatColor.GREEN + "Sell Offer Mode")
                        .addLoreLines(
                                "",
                                ChatColor.GRAY + "Set a fixed price",
                                ChatColor.GRAY + "First buyer gets the item",
                                "",
                                ChatColor.YELLOW + "Click to switch to Auction"
                        );
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            boolean currentMode = isAuctionMap.getOrDefault(player.getUniqueId(), false);
            isAuctionMap.put(player.getUniqueId(), !currentMode);
            notifyWindows();
            event.setCancelled(true);
        }
    }

    /**
     * Price Entry Item - opens chat input
     */
    private class PriceEntryItem extends AbstractItem {
        private final Player player;

        public PriceEntryItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            boolean isAuction = isAuctionMap.getOrDefault(player.getUniqueId(), false);
            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            
            String title = isAuction ? "Starting Bid" : "Sell Price";
            Material material = isAuction ? Material.GOLD_INGOT : Material.DIAMOND;
            
            return new ItemBuilder(material)
                    .setDisplayName(ChatColor.YELLOW + title)
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current: " + ChatColor.WHITE + economyHelper.formatGold(price) + " coins",
                            "",
                            ChatColor.GREEN + "Click to enter new price",
                            ChatColor.GRAY + "(Type in chat)"
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            // Close GUI and request chat input
            player.closeInventory();
            pendingPriceInput.put(player.getUniqueId(), SellGUI.this);
            
            boolean isAuction = isAuctionMap.getOrDefault(player.getUniqueId(), false);
            String priceType = isAuction ? "starting bid" : "sell price";
            
            player.sendMessage(Utils.getInstance().$("Enter the " + priceType + " in coins (numbers only):"));
            event.setCancelled(true);
        }
    }

    /**
     * Duration Selector - cycles through predefined durations
     */
    private class DurationSelectorItem extends AbstractItem {
        private final Player player;

        public DurationSelectorItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            int durationIndex = durationIndexMap.getOrDefault(player.getUniqueId(), 3);
            String durationName = AuctionDuration.getDurationName(durationIndex);
            
            return new ItemBuilder(Material.CLOCK)
                    .setDisplayName(ChatColor.AQUA + "Duration")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Selected: " + ChatColor.WHITE + durationName,
                            ChatColor.GRAY + "The auction will run for this duration",
                            "",
                            ChatColor.GREEN + "Click to cycle through options"
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            int currentIndex = durationIndexMap.getOrDefault(player.getUniqueId(), 3);
            int nextIndex = AuctionDuration.getNextDurationIndex(currentIndex);
            durationIndexMap.put(player.getUniqueId(), nextIndex);
            
            String newDurationName = AuctionDuration.getDurationName(nextIndex);
            player.sendMessage(Utils.getInstance().$("Duration changed to: " + newDurationName));
            
            notifyWindows();
            event.setCancelled(true);
        }
    }

    /**
     * Confirm Button - creates the auction
     */
    private class ConfirmItem extends AbstractItem {
        private final Player player;

        public ConfirmItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.LIME_CONCRETE)
                    .setDisplayName(ChatColor.GREEN + "Confirm")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Create the auction with",
                            ChatColor.GRAY + "the current settings",
                            "",
                            ChatColor.GREEN + "Click to confirm"
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            ItemStack selectedItem = selectedItems.get(player.getUniqueId());
            
            if (selectedItem == null) {
                player.sendMessage(Utils.getInstance().$("You must select an item first!"));
                event.setCancelled(true);
                return;
            }
            
            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            int durationIndex = durationIndexMap.getOrDefault(player.getUniqueId(), 3);
            boolean isAuction = isAuctionMap.getOrDefault(player.getUniqueId(), false);
            
            long duration = AuctionDuration.getDuration(durationIndex);
            
            // Check if player can afford listing fee
            if (!auctionHouseHelper.canAffordListingFee(player, price)) {
                player.sendMessage(Utils.getInstance().$("You cannot afford the listing fee for this price!"));
                event.setCancelled(true);
                return;
            }
            
            // Deduct listing fee
            if (!auctionHouseHelper.deductListingFee(player, price)) {
                player.sendMessage(Utils.getInstance().$("Failed to deduct listing fee."));
                event.setCancelled(true);
                return;
            }

            // Create the auction
            UUID auctionId;
            if (isAuction) {
                // Create bid auction
                auctionId = auctionHouseHelper.addBidAuction(player.getUniqueId(), selectedItem, price, duration);
            } else {
                // Create sell offer - now works with regular ItemStacks
                auctionId = auctionHouseHelper.addSellOffer(player.getUniqueId(), selectedItem, price, duration);
            }

            String actionType = isAuction ? "auction" : "sale";
            player.sendMessage(Utils.getInstance().$("Your item has been listed for " + actionType + "!"));

            // Clear player data
            selectedItems.remove(player.getUniqueId());
            player.closeInventory();
            
            event.setCancelled(true);
        }
    }

    /**
     * Cancel Button - returns item and closes GUI
     */
    private class CancelItem extends AbstractItem {
        private final Player player;

        public CancelItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.RED_CONCRETE)
                    .setDisplayName(ChatColor.RED + "Cancel")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Close this menu and return",
                            ChatColor.GRAY + "any selected item",
                            "",
                            ChatColor.RED + "Click to cancel"
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            // Return selected item if any
            ItemStack selectedItem = selectedItems.get(player.getUniqueId());
            if (selectedItem != null) {
                player.getInventory().addItem(selectedItem);
                selectedItems.remove(player.getUniqueId());
                player.sendMessage(Utils.getInstance().$("Item returned to your inventory."));
            }
            
            player.closeInventory();
            event.setCancelled(true);
        }
    }

    // Static methods for handling chat input
    public static boolean isPendingPriceInput(Player player) {
        return pendingPriceInput.containsKey(player.getUniqueId());
    }

    public static void handlePriceInput(Player player, String input) {
        SellGUI gui = pendingPriceInput.remove(player.getUniqueId());
        if (gui == null) return;

        try {
            int price = Integer.parseInt(input);
            if (price < 0) {
                player.sendMessage(Utils.getInstance().$("Price cannot be negative."));
            } else {
                gui.priceMap.put(player.getUniqueId(), price);
                player.sendMessage(Utils.getInstance().$("Price set to " + gui.economyHelper.formatGold(price) + " coins."));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Utils.getInstance().$("Invalid price. Please enter a number."));
        }

        // Reopen the GUI
        gui.openSellGUI(player);
    }

    // Inventory click handler for item selection
    public void handleInventoryClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Move item to selection slot
        ItemStack itemToSelect = clickedItem.clone();
        itemToSelect.setAmount(1); // Only take one item
        
        // Return previous item if any
        ItemStack previousItem = selectedItems.get(player.getUniqueId());
        if (previousItem != null) {
            player.getInventory().addItem(previousItem);
        }
        
        // Remove one of the clicked item from inventory
        if (clickedItem.getAmount() > 1) {
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else {
            player.getInventory().remove(clickedItem);
        }
        
        selectedItems.put(player.getUniqueId(), itemToSelect);
        player.sendMessage(Utils.getInstance().$("Item selected: " + itemToSelect.getType().toString().replace("_", " ")));
    }
}