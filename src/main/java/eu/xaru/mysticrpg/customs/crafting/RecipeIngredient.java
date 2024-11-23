package eu.xaru.mysticrpg.customs.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RecipeIngredient {

    private final ItemStack itemStack;

    public RecipeIngredient(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public boolean matches(ItemStack other) {
        if (other == null || other.getType() == Material.AIR) return false;
        if (!itemStack.getType().equals(other.getType())) return false;
        // Additional checks like metadata can be added here if needed
        return true;
    }
}
