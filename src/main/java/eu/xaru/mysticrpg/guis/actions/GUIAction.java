// File: eu/xaru/mysticrpg/gui/actions/GUIAction.java
package eu.xaru.mysticrpg.guis.actions;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Functional interface representing an action to be performed when a GUI slot is clicked.
 */
@FunctionalInterface
public interface GUIAction {
    /**
     * Executes the action.
     *
     * @param event The InventoryClickEvent triggered by the click.
     */
    void execute(InventoryClickEvent event);
}
