package eu.xaru.mysticrpg.guis;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles pagination controls - Left-click to go back, Right-click to go forward.
 * Currently, pagination isn't implemented, so these actions will notify the player.
 */
public class ChangePageItem extends ControlItem<TabGui> {

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        TabGui tabGui = getGui();
        Gui currentTab = tabGui.getTabs().get(tabGui.getCurrentTab());

        if (currentTab instanceof PagedGui<?> paged) {
            if(clickType == ClickType.LEFT) {
                if (paged.hasPreviousPage()) {
                    paged.goBack();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                } else {
                    player.sendMessage(ChatColor.RED + "No previous page!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                }
            } else if(clickType == ClickType.RIGHT) {
                if (paged.hasNextPage()) {
                    paged.goForward();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                } else {
                    player.sendMessage(ChatColor.RED + "No next page!");
                }
            }
            notifyWindows();
        }
    }

    @Override
    public ItemProvider getItemProvider(TabGui gui) {
        Gui currentTab = gui.getTabs().get(gui.getCurrentTab());
        List<String> lore = new ArrayList<>();
        if (currentTab instanceof PagedGui<?> paged) {

            lore.add( ChatColor.GRAY + "Current page: " + (paged.getCurrentPage() + 1) + " from " + (paged.getPageAmount()) + " pages" );
            if (paged.hasPreviousPage()) {
                lore.add( ChatColor.GRAY + "Click " + ChatColor.GOLD + "LEFT_CLICK" + ChatColor.GRAY + " to go back." );
            }
            if (paged.hasNextPage()) {
                lore.add( ChatColor.GRAY + "Click " + ChatColor.GOLD + "RIGHT_CLICK" + ChatColor.GRAY + " to go forward." );
            }

        } else {
            lore.add( ChatColor.GRAY + "No pagination available." );
        }

        return new ItemBuilder(Material.ARROW)
                .setDisplayName("Switch pages")
                .addLoreLines(
                        lore.toArray(new String[0])
                )
                .addEnchantment(Enchantment.UNBREAKING,1,true)
                .addAllItemFlags();
    }

}