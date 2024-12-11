package eu.xaru.mysticrpg.guis.globalbuttons;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.TabItem;

/**
 * Custom TabItem representing an auction category.
 */
public class CategoryTabItem extends TabItem {

    private final int tabIndex;
    private final String categoryName;
    private final Material icon;

    /**
     * Constructs a CategoryTabItem.
     *
     * @param tabIndex     The index of the tab.
     * @param categoryName The display name of the auction category.
     * @param icon         The material icon representing the category.
     */
    public CategoryTabItem(int tabIndex, String categoryName, Material icon) {
        super(tabIndex);
        this.tabIndex = tabIndex;
        this.categoryName = categoryName;
        this.icon = icon;
    }

    @Override
    public ItemProvider getItemProvider(TabGui gui) {
        if (gui.getCurrentTab() == tabIndex) {
            // Selected Tab Appearance
            return new ItemBuilder(icon)
                    .setDisplayName(ChatColor.GOLD + categoryName + " (Selected)")
                    .addLoreLines(ChatColor.GRAY + "Click to view " + categoryName + " auctions.");
        } else {
            // Unselected Tab Appearance
            return new ItemBuilder(icon)
                    .setDisplayName(ChatColor.WHITE + categoryName)
                    .addLoreLines(ChatColor.GRAY + "Click to view " + categoryName + " auctions.");
        }
    }
}
