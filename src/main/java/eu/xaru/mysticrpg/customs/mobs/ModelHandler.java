package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.Entity;

public class ModelHandler {

    /**
     * Applies a model to an entity using ModelEngine.
     *
     * @param entity  The entity to apply the model to.
     * @param modelId The ID of the model to apply.
     * @return The ModeledEntity if successful, null otherwise.
     */
    public static ModeledEntity applyModel(Entity entity, String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return null; // No model to apply
        }

        // Create or get the ModeledEntity from the entity
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null) {
            modeledEntity = ModelEngineAPI.createModeledEntity(entity);
        }

        try {
            // Create a new ActiveModel using the ID of the model
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);

            // Add the model to the entity
            modeledEntity.addModel(activeModel, true);

            // Hide the base entity
            modeledEntity.setBaseEntityVisible(false);

        } catch (Exception e) {
            System.err.println("Error applying model with ID '" + modelId + "': " + e.getMessage());
            return null;
        }

        return modeledEntity;
    }

    /**
     * Removes a model from an entity using ModelEngine.
     *
     * @param entity  The entity to remove the model from.
     * @param modelId The ID of the model to remove.
     */
    public static void removeModel(Entity entity, String modelId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity != null) {
            modeledEntity.removeModel(modelId);
            // Show the base entity again if needed
            modeledEntity.setBaseEntityVisible(true);
        }
    }

    /**
     * Plays an animation on the entity's model.
     *
     * @param entity        The entity whose model will play the animation.
     * @param modelId       The ID of the model.
     * @param animationName The name of the animation to play.
     * @param transitionIn  Transition time into the animation.
     * @param transitionOut Transition time out of the animation.
     * @param speed         Speed multiplier of the animation.
     * @param shouldLoop    Whether the animation should loop.
     */
    public static void playAnimation(Entity entity, String modelId, String animationName, double transitionIn, double transitionOut, double speed, boolean shouldLoop) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity != null) {
            ActiveModel activeModel = modeledEntity.getModel(modelId).orElse(null);
            if (activeModel != null) {
                stopAllAnimations(entity, modelId);
                activeModel.getAnimationHandler().playAnimation(animationName, transitionIn, transitionOut, speed, shouldLoop);
            }
        }
    }

    /**
     * Stops an animation on the entity's model.
     *
     * @param entity        The entity whose model will stop the animation.
     * @param modelId       The ID of the model.
     * @param animationName The name of the animation to stop.
     */
    public static void stopAnimation(Entity entity, String modelId, String animationName) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity != null) {
            ActiveModel activeModel = modeledEntity.getModel(modelId).orElse(null);
            if (activeModel != null) {
                activeModel.getAnimationHandler().stopAnimation(animationName);
            }
        }
    }

    /**
     * Stops all animations on the entity's model.
     *
     * @param entity  The entity whose model will stop all animations.
     * @param modelId The ID of the model.
     */
    public static void stopAllAnimations(Entity entity, String modelId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity != null) {
            ActiveModel activeModel = modeledEntity.getModel(modelId).orElse(null);
            if (activeModel != null) {
                activeModel.getAnimationHandler().forceStopAllAnimations();
            }
        }
    }
}
