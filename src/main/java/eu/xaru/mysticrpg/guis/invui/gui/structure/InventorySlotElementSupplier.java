package eu.xaru.mysticrpg.guis.invui.gui.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import eu.xaru.mysticrpg.guis.invui.gui.SlotElement;
import eu.xaru.mysticrpg.guis.invui.gui.SlotElement.InventorySlotElement;
import eu.xaru.mysticrpg.guis.invui.inventory.Inventory;
import eu.xaru.mysticrpg.guis.invui.item.ItemProvider;

import java.util.function.Supplier;

public class InventorySlotElementSupplier implements Supplier<InventorySlotElement> {
    
    private final Inventory inventory;
    private final ItemProvider background;
    private int slot = -1;
    
    public InventorySlotElementSupplier(@NotNull Inventory inventory) {
        this.inventory = inventory;
        this.background = null;
    }
    
    public InventorySlotElementSupplier(@NotNull Inventory inventory, @Nullable ItemProvider background) {
        this.inventory = inventory;
        this.background = background;
    }
    
    @NotNull
    @Override
    public SlotElement.InventorySlotElement get() {
        if (++slot == inventory.getSize()) slot = 0;
        return new InventorySlotElement(inventory, slot, background);
    }
    
}
