package eu.xaru.mysticrpg.guis.economy;

import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Main Bank GUI: Shows player's held and bank gold, and options to deposit or withdraw.
 */
public class BankGUI {

    private final Player player;
    private final EconomyHelper economyHelper;
    private Window window;

    public BankGUI(Player player, EconomyHelper economyHelper) {
        this.player = player;
        this.economyHelper = economyHelper;
    }

    public void open() {
        // Structure: 3x9
        // #########
        // #I   D W#
        // #########
        // I = Info, D = Deposit, W = Withdraw, # = border, spaces = empty
        Structure structure = new Structure(
                "#########",
                "#I   D W#",
                "#########"
        );

        // Add ingredients
        // Border (#)
        structure.addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")));

        // Info (I)
        structure.addIngredient('I', new SimpleItem(
                new ItemBuilder(Material.CHEST)
                        .setDisplayName(ChatColor.GOLD + "Your Bank")
                        .addLoreLines(
                                ChatColor.GRAY + "Held Gold: " + economyHelper.getHeldGold(player),
                                ChatColor.GRAY + "Bank Gold: " + economyHelper.getBankGold(player)
                        ))
        );

        // Deposit (D)
        structure.addIngredient('D', new SimpleItem(
                new ItemBuilder(Material.EMERALD)
                        .setDisplayName(ChatColor.GREEN + "Deposit Gold")
                        .addLoreLines(
                                ChatColor.GRAY + "Click to deposit gold from",
                                ChatColor.GRAY + "your held gold into the bank."
                        )) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                new BankSubGUI(p, economyHelper, true).open();
                if (window != null) window.close();
            }
        });

        // Withdraw (W)
        structure.addIngredient('W', new SimpleItem(
                new ItemBuilder(Material.GOLD_INGOT)
                        .setDisplayName(ChatColor.YELLOW + "Withdraw Gold")
                        .addLoreLines(
                                ChatColor.GRAY + "Click to withdraw gold from",
                                ChatColor.GRAY + "your bank into your held gold."
                        )) {
            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                event.setCancelled(true);
                new BankSubGUI(p, economyHelper, false).open();
                if (window != null) window.close();
            }
        });

        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);


        // Build GUI
        Gui gui = Gui.normal()
                .setStructure(structure)
                .setBackground(background)
                .build();

        window = Window.single()
                .setViewer(player)
                .setTitle("Bank")
                .setGui(gui)
                .build();
        window.open();
    }
}