package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseHelper;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;

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

import xyz.xenondevs.invui.item.ItemWrapper;

public class SellGUI {

    private final AuctionHouseHelper auctionHouseHelper;
    private final EconomyHelper economyHelper;
    private final Map<UUID, Integer> priceMap;
    private final Map<UUID, Long> durationMap;
    private final Map<UUID, Boolean> bidMap;
    private final Map<UUID, ItemStack> sellingItems;

    public SellGUI(AuctionHouseHelper auctionHouseHelper, EconomyHelper economyHelper) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
        this.priceMap = new HashMap<>();
        this.durationMap = new HashMap<>();
        this.bidMap = new HashMap<>();
        this.sellingItems = new HashMap<>();
    }

    public void openSellGUI(Player player) {
        // Set default values if not present
        priceMap.putIfAbsent(player.getUniqueId(), 100); // Default price
        durationMap.putIfAbsent(player.getUniqueId(), 86400000L); // Default duration (24h)
        bidMap.putIfAbsent(player.getUniqueId(), false); // Default to fixed price

        // Build the GUI
        Gui gui = buildGui(player);

        // Open the GUI
        Window window = Window.single()
                .setViewer(player)
                .setTitle(Utils.getInstance().$("Auction House - Sell"))
                .setGui(gui)
                .build();

        window.open();
    }

    private Gui buildGui(Player player) {
        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "));

        Item decreasePrice = new DecreasePriceItem(player);
        Item increasePrice = new IncreasePriceItem(player);
        Item priceDisplay = new PriceDisplayItem(player);
        Item toggleAuctionType = new ToggleAuctionTypeItem(player);
        Item confirmItem = new ConfirmItem(player);
        Item changeDuration = new ChangeDurationItem(player);
        Item itemSlot = new ItemSlotItem(player);

        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . d p i . . #",
                        "# . . t c n . . #",
                        "# # # # # # # # #")
                .addIngredient('#', filler)
                .addIngredient('.', itemSlot)
                // ...existing code...
                .addIngredient('d', decreasePrice)
                .addIngredient('i', increasePrice)
                .addIngredient('p', priceDisplay)
                .addIngredient('t', toggleAuctionType)
                .addIngredient('c', confirmItem)
                .addIngredient('n', changeDuration)
                // ...existing code...
                .build();
    }

    // Define the custom items used in the GUI

    private class DecreasePriceItem extends AbstractItem {
        private final Player player;

        public DecreasePriceItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.REDSTONE_BLOCK)
                    .setDisplayName(Utils.getInstance().$("Decrease Price"));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            price = Math.max(0, price - 10);
            priceMap.put(player.getUniqueId(), price);
            notifyWindows();
            event.setCancelled(true);
        }
    }

    private class IncreasePriceItem extends AbstractItem {
        private final Player player;

        public IncreasePriceItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.EMERALD_BLOCK)
                    .setDisplayName(Utils.getInstance().$("Increase Price"));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            price += 10;
            priceMap.put(player.getUniqueId(), price);
            notifyWindows();
            event.setCancelled(true);
        }
    }

    private class PriceDisplayItem extends AbstractItem {
        private final Player player;

        public PriceDisplayItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName(Utils.getInstance().$("Current Price: $" + economyHelper.formatBalance(price)))
                    .addLoreLines(Utils.getInstance().$("Right-click to set custom price"));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (clickType.isRightClick()) {
                promptCustomPrice(player);
                player.closeInventory();
            }
            event.setCancelled(true);
        }
    }

    private class ToggleAuctionTypeItem extends AbstractItem {
        private final Player player;

        public ToggleAuctionTypeItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            boolean isBidItem = bidMap.getOrDefault(player.getUniqueId(), false);
            String type = isBidItem ? "Bidding" : "Fixed Price";
            return new ItemBuilder(Material.GOLDEN_HOE)
                    .setDisplayName(Utils.getInstance().$("Auction Type: " + type))
                    .addLoreLines(Utils.getInstance().$("Click to switch auction type"));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            boolean isBidItem = !bidMap.getOrDefault(player.getUniqueId(), false);
            bidMap.put(player.getUniqueId(), isBidItem);
            notifyWindows();
            event.setCancelled(true);
        }
    }

    private class ConfirmItem extends AbstractItem {
        private final Player player;

        public ConfirmItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            boolean isBidItem = bidMap.getOrDefault(player.getUniqueId(), false);
            String action = isBidItem ? "Confirm Auction" : "Confirm Sale";
            return new ItemBuilder(Material.GREEN_WOOL)
                    .setDisplayName(Utils.getInstance().$(action));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            ItemStack itemToSell = sellingItems.get(player.getUniqueId());
            if (itemToSell == null || itemToSell.getType() == Material.AIR) {
                player.sendMessage(Utils.getInstance().$("You must place an item to sell."));
                return;
            }

            int price = priceMap.getOrDefault(player.getUniqueId(), 100);
            long duration = durationMap.getOrDefault(player.getUniqueId(), 86400000L); // Default 24h
            boolean isBidItem = bidMap.getOrDefault(player.getUniqueId(), false);

            CustomItem customItem = CustomItemUtils.fromItemStack(itemToSell);

            if (customItem != null) {
                if (isBidItem) {
                    auctionHouseHelper.addBidAuction(player.getUniqueId(), itemToSell, price, duration);
                } else {
                    auctionHouseHelper.addAuction(player.getUniqueId(), customItem, price, duration);
                }

                player.sendMessage(Utils.getInstance().$("Your item has been listed for " + (isBidItem ? "auction!" : "sale!")));

                sellingItems.remove(player.getUniqueId());
                player.closeInventory();
            } else {
                player.sendMessage(Utils.getInstance().$("Invalid item."));
            }

            event.setCancelled(true);
        }
    }

    private class ChangeDurationItem extends AbstractItem {
        private final Player player;

        public ChangeDurationItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            long duration = durationMap.getOrDefault(player.getUniqueId(), 86400000L);
            return new ItemBuilder(Material.CLOCK)
                    .setDisplayName(Utils.getInstance().$("Change Duration"))
                    .addLoreLines(Utils.getInstance().$("Current Duration: " + formatDuration(duration)));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            long currentDuration = durationMap.getOrDefault(player.getUniqueId(), 86400000L);
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
            durationMap.put(player.getUniqueId(), currentDuration);
            notifyWindows();
            event.setCancelled(true);
        }
    }

    private class ItemSlotItem extends AbstractItem {
        private final Player player;

        public ItemSlotItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            ItemStack item = sellingItems.get(player.getUniqueId());
            if (item == null) {
                return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName(Utils.getInstance().$("Place Item Here"));
            } else {
                return new ItemWrapper(item);
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                ItemStack itemToSell = cursor.clone();
                itemToSell.setAmount(1);
                sellingItems.put(player.getUniqueId(), itemToSell);
                cursor.setAmount(cursor.getAmount() - 1);
                event.setCursor(cursor.getAmount() > 0 ? cursor : null);
                notifyWindows();
            } else {
                ItemStack item = sellingItems.get(player.getUniqueId());
                if (item != null) {
                    player.getInventory().addItem(item);
                    sellingItems.remove(player.getUniqueId());
                    notifyWindows();
                }
            }
            event.setCancelled(true);
        }
    }

    private void promptCustomPrice(Player player) {
        player.sendMessage(Utils.getInstance().$("Enter your custom price in chat:"));
        // Handle chat input elsewhere
    }

    private String formatDuration(long millis) {
        long hours = millis / 3600000L;
        return hours + "h";
    }
}
