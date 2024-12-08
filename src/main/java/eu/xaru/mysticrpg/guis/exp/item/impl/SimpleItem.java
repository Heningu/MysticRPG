package eu.xaru.mysticrpg.guis.exp.item.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import eu.xaru.mysticrpg.guis.exp.item.Click;
import eu.xaru.mysticrpg.guis.exp.item.Item;
import eu.xaru.mysticrpg.guis.exp.item.ItemProvider;
import eu.xaru.mysticrpg.guis.exp.item.ItemWrapper;

import java.util.function.Consumer;

/**
 * A simple {@link Item} that does nothing.
 */
public class SimpleItem extends AbstractItem {
    
    private final ItemProvider itemProvider;
    private final Consumer<Click> clickHandler;
    
    public SimpleItem(@NotNull ItemProvider itemProvider) {
        this.itemProvider = itemProvider;
        this.clickHandler = null;
    }
    
    public SimpleItem(@NotNull ItemStack itemStack) {
        this.itemProvider = new ItemWrapper(itemStack);
        this.clickHandler = null;
    }
    
    public SimpleItem(@NotNull ItemProvider itemProvider, @Nullable Consumer<@NotNull Click> clickHandler) {
        this.itemProvider = itemProvider;
        this.clickHandler = clickHandler;
    }
    
    public SimpleItem(@NotNull ItemStack itemStack, @Nullable Consumer<@NotNull Click> clickHandler) {
        this.itemProvider = new ItemWrapper(itemStack);
        this.clickHandler = clickHandler;
    }
    
    public ItemProvider getItemProvider() {
        return itemProvider;
    }
    
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickHandler != null) clickHandler.accept(new Click(event));
    }
    
}
