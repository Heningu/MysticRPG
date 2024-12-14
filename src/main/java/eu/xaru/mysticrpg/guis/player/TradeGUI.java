package eu.xaru.mysticrpg.guis.player;

import eu.xaru.mysticrpg.player.interaction.trading.TradeManager;
import eu.xaru.mysticrpg.player.interaction.trading.TradeSession;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * TradeGUI manages the trading interface between two players.
 */
public class TradeGUI {

    // Static registry to keep track of open TradeGUIs for each player
    private static final Map<UUID, TradeGUI> openGUIs = new HashMap<>();

    private final TradeSession session;
    private final Player viewer;
    private final Window window;

    // Corrected Slot indices based on the structure
    private static final int PLAYER1_ITEMS_START = 10;    // Slot 10-12
    private static final int PLAYER2_ITEMS_START = 14;    // Slot 14-16
    private static final int READY_BUTTON_SLOT = 51;      // Slot 51
    private static final int CONFIRM_BUTTON_SLOT = 52;    // Slot 52
    private static final int CANCEL_BUTTON_SLOT = 45;     // Slot 45

    /**
     * Constructs a TradeGUI for a specific player within a trade session.
     *
     * @param session The active trade session.
     * @param viewer  The player viewing the GUI.
     */
    public TradeGUI(TradeSession session, Player viewer) {
        this.session = session;
        this.viewer = viewer;
        this.window = createGui();
        openGUIs.put(viewer.getUniqueId(), this);
        DebugLogger.getInstance().log(Level.INFO, "Opened TradeGUI for " + viewer.getName(), 0);
    }

    /**
     * Creates the GUI layout using InvUI.
     *
     * @return The constructed Window.
     */
    private Window createGui() {
        // Define the structure of the GUI
        String[] structure = {
                "# # # # # # # # #",
                "# P I I # T I I #",
                "# . . . # . . . #",
                "# . . . # . . . #",
                "# . . . # . . . #",
                "C # # # # # R C X"
        };

        // Create the GUI
        Gui gui = Gui.normal()
                .setStructure(structure)
                .addIngredient('#', createFiller())
                // Player 1 Items Header
                .addIngredient('P', createPlayerHeader(session.getPlayer1(), ChatColor.GREEN))
                // Player 2 Items Header
                .addIngredient('T', createPlayerHeader(session.getPlayer2(), ChatColor.BLUE))
                // Ready Button
                .addIngredient('R', createReadyButton())
                // Confirm Button
                .addIngredient('C', createConfirmButton())
                // Cancel Button
                .addIngredient('X', createCancelButton())
                .build();

        // Populate Player 1 Items
        populateItems(gui, session.getPlayer1Items(), PLAYER1_ITEMS_START, session.getPlayer1());

        // Populate Player 2 Items
        populateItems(gui, session.getPlayer2Items(), PLAYER2_ITEMS_START, session.getPlayer2());

        // Create and build the window
        Window createdWindow = Window.single()
                .setViewer(viewer)
                .setTitle(ChatColor.GOLD + "Trade with " + getOtherPlayer().getName())
                .setGui(gui)
                .build();

        return createdWindow;
    }

