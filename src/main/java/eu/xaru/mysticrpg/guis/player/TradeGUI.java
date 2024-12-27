package eu.xaru.mysticrpg.guis.player;

import eu.xaru.mysticrpg.player.interaction.trading.TradeManager;
import eu.xaru.mysticrpg.player.interaction.trading.TradeSession;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.logging.Level;

/**
 * A TradeGUI using an InvUI "Approach A" structure-based method,
 * but without using SlotElement or .update(). Instead, we define
 * placeholders and rebuild the entire GUI whenever we need to refresh.
 *
 * Layout (6 rows, 9 columns => 54 placeholders):
 *
 * Row 0: # # # # # # # # #
 * Row 1: # A B C # a b c #
 * Row 2: # D E F # d e f #
 * Row 3: # G H I # g h i #
 * Row 4: # J K L # j k l #
 * Row 5: R O # # # # # # X
 *
 * Where:
 *   '#' => filler
 *   'A'..'L' => placeholders for Player1's items (12 total)
 *   'a'..'l' => placeholders for Player2's items (12 total)
 *   'R' => "Ready" toggle
 *   'O' => "Confirm"
 *   'X' => "Cancel"
 *
 * For dynamic changes (like item added/removed), we close & reopen the GUI
 * for both players rather than calling something like .update().
 */
public class TradeGUI {

    private static final Map<UUID, TradeGUI> OPEN_TRADE_GUIS = new HashMap<>();

    // The 6×9 layout with placeholders
    private static final String[] LAYOUT = {
            "# # # # # # # # #",
            "# A B C # a b c #",
            "# D E F # d e f #",
            "# G H I # g h i #",
            "# J K L # j k l #",
            "R O # # # # # # X"
    };

    private final TradeSession session;
    private final Player viewer;

    private Gui gui;
    private Window window;

    public TradeGUI(TradeSession session, Player viewer) {
        this.session = session;
        this.viewer = viewer;
    }

    /**
     * Opens (builds) the GUI for this viewer. We define placeholders
     * via .addIngredient(...) + a Supplier<Item>.
     */
    public void open() {
        buildGui(); // Build the actual GUI
        window.open();
    }

    /**
     * Closes & re-opens the GUI (forcing a rebuild). Call this if you want to refresh.
     */
    private void reopenGui() {
        if (window != null && window.isOpen()) {
            window.close();
        }
        buildGui();
        window.open();
    }

    /**
     * Actually build the GUI from placeholders. We remove references to
     * setItem(...) or manual row/col logic.
     */
    private void buildGui() {
        // 1) Create a normal GUI from our 6×9 layout
        Gui.Builder.Normal builder = Gui.normal().setStructure(LAYOUT);

        // 2) Filler => '#'
        builder.addIngredient('#', getFillerItem());

        // 3) Player1 placeholders => 'A'..'L' => 12 total
        for (char c = 'A'; c <= 'L'; c++) {
            char finalC = c;
            builder.addIngredient(c, () -> getPlayer1Item(finalC));
        }

        // 4) Player2 placeholders => 'a'..'l'
        for (char c = 'a'; c <= 'l'; c++) {
            char finalC = c;
            builder.addIngredient(c, () -> getPlayer2Item(finalC));
        }

        // 5) 'R' => readiness toggle
        builder.addIngredient('R', () -> createReadyItem());

        // 6) 'O' => confirm
        builder.addIngredient('O', () -> createConfirmItem());

        // 7) 'X' => cancel
        builder.addIngredient('X', () -> createCancelItem());

        // 8) Build the GUI
        this.gui = builder.build();

        // 9) Create the Window
        this.window = Window.single()
                .setViewer(viewer)
                .setGui(gui)
                .setTitle(ChatColor.GOLD + "Trading with " + getOtherPlayerName())
                .setCloseable(true)
                .build();

        // 10) Add close handler => if trade in progress, cancel
        this.window.addCloseHandler(() -> {
            if (TradeManager.getInstance().getTradeSession(viewer) == session) {
                // cancel
                session.returnItems();
                TradeManager.getInstance().endTrade(session);

                Player other = getOtherPlayer();
                if (other != null && other.isOnline()) {
                    other.sendMessage(ChatColor.RED + viewer.getName() + " canceled the trade.");
                    TradeGUI otherGUI = OPEN_TRADE_GUIS.get(other.getUniqueId());
                    if (otherGUI != null) {
                        otherGUI.forceClose();
                    }
                }
            }
            OPEN_TRADE_GUIS.remove(viewer.getUniqueId());
        });

        // 11) Register in static map
        OPEN_TRADE_GUIS.put(viewer.getUniqueId(), this);
    }

