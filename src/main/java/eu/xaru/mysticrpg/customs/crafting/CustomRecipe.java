package eu.xaru.mysticrpg.customs.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRecipe {

    private final String id;
    private final List<String> shape;
    private final Map<Character, RecipeIngredient> ingredients;
    private final RecipeIngredient result;

    public CustomRecipe(String id, List<String> shape, Map<Character, RecipeIngredient> ingredients, RecipeIngredient result) {
        this.id = id;
        this.shape = shape;
        this.ingredients = ingredients;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public RecipeIngredient getResult() {
        return result;
    }

    public boolean matches(ItemStack[] matrix) {
        // Flatten the shape and compare with the matrix
        char[] pattern = new char[9];
        int index = 0;
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                pattern[index++] = c;
            }
        }

        for (int i = 0; i < 9; i++) {
            char key = pattern[i];
            RecipeIngredient expected = ingredients.get(key);
            ItemStack actual = matrix[i];

            if (expected == null || key == ' ') {
                if (actual != null && actual.getType() != Material.AIR) return false;
            } else {
                if (!expected.matches(actual)) return false;
            }
        }
        return true;
    }
}
