package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.ModelHandler;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;

public class AnimationActionStep implements ActionStep {

    private final String animationName;

    public AnimationActionStep(String animationName) {
        this.animationName = animationName;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
        String modelId = mobInstance.getCustomMob().getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            DebugLogger.getInstance().log("Playing animation '" + animationName + "' for model '" + modelId + "'");
            ModelHandler.playAnimation(mobInstance.getEntity(), modelId, animationName, 0.0, 0.0, 1.0, false);
        }
    }
}