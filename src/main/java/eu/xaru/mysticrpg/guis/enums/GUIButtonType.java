// File: eu/xaru/mysticrpg/gui/enums/GUIButtonType.java
package eu.xaru.mysticrpg.guis.enums;

import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

/**
 * Enum representing standard GUI button types with predefined ItemStacks.
 */
public enum GUIButtonType {
    CLOSE(Material.BARRIER, "&cClose", List.of("&7Click to close the GUI")),
    NEXT_PAGE(Material.ARROW, "&aNext Page", List.of("&7Click to go to the next page")),
    PREVIOUS_PAGE(Material.ARROW, "&aPrevious Page", List.of("&7Click to go to the previous page")),
    CONFIRM(Material.GREEN_DYE, "&aConfirm", List.of("&7Click to confirm your action")),
    CANCEL(Material.RED_DYE, "&cCancel", List.of("&7Click to cancel your action")),
    BACK(Material.COMPASS, "&eBack", List.of("&7Click to go back to the previous menu")),
    REFRESH(Material.REDSTONE, "&cRefresh", List.of("&7Click to refresh the GUI")),
    SETTINGS(Material.CLOCK, "&6Settings", List.of("&7Click to modify settings"));

    private final ItemStack itemStack;

    /**
     * Constructs a GUIButtonType with the specified properties.
     *
     * @param material    The material of the button.
     * @param displayName The display name of the button.
     * @param lore        The lore of the button.
     */
    GUIButtonType(Material material, String displayName, List<String> lore) {
        this.itemStack = createButtonItem(material, displayName, lore);
    }

    /**
     * Retrieves the ItemStack representation of the button.
     *
     * @return The ItemStack associated with the button type.
     */
    public ItemStack getItem() {
        return itemStack.clone(); // Clone to prevent unintended modifications
    }

    /**
     * Creates an ItemStack for the button with the specified properties.
     *
     * @param material    The material of the button.
     * @param displayName The display name of the button.
     * @param lore        The lore of the button.
     * @return The constructed ItemStack.
     */
    private ItemStack createButtonItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.getInstance().$(displayName));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(Collections.singletonList(Utils.getInstance().$(String.valueOf(lore))));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
