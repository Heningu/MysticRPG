package eu.xaru.mysticrpg.guis.economy;

import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

class BankSubGUI {
    private final Player player;
    private final EconomyHelper economyHelper;
    private final boolean isDeposit; // true if deposit mode, false if withdraw
    private Window window;

    public BankSubGUI(Player player, EconomyHelper economyHelper, boolean isDeposit) {
        this.player = player;
        this.economyHelper = economyHelper;
        this.isDeposit = isDeposit;
    }

    public void open() {
        String action = isDeposit ? "Deposit" : "Withdraw";

        // Structure 3x9
        // #########
        // ##C5H B##
        // #########
        Structure structure = new Structure(
                "#########",
                "##C5H B##",
                "#########"
        );

        // Border (#)
        structure.addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")));

        // Custom (C)
        // For simplicity, we just choose a fixed custom amount = 10
        // You can later implement chat input or a separate GUI for custom amounts.
        structure.addIngredient('C', new SimpleItem(
                new ItemBuilder(Material.PAPER)
                        .setDisplayName(ChatColor.BLUE + action + " Custom Amount")
                        .addLoreLines(ChatColor.GRAY + "Click to " + action.toLowerCase() + " 10 gold.")
        ) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                int amount = 10;
                performAction(p, amount);
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

                new BankGUI(p, economyHelper).open();
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
            return;
        }

        boolean success = isDeposit ? economyHelper.depositToBank(p, amount) : economyHelper.withdrawFromBank(p, amount);
        if (success) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        // Reopen main GUI to show updated values
        new BankGUI(p, economyHelper).open();
        if (window != null) window.close();
    }
}