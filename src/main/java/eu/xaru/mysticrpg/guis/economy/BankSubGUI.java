package eu.xaru.mysticrpg.guis.economy;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankSubGUI {
    private final Player player;
    private final EconomyHelper economyHelper;
    private final boolean isDeposit; // true if deposit mode, false if withdraw
    private Window window;

    // Static map to track which player is awaiting custom input
    private static final Map<UUID, PendingAmountContext> pendingInput = new HashMap<>();

    public BankSubGUI(Player player, EconomyHelper economyHelper, boolean isDeposit) {
        this.player = player;
        this.economyHelper = economyHelper;
        this.isDeposit = isDeposit;
    }

    public void open() {
        String action = isDeposit ? "Deposit" : "Withdraw";

        // Structure 3x9
        // For clarity changed structure lines to match ingredients
        // #########
        // ##C5H B##
        // #########
        Structure structure = new Structure(
                "# # # # # # # # #",
                "# # C # 5 # H # #",
                "B # # # # # # # #"
        );

        // Border (#)
        structure.addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")));


        // Custom (C)
        structure.addIngredient('C', new SimpleItem(
                new ItemBuilder(Material.PAPER)
                        .setDisplayName(ChatColor.BLUE + action + " Custom Amount")
                        .addLoreLines(ChatColor.GRAY + "Click to input a custom amount via chat.")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                // Close this GUI and prompt user in chat
                if (window != null) window.close();
                p.sendMessage(Utils.getInstance().$("Please type the amount of gold you want to " + (isDeposit ? "deposit" : "withdraw") + " in the chat."));
                p.sendMessage(Utils.getInstance().$("Type 'cancel' to cancel the operation."));
                // Record that we are waiting for this player's input
                pendingInput.put(p.getUniqueId(), new PendingAmountContext(isDeposit));
            }
        });

        // 50% (5)
        structure.addIngredient('5', new SimpleItem(
                new ItemBuilder(Material.IRON_INGOT)
                        .setDisplayName(ChatColor.GREEN + action + " 50%")
                        .addLoreLines(ChatColor.GRAY + "Click to " + action.toLowerCase() + " half of your gold.")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                int amount = isDeposit ? economyHelper.getHeldGold(p) / 2 : economyHelper.getBankGold(p) / 2;
                performAction(p, amount);
            }
        });

        // 100% (H)
        structure.addIngredient('H', new SimpleItem(
                new ItemBuilder(Material.DIAMOND)
                        .setDisplayName(ChatColor.GREEN + action + " 100%")
                        .addLoreLines(ChatColor.GRAY + "Click to " + action.toLowerCase() + " all of your gold.")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                int amount = isDeposit ? economyHelper.getHeldGold(p) : economyHelper.getBankGold(p);
                performAction(p, amount);
            }
        });

        // Back (B)
        structure.addIngredient('B', new SimpleItem(
                new ItemBuilder(Material.BARRIER)
                        .setDisplayName(ChatColor.RED + "Back")
                        .addLoreLines(ChatColor.GRAY + "Click to go back")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);

                new BankGUI(p).open();
                if (window != null) window.close();
            }
        });

        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

        Gui gui = Gui.normal()
                .setStructure(structure)
                .setBackground(background)
                .build();

        window = Window.single()
                .setViewer(player)
                .setTitle(action + " Gold")
                .setGui(gui)
                .build();
        window.open();
    }

    private void performAction(Player p, int amount) {
        if (amount <= 0) {
            p.sendMessage(Utils.getInstance().$(ChatColor.RED + "Invalid amount."));
            reopenMainGUI(p);
            return;
        }

        boolean success = isDeposit ? economyHelper.depositToBank(p, amount) : economyHelper.withdrawFromBank(p, amount);
        if (success) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        reopenMainGUI(p);
        if (window != null) window.close();
    }

    private void reopenMainGUI(Player p) {
        new BankGUI(p).open();
    }

    // This method will be called from the chat listener when the player inputs an amount.
    public static void handleChatInput(Player player, String message, EconomyHelper economyHelper) {
        UUID uuid = player.getUniqueId();
        PendingAmountContext context = pendingInput.get(uuid);
        if (context == null) {
            return; // Not waiting for this player
        }

        // We need the plugin instance, assuming MysticCore is your main class:
        JavaPlugin plugin = MysticCore.getPlugin(MysticCore.class);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Utils.getInstance().$("Operation cancelled."));
            pendingInput.remove(uuid);
            // Schedule sync task to reopen GUI
            Bukkit.getScheduler().runTask(plugin, () -> new BankGUI(player).open());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Invalid number. Operation cancelled."));
            pendingInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> new BankGUI(player).open());
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Invalid amount. Operation cancelled."));
            pendingInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> new BankGUI(player).open());
            return;
        }

        boolean success = context.isDeposit ? economyHelper.depositToBank(player, amount) : economyHelper.withdrawFromBank(player, amount);
        if (success) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        pendingInput.remove(uuid);

        // Open BankGUI on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> new BankGUI(player).open());
    }



    private static class PendingAmountContext {
        final boolean isDeposit;

        PendingAmountContext(boolean isDeposit) {
            this.isDeposit = isDeposit;
        }
    }
    public static boolean isAwaitingInput(UUID uuid) {
        // return true if player uuid is in pendingInput map
        return pendingInput.containsKey(uuid);
    }
}
