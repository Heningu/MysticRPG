// File: eu/xaru/mysticrpg/gui/components/Button.java
package eu.xaru.mysticrpg.guis.components;

import eu.xaru.mysticrpg.guis.actions.GUIAction;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a clickable button within a GUI.
 */
public class Button {
    private final ItemStack item;
    private final GUIAction action;

    /**
     * Constructs a Button with the specified item and action.
     *
     * @param item   The ItemStack representing the button's appearance.
     * @param action The action to execute upon clicking the button.
     */
    public Button(ItemStack item, GUIAction action) {
        this.item = item;
        this.action = action;
    }

    public ItemStack getItem() {
        return item;
    }

    public GUIAction getAction() {
        return action;
    }
}