    /**
     * Creates a filler item (black stained glass pane).
     *
     * @return The filler Item.
     */
    private Item createFiller() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());
    }

    /**
     * Creates a header item for a player.
     *
     * @param player The player.
     * @param color  The color for the header text.
     * @return The header Item.
     */
    private Item createPlayerHeader(Player player, ChatColor color) {
        return new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(color + player.getName() + "'s Items")
                .addLoreLines("Click to manage your offered items")
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                // Optional: Implement any special behavior when clicking on the header
                // For now, do nothing
                clickPlayer.sendMessage(ChatColor.YELLOW + "Manage your items by clicking in your inventory.");
            }
        };
    }

    /**
     * Creates the Ready/Not Ready button based on the player's current state.
     *
     * @return The Ready/Not Ready Item.
     */
    private Item createReadyButton() {
        return new SimpleItem(new ItemBuilder(session.isPlayerReady(viewer) ? Material.GREEN_WOOL : Material.RED_WOOL)
                .setDisplayName(session.isPlayerReady(viewer) ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Not Ready")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "Click to toggle your ready state"
                )
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                toggleReady();
            }
        };
    }

    /**
     * Creates the Confirm Trade button.
     *
     * @return The Confirm Trade Item.
     */
    private Item createConfirmButton() {
        Material material = session.bothReady() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        String name = session.bothReady() ? ChatColor.GREEN + "Confirm Trade" : ChatColor.GRAY + "Waiting for both players to be ready";

        return new SimpleItem(new ItemBuilder(material)
                .setDisplayName(name)
                .addLoreLines(
                        "",
                        session.bothReady() ? ChatColor.GRAY + "Click to confirm the trade" : ChatColor.GRAY + "Both players must be ready to confirm."
                )
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                if (session.bothReady()) {
                    confirmTrade();
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Both players must be ready to confirm the trade.");
                }
            }
        };
    }

    /**
     * Creates the Cancel Trade button.
     *
     * @return The Cancel Trade Item.
     */
    private Item createCancelButton() {
        return new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Cancel Trade")
                .addLoreLines(
                        "",
                        ChatColor.GRAY + "Click to cancel the trade"
                )
                .addEnchantment(Enchantment.UNBREAKING, 1, true)
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                cancelTrade();
            }
        };
    }

    /**
     * Populates the GUI with the offered items from a player.
     *
     * @param gui       The GUI to populate.
     * @param items     The list of items offered by the player.
     * @param startSlot The starting slot index for the player's items.
     * @param owner     The owner of the items.
     */
    private void populateItems(Gui gui, List<ItemStack> items, int startSlot, Player owner) {
        for (int i = 0; i < items.size() && i < 3; i++) { // Adjusted to 3 items based on GUI layout
            ItemStack item = items.get(i);
            gui.setItem(startSlot + i, new SimpleItem(item) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                    // Remove the item from the trade
                    items.remove(item);
                    clickPlayer.getInventory().addItem(item.clone());
                    DebugLogger.getInstance().log(Level.INFO, clickPlayer.getName() + " removed an item from the trade.", 0);
                    updateGui();
                }
            });
        }

        // Fill remaining slots with placeholders
        for (int i = items.size(); i < 3; i++) { // Adjusted to 3 items based on GUI layout
            int slot = startSlot + i;
            gui.setItem(slot, createAddItemPlaceholder(owner));
        }
    }

    /**
     * Creates a placeholder item that allows players to add items to the trade.
     *
     * @param owner The player who can add items.
     * @return The placeholder Item.
     */
    private Item createAddItemPlaceholder(Player owner) {
        return new SimpleItem(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .setDisplayName(ChatColor.GRAY + "Add Item")
                .addLoreLines("Click to add an item from your inventory")
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                ItemStack cursorItem = clickPlayer.getInventory().getItemInMainHand();
                if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                    clickPlayer.sendMessage(ChatColor.RED + "You have no item in your main hand to add.");
                    return;
                }

                // Add the item to the trade
                boolean added = session.addItem(clickPlayer, cursorItem.clone());
                if (added) {
                    clickPlayer.getInventory().setItemInMainHand(null);
                    clickPlayer.sendMessage(ChatColor.GREEN + "Added " + cursorItem.getType().toString() + " to the trade.");
                    updateGui();
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Cannot add this item to the trade.");
                }
            }
        };
    }

    /**
     * Toggles the ready state of the viewer.
     */
    private void toggleReady() {
        boolean currentReady = session.isPlayerReady(viewer);
        session.setPlayerReady(viewer, !currentReady);
        viewer.sendMessage(ChatColor.YELLOW + "You are now " + (currentReady ? "not ready." : "ready."));
        updateGui();
    }

    /**
     * Confirms the trade if both players are ready.
     */
    private void confirmTrade() {
        if (session.bothReady()) {
            // Transfer items from Player1 to Player2
            for (ItemStack item : session.getPlayer1Items()) {
                session.getPlayer2().getInventory().addItem(item.clone());
            }
            // Transfer items from Player2 to Player1
            for (ItemStack item : session.getPlayer2Items()) {
                session.getPlayer1().getInventory().addItem(item.clone());
            }

            // Notify players
            session.getPlayer1().sendMessage(ChatColor.GREEN + "Trade completed successfully!");
            session.getPlayer2().sendMessage(ChatColor.GREEN + "Trade completed successfully!");

            // End trade session
            TradeManager.getInstance().endTrade(session);

            // Close GUIs
            closeTradeGUI(session.getPlayer1());
            closeTradeGUI(session.getPlayer2());
        } else {
            viewer.sendMessage(ChatColor.RED + "Both players must be ready to confirm the trade.");
        }
    }

    /**
     * Cancels the trade, returning all items to their respective owners.
     */
    private void cancelTrade() {
        // Return items to players
        session.returnItems();

        // Notify players
        session.getPlayer1().sendMessage(ChatColor.RED + "Trade has been canceled.");
        session.getPlayer2().sendMessage(ChatColor.RED + "Trade has been canceled.");

        // End trade session
        TradeManager.getInstance().endTrade(session);

        // Close GUIs
        closeTradeGUI(session.getPlayer1());
        closeTradeGUI(session.getPlayer2());
    }

    /**
     * Updates the GUI to reflect the current state of the trade.
     */
    public void updateGui() {
        // Since window.refresh does not exist, close and reopen the window to update
        window.close();
        open();
    }

    /**
     * Notifies the window to update its contents by closing and reopening it.
     */
    private void notifyWindow() {
        window.close();
        open();
    }

    /**
     * Opens the trade GUI for the viewer.
     */
    public void open() {
        window.open();
    }

    /**
     * Closes the trade GUI for a player.
     *
     * @param player The player whose GUI should be closed.
     */
    public static void closeTradeGUI(Player player) {
        TradeGUI gui = openGUIs.get(player.getUniqueId());
        if (gui != null && gui.window.isOpen()) {
            gui.window.close();
            openGUIs.remove(player.getUniqueId());
            DebugLogger.getInstance().log(Level.INFO, "Closed TradeGUI for " + player.getName(), 0);
        }
    }

    /**
     * Retrieves the other player involved in the trade.
     *
     * @return The other player.
     */
    private Player getOtherPlayer() {
        return session.getPlayer1().equals(viewer) ? session.getPlayer2() : session.getPlayer1();
    }

    /**
     * Retrieves the TradeGUI instance for a player.
     *
     * @param player The player to retrieve the TradeGUI for.
     * @return The TradeGUI instance if open, otherwise null.
     */
    public static TradeGUI getTradeGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
}
