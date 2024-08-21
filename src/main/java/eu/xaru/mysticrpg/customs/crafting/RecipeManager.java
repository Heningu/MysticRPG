package eu.xaru.mysticrpg.customs.crafting;

import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RecipeManager {

    private final Map<String, Recipe> recipes = new HashMap<>();
    private final Gson gson = new Gson();
    private final DebugLoggerModule logger;

    public RecipeManager(DebugLoggerModule logger) {
        this.logger = logger;
    }

    public void loadRecipes(File recipeFolder) {
        if (!recipeFolder.exists()) {
            if (!recipeFolder.mkdirs()) {
                logger.error("Failed to create recipe folder.");
                return;
            }
        }

        File[] files = recipeFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    Type recipeType = new TypeToken<Recipe>() {}.getType();
                    Recipe recipe = gson.fromJson(reader, recipeType);
                    recipes.put(recipe.getRecipeID(), recipe);
                    logger.log(Level.INFO, "Loaded recipe: " + recipe.getRecipeID(), 0);
                } catch (IOException e) {
                    logger.error("Failed to load recipe file: " + file.getName(), e, null);
                }
            }
        } else {
            logger.warn("No recipes found in the recipe folder.");
        }
    }

    public Recipe getRecipe(String recipeID) {
        return recipes.get(recipeID);
    }

    public boolean isRecipeValid(String recipeID) {
        return recipes.containsKey(recipeID);
    }

    public Map<String, Recipe> getAllRecipes() {
        return new HashMap<>(recipes);
    }
}
