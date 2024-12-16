package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.CustomDamageHandler;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DamageActionStep implements ActionStep {

    private final double damageAmount;

    public DamageActionStep(double damageAmount) {
        this.damageAmount = damageAmount;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
        Entity target = mobInstance.getTarget();
        if (target instanceof LivingEntity livingTarget) {
            if (livingTarget.equals(mobInstance.getEntity())) {
                DebugLogger.getInstance().warning("Attempted to damage self. Skipping damage.");
                return;
            }
            if (livingTarget instanceof Player playerTarget) {
                // Apply custom damage to the player using our new CustomDamageHandler
                CustomDamageHandler customDamageHandler = ModuleManager.getInstance().getModuleInstance(CustomDamageHandler.class);
                if (customDamageHandler != null) {
                    customDamageHandler.applyCustomDamage(playerTarget, damageAmount);
                } else {
                    DebugLogger.getInstance().warning("CustomDamageHandler is not loaded. Cannot apply custom damage.");
                }
            } else {
                // For non-player entities, apply default vanilla damage
                livingTarget.damage(damageAmount, mobInstance.getEntity());
            }
        } else {
            DebugLogger.getInstance().warning("No valid target to apply damage. Target is " + (target != null ? target.getName() : "null"));
        }
    }
}
