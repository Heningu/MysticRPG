package eu.xaru.mysticrpg.guis.exp.inventory;

import org.bukkit.inventory.ItemStack;

public interface StackSizeProvider {
    
    int getMaxStackSize(ItemStack itemStack);
    
}
