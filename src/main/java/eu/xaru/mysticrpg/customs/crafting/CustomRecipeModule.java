package eu.xaru.mysticrpg.customs.crafting;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;

import java.util.List;
import java.util.logging.Level;

public class CustomRecipeModule implements IBaseModule {

    private RecipeManager recipeManager;
    private DebugLoggerModule logger;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        logger.initialize();

        recipeManager = new RecipeManager(logger);
        logger.log(Level.INFO, "CustomRecipeModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "CustomRecipeModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "CustomRecipeModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "CustomRecipeModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);  // Example dependency
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    //public boolean checkRecipeUnlocked(PlayerData playerData, String recipeID) {
    ////    return recipeManager.isRecipeUnlocked(playerData, recipeID);
    //}

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
