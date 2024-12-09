package eu.xaru.mysticrpg.guis.invui.inventory;

import org.bukkit.inventory.ItemStack;

public interface StackSizeProvider {
    
    int getMaxStackSize(ItemStack itemStack);
    
}
