package eu.xaru.mysticrpg.games;

import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SlotGameGUI {

    private final SlotGameHelper slotGameHelper;

    /**
     * Each player's current bet (100, 1000, or 10000).
     */
    private final Map<UUID, Integer> betMap = new HashMap<>();

    /**
     * Tracks if a player is currently spinning, so we don't allow re-spins.
     */
    private final Map<UUID, Boolean> isSpinningMap = new HashMap<>();

    private Gui slotMachineGui;

    public SlotGameGUI(SlotGameHelper slotGameHelper) {
        this.slotGameHelper = slotGameHelper;
    }

    /**
     * 6x9 "Slot Machine" GUI layout:
     *   row=0 => ###...###
     *   row=1 => ###...###
     *   row=2 => A##...###
     *   row=3 => ###...###
     *   row=4 => #########
     *   row=5 => #########
     *
     * Column 8 in row=0 => absolute slot 8 => top-right corner
     */
    public void openSlotGUI(Player player) {
        // Default bet for new players
        betMap.putIfAbsent(player.getUniqueId(), 100);

        // Also ensure isSpinning is initially false
        isSpinningMap.putIfAbsent(player.getUniqueId(), false);

        String[] layout = {
                "#########",  // row=0 => slots 0..8
                "###...###",  // row=1 => slots 9..17
                "A##...###",  // row=2 => slots 18..26 (A is slot 18)
                "###...###",  // row=3 => slots 27..35
                "#########",  // row=4 => slots 36..44
                "#########"   // row=5 => slots 45..53
        };
        Structure structure = new Structure(layout);

        // Fill the border (#) with black glass
        structure.addIngredient('#',
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                        .setDisplayName(" ")
        );

        // Mark row=2's first column with an ARROW
        structure.addIngredient('A',
                new ItemBuilder(Material.ARROW)
                        .setDisplayName(ChatColor.YELLOW + "Result Row")
        );

        slotMachineGui = Gui.of(structure);

        // Place the toggle bet item at slot 49 => row=5, col=4
        slotMachineGui.setItem(49, new ToggleBetItem(player));
        // Place the spin button at slot 50 => row=5, col=5
        slotMachineGui.setItem(50, new SpinItem(player));

        // Slot 8 => top-right corner => "BalanceItem"
        refreshBalanceItem(player);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.DARK_GREEN + "Slot Machine")
                .setGui(slotMachineGui)
                .build();

        window.open();
    }

    /**
     * Refresh the top-right corner item (slot 8) to show the player's current bank gold.
     */
    private void refreshBalanceItem(Player player) {
        slotMachineGui.setItem(8, new BalanceItem(player));
    }

    /**
     * Spin each of the 3 columns (3,4,5) top -> bottom for 3 seconds.
     * Final items in row=2 for columns 3,4,5 => check if 3-of-a-kind => payout or lose.
     */
    public void spinReels(Player player) {
        UUID uuid = player.getUniqueId();

        // If already spinning, do nothing
        if (isSpinningMap.getOrDefault(uuid, false)) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You are already spinning the slot machine!"));
            return;
        }

        betMap.putIfAbsent(uuid, 100);
        int bet = betMap.get(uuid);

        // Check if the player can afford the bet
        int bankGold = slotGameHelper.getEconomy().getBankGold(player);
        if (bankGold < bet) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You don't have enough gold in the bank to spin."));
            return;
        }

        // Deduct the bet
        slotGameHelper.getEconomy().setBankGold(player, bankGold - bet);

        // Mark the player as currently spinning
        isSpinningMap.put(uuid, true);

        player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW
                + "Spinning each column for " + bet + " gold... Good luck!"));

        // Refresh the top-right balance
        refreshBalanceItem(player);

        // Columns: 3,4,5
        int[] columns = {3, 4, 5};

        // We'll store final items for each column in row=2.
        Map<Integer, Material> finalColItems = new HashMap<>();

        // 3 columns -> each column is 3 seconds => 12 updates at 5 ticks each => 36 updates total.
        AtomicInteger c = new AtomicInteger(0);

        new BukkitRunnable() {
            @Override
            public void run() {
                int count = c.getAndIncrement();
                if (count >= 36) {
                    // Done all columns
                    this.cancel();

                    // Re-enable spinning
                    isSpinningMap.put(uuid, false);

                    // Now check if finalColItems for columns=3,4,5 are 3-of-a-kind
                    Material col3 = finalColItems.get(3);
                    Material col4 = finalColItems.get(4);
                    Material col5 = finalColItems.get(5);

                    if (col3 == null || col4 == null || col5 == null) {
                        player.sendMessage(Utils.getInstance().$(ChatColor.RED
                                + "Slot machine error. Missing final items."));
                        return;
                    }

                    // 3-of-a-kind => pay out
                    if (col3 == col4 && col4 == col5) {
                        slotGameHelper.getEconomy().addBankGold(player, bet * 10);
                        player.sendMessage(Utils.getInstance().$(ChatColor.GREEN
                                + "You won! You received " + (bet * 10) + " gold!"));
                    } else {
                        player.sendMessage(Utils.getInstance().$(ChatColor.RED
                                + "You lost! Better luck next time."));
                    }

                    // Refresh balance after final result
                    refreshBalanceItem(player);
                    return;
                }

                // figure out which column => colStep in [0..2]
                int colStep = count / 12;       // 0..2
                int stepInCol = count % 12;     // 0..11
                int column = columns[colStep];  // 3..5

                // For "vertical" effect: shift row2->row3, row1->row2, new random->row1
                int row1Base = 9;   // row=1
                int row2Base = 18;  // row=2
                int row3Base = 27;  // row=3

                int row1Slot = row1Base + column;
                int row2Slot = row2Base + column;
                int row3Slot = row3Base + column;

                Material oldRow1 = getMaterialFromSlot(row1Slot);
                Material oldRow2 = getMaterialFromSlot(row2Slot);

                // row2->row3
                if (oldRow2 != null) {
                    slotMachineGui.setItem(row3Slot, new SimpleItem(
                            new ItemBuilder(oldRow2).setDisplayName(ChatColor.YELLOW + oldRow2.name())
                    ));
                } else {
                    slotMachineGui.setItem(row3Slot, new SimpleItem(
                            new ItemBuilder(Material.AIR).setDisplayName(" ")
                    ));
                }

                // row1->row2
                if (oldRow1 != null) {
                    slotMachineGui.setItem(row2Slot, new SimpleItem(
                            new ItemBuilder(oldRow1).setDisplayName(ChatColor.YELLOW + oldRow1.name())
                    ));
                } else {
                    slotMachineGui.setItem(row2Slot, new SimpleItem(
                            new ItemBuilder(Material.AIR).setDisplayName(" ")
                    ));
                }

                // new random => row1
                Material newMat = slotGameHelper.spinSlots().get(0);
                slotMachineGui.setItem(row1Slot, new SimpleItem(
                        new ItemBuilder(newMat).setDisplayName(ChatColor.YELLOW + newMat.name())
                ));

                // If final step in column => store middle row's item
                if (stepInCol == 11) {
                    Material finalMat = getMaterialFromSlot(row2Slot);
                    finalColItems.put(column, finalMat);
                }
            }
        }.runTaskTimer(slotGameHelper.getPlugin(), 0L, 5L);
    }

    /**
     * Reads the current item in the GUI slot => returns its Material if non-empty, else null.
     */
    private Material getMaterialFromSlot(int slotIndex) {
        var invItem = slotMachineGui.getItem(slotIndex);
        if (invItem instanceof SimpleItem si) {
            var provider = si.getItemProvider();
            if (provider instanceof ItemBuilder ib) {
                return ib.getMaterial();
            }
        }
        return null;
    }

    /**
     * A clickable item that cycles the player's bet: 100 -> 1000 -> 10000 -> 100
     */
    private class ToggleBetItem extends AbstractItem {
        private final Player player;

        public ToggleBetItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            int currentBet = betMap.getOrDefault(player.getUniqueId(), 100);
            return new ItemBuilder(Material.GOLD_BLOCK)
                    .setDisplayName(ChatColor.GOLD + "Current Bet: " + currentBet);
        }

        @Override
        public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
            event.setCancelled(true);

            // If user is spinning, can't change bet either
            if (isSpinningMap.getOrDefault(clickPlayer.getUniqueId(), false)) {
                clickPlayer.sendMessage(Utils.getInstance().$(ChatColor.RED + "You cannot change the bet while spinning!"));
                return;
            }

            UUID uuid = clickPlayer.getUniqueId();
            int currBet = betMap.getOrDefault(uuid, 100);

            int newBet;
            if (currBet == 100) {
                newBet = 1000;
            } else if (currBet == 1000) {
                newBet = 10000;
            } else {
                newBet = 100;
            }

            betMap.put(uuid, newBet);
            clickPlayer.sendMessage(Utils.getInstance().$(ChatColor.YELLOW
                    + "Your bet is now " + newBet + " gold."));

            // Refresh GUI item
            notifyWindows();
        }
    }

    /**
     * The "SPIN" emerald. If spinning => do nothing. If not in the correct GUI => reopen. Otherwise => spin.
     */
    private class SpinItem extends AbstractItem {
        private final Player player;

        public SpinItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.EMERALD_BLOCK)
                    .setDisplayName(ChatColor.GREEN + "SPIN!");
        }

        @Override
        public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
            event.setCancelled(true);

            // Are we already spinning?
            if (isSpinningMap.getOrDefault(clickPlayer.getUniqueId(), false)) {
                clickPlayer.sendMessage(Utils.getInstance().$(ChatColor.RED + "You are already spinning!"));
                return;
            }

            // Title check
            String displayedTitle = ChatColor.stripColor(event.getView().getTitle());
            String expectedTitle = ChatColor.stripColor(ChatColor.DARK_GREEN + "Slot Machine");

            if (!displayedTitle.equals(expectedTitle)) {
                openSlotGUI(clickPlayer);

                // Let the GUI open, then spin
                Bukkit.getScheduler().runTaskLater(
                        slotGameHelper.getPlugin(),
                        () -> spinReels(clickPlayer),
                        3L
                );
                return;
            }

            // Otherwise, user is in the correct GUI => spin
            spinReels(clickPlayer);
        }
    }

    /**
     * Shows the player's current bank gold in the top-right slot (slot=8).
     */
    private class BalanceItem extends AbstractItem {
        private final Player player;

        public BalanceItem(Player player) {
            this.player = player;
        }

        @Override
        public ItemProvider getItemProvider() {
            int bankGold = slotGameHelper.getEconomy().getBankGold(player);
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName(ChatColor.GOLD + "Balance: " + bankGold + " gold")
                    .addLoreLines("", ChatColor.GRAY + "Your current bank gold.");
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            // Do nothing but prevent taking the item
            event.setCancelled(true);
        }
    }
}
