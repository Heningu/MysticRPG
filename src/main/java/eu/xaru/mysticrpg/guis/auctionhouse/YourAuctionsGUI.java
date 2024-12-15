package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.guis.admin.MobGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
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
 * Handles the Your Auctions GUI using InvUI, allowing players to view and manage their active auctions with pagination.
 */
public class YourAuctionsGUI {

    private final AuctionHouseMainMenu mainGUI;
    private EconomyModule economyModule;

    public YourAuctionsGUI(AuctionHouseMainMenu mainGUI) {
        this.mainGUI = mainGUI;
        this.economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
    }

    /**
     * Opens the Your Auctions GUI for the specified player.
     *
     * @param player The player to open the GUI for.
     */
    public void openAuctionHouseYourAuctionsGUI(Player player) {
        // Retrieve the player's active auctions
        List<Auction> playerAuctions = mainGUI.getAuctionHouseHelper().getPlayerAuctions(player.getUniqueId());

        if (playerAuctions.isEmpty()) {
            player.sendMessage(Utils.getInstance().$("You have no active auctions."));
            return;
        }

        // Convert auctions to InvUI Items
        List<Item> auctionItems = playerAuctions.stream().map(auction -> {
            // Clone the item's ItemStack to avoid mutating the original

            String type;

            if (auction.isBidItem()) {
                type = ChatColor.GRAY + "Current Bid: $" + economyModule.getEconomyHelper().formatGold(auction.getCurrentBid());
            } else {
                type = ChatColor.GRAY + "Price: $" + economyModule.getEconomyHelper().formatGold(auction.getStartingPrice());
            }


            ItemStack itemStack = auction.getItem().clone();
            ItemBuilder builder = new ItemBuilder(itemStack)
                    .setDisplayName(ChatColor.GREEN + itemStack.getType().toString().replace("_", " ") + " Auction")
                    .addLoreLines(
                            "",
                            type,
                            ChatColor.YELLOW + "Time Left: " + formatTimeLeft(auction.getEndTime() - System.currentTimeMillis()),
                            ChatColor.AQUA + "Auction ID: " + auction.getAuctionId(),
                            "",
                            ChatColor.RED + "Right-click to cancel this auction"
                    );

            Item auctionItem = new SimpleItem(builder) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                    if (clickType == ClickType.RIGHT) {
                        // Cancel the auction
                        try {
                            mainGUI.getAuctionHouseHelper().cancelAuction(auction.getAuctionId(), p);
                            p.sendMessage(Utils.getInstance().$("Auction cancelled successfully."));
                            // Refresh the GUI
                            openAuctionHouseYourAuctionsGUI(p);
                        } catch (Exception e) {
                            p.sendMessage(Utils.getInstance().$("Failed to cancel the auction."));
                            e.printStackTrace();
                        }
                    }
                }
            };

            return auctionItem;
        }).collect(Collectors.toList());

        Item backButton = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Back")
                .addLoreLines(
                        "",
                        "Click to return to the main menu.",
                        ""
                )
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                // Close the current window and open the main auction GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                AuctionHouseMainMenu mainMenu = new AuctionHouseMainMenu();

                mainMenu.openAuctionsGUI(p);
            }
        };

        // Placeholder Item for borders
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        Item controler = new ChangePageItem();


        // Create the paged GUI
        PagedGui<Item> pagedGui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B > # # # # # # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // Slots where auction items will be placed
                .addIngredient('#', border) // Border items
                .addIngredient('B', backButton) // Back button
                .addIngredient('>', controler) // Page indicator
                .setContent(auctionItems) // Set the content to the list of auction items
                .build();

        // Create and open the Window with the paged GUI
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + "Your Auctions")
                .setGui(pagedGui)
                .build();
        window.open();
    }

    public class ChangePageItem extends ControlItem<PagedGui<?>> {

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
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " from " + (gui.getPageAmount()) + " pages",
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addEnchantment(Enchantment.UNBREAKING,1,true)
                    .addAllItemFlags();
        }

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