    /**
     * Returns a filler "black glass" item.
     */
    private Item getFillerItem() {
        return new SimpleItem(
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("")
        );
    }

    /**
     * Returns the Item for a given placeholder char 'A'..'L' (index 0..11).
     * If there's a trade item at that index, show it, otherwise "add item".
     */
    private Item getPlayer1Item(char c) {
        int index = c - 'A'; // 'A'=0, 'B'=1, etc.
        if (index < 0 || index >= 12) return getFillerItem();

        List<ItemStack> p1Items = session.getPlayer1Items();
        if (index < p1Items.size()) {
            // There's an item => show it
            ItemStack stack = p1Items.get(index);
            return createOfferedItem(stack, session.getPlayer1());
        } else {
            // "add item"
            return createAddItemPlaceholder(session.getPlayer1());
        }
    }

    /**
     * For a given 'a'..'l' => index 0..11 for player2's items.
     */
    private Item getPlayer2Item(char c) {
        int index = c - 'a'; // 'a'=0, 'b'=1, etc.
        if (index < 0 || index >= 12) return getFillerItem();

        List<ItemStack> p2Items = session.getPlayer2Items();
        if (index < p2Items.size()) {
            // There's an item => show it
            ItemStack stack = p2Items.get(index);
            return createOfferedItem(stack, session.getPlayer2());
        } else {
            return createAddItemPlaceholder(session.getPlayer2());
        }
    }

