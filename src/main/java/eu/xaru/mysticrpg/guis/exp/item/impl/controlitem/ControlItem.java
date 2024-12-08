package eu.xaru.mysticrpg.guis.exp.item.impl.controlitem;

import eu.xaru.mysticrpg.guis.exp.gui.Gui;
import eu.xaru.mysticrpg.guis.exp.item.Item;
import eu.xaru.mysticrpg.guis.exp.item.ItemProvider;
import eu.xaru.mysticrpg.guis.exp.item.impl.AbstractItem;

/**
 * A special type of {@link Item} that stores the {@link Gui} in which it is displayed.
 *
 * @param <G> The Gui Type this {@link ControlItem} will control. Not checked when adding it to a Gui.
 */
public abstract class ControlItem<G extends Gui> extends AbstractItem {
    
    private G gui;
    
    public abstract ItemProvider getItemProvider(G gui);
    
    @Override
    public final ItemProvider getItemProvider() {
        return getItemProvider(gui);
    }
    
    public G getGui() {
        return gui;
    }
    
    public void setGui(G gui) {
        if (this.gui == null) {
            this.gui = gui;
        }
    }
    
}
