package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Entity;

/**
 * Applies or removes a model using ModelEngine.
 * No explicit animation logic needed; we rely on default ModelEngine animations.
 */
public class ModelHandler {

    public static ModeledEntity applyModel(Entity entity, String modelId) {
        if (modelId == null || modelId.isEmpty()) return null;

        ModeledEntity me = ModelEngineAPI.getModeledEntity(entity);
        if (me == null) {
            me = ModelEngineAPI.createModeledEntity(entity);
        }
        try {
            ActiveModel am = ModelEngineAPI.createActiveModel(modelId);
            me.addModel(am, true);
            me.setBaseEntityVisible(false);
            return me;
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to apply model '" + modelId + "' to entity: " + entity.getUniqueId());
            return null;
        }
    }

    public static void removeModel(Entity entity, String modelId) {
        ModeledEntity me = ModelEngineAPI.getModeledEntity(entity);
        if (me != null) {
            me.removeModel(modelId);
            me.setBaseEntityVisible(true);
        }
    }
}