    /**
     * "Offered item" => if the correct owner clicks, remove from trade.
     */
    private Item createOfferedItem(ItemStack stack, Player owner) {
        return new SimpleItem(stack) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.setCancelled(true);

                if (!clickPlayer.equals(owner)) {
                    clickPlayer.sendMessage(ChatColor.RED + "You cannot remove another player's items!");
                    return;
                }

                // remove from session
                if (owner.equals(session.getPlayer1())) {
                    session.getPlayer1Items().remove(stack);
                } else {
                    session.getPlayer2Items().remove(stack);
                }
                owner.getInventory().addItem(stack.clone());

                // reset readiness
                session.setPlayerReady(session.getPlayer1(), false);
                session.setPlayerReady(session.getPlayer2(), false);

                // forcibly rebuild for both
                updateTradeUIForBothPlayers(false);
            }
        };
    }

    /**
     * Placeholder for "add item." SHIFT-click from inventory is typical,
     * but you could do direct click logic if desired.
     */
    private Item createAddItemPlaceholder(Player owner) {
        return new SimpleItem(
                new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .setDisplayName(ChatColor.GRAY + "Add Item")
                        .addLoreLines("", ChatColor.YELLOW + "SHIFT-click an item from your inventory")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.setCancelled(true);
                clickPlayer.sendMessage(ChatColor.YELLOW + "Use SHIFT-click in your inventory to add an item to the trade.");
            }
        };
    }

    /**
     * "Ready"/"Not Ready" => toggles for the viewer.
     */
    private Item createReadyItem() {
        boolean isReady = session.isPlayerReady(viewer);
        Material mat = isReady ? Material.GREEN_WOOL : Material.RED_WOOL;
        String name = isReady ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Not Ready";

        return new SimpleItem(new ItemBuilder(mat).setDisplayName(name)) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.setCancelled(true);

                boolean current = session.isPlayerReady(clickPlayer);
                session.setPlayerReady(clickPlayer, !current);

                // forcibly rebuild
                updateTradeUIForBothPlayers(false);
            }
        };
    }

    /**
     * "Confirm" => if both are ready, finalize the trade.
     */
    private Item createConfirmItem() {
        boolean bothReady = session.bothReady();
        Material mat = bothReady ? Material.EMERALD_BLOCK : Material.COAL_BLOCK;
        String name = bothReady
                ? ChatColor.GREEN + "Confirm Trade"
                : ChatColor.DARK_GRAY + "Waiting for both players";

        return new SimpleItem(new ItemBuilder(mat).setDisplayName(name)) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.setCancelled(true);

                if (!session.bothReady()) {
                    clickPlayer.sendMessage(ChatColor.RED + "Both players must be ready to confirm!");
                    return;
                }
                finalizeTrade();
            }
        };
    }

    /**
     * "Cancel" => end trade, return items
     */
    private Item createCancelItem() {
        return new SimpleItem(
                new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "Cancel Trade")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                event.setCancelled(true);

                session.returnItems();
                TradeManager.getInstance().endTrade(session);

                session.getPlayer1().sendMessage(ChatColor.RED + "Trade canceled.");
                session.getPlayer2().sendMessage(ChatColor.RED + "Trade canceled.");

                updateTradeUIForBothPlayers(true);
            }
        };
    }

    /**
     * Actually finalize => exchange items, end trade, close GUIs.
     */
    private void finalizeTrade() {
        // p2 -> p1
        for (ItemStack s : session.getPlayer2Items()) {
            session.getPlayer1().getInventory().addItem(s.clone());
        }
        // p1 -> p2
        for (ItemStack s : session.getPlayer1Items()) {
            session.getPlayer2().getInventory().addItem(s.clone());
        }
        session.getPlayer1Items().clear();
        session.getPlayer2Items().clear();

        TradeManager.getInstance().endTrade(session);

        session.getPlayer1().sendMessage(ChatColor.GREEN + "Trade completed successfully!");
        session.getPlayer2().sendMessage(ChatColor.GREEN + "Trade completed successfully!");

        updateTradeUIForBothPlayers(true);
    }

    /**
     * Rebuild or close both players' GUIs.
     * If close==true => forcibly close, else forcibly re-open so changes show.
     */
    private void updateTradeUIForBothPlayers(boolean close) {
        Player p1 = session.getPlayer1();
        Player p2 = session.getPlayer2();

        for (Player p : new Player[]{p1, p2}) {
            TradeGUI tgui = OPEN_TRADE_GUIS.get(p.getUniqueId());
            if (tgui != null) {
                if (close) {
                    tgui.forceClose();
                } else {
                    // forcibly re-build & open for them => "refresh"
                    tgui.reopenGui();
                }
            }
        }
    }

    /**
     * Force-close THIS window, removing from the map.
     */
    private void forceClose() {
        if (window != null && window.isOpen()) {
            window.close();
        }
        OPEN_TRADE_GUIS.remove(viewer.getUniqueId());
    }

    /**
     * Public static: if SHIFT-click logic modifies the trade,
     * we forcibly re-open for the player(s).
     */
    public static void refreshPlayerGUI(Player player) {
        TradeGUI tg = OPEN_TRADE_GUIS.get(player.getUniqueId());
        if (tg != null) {
            tg.reopenGui(); // forcibly rebuild & re-open
        }
    }

    public static void closeTradeGUI(Player player) {
        TradeGUI tg = OPEN_TRADE_GUIS.get(player.getUniqueId());
        if (tg != null) {
            tg.forceClose();
        }
    }

    public static TradeGUI getTradeGUI(Player player) {
        return OPEN_TRADE_GUIS.get(player.getUniqueId());
    }

    private Player getOtherPlayer() {
        return session.getPlayer1().equals(viewer) ? session.getPlayer2() : session.getPlayer1();
    }

    private String getOtherPlayerName() {
        Player other = getOtherPlayer();
        return (other != null) ? other.getName() : "???";
    }
}
