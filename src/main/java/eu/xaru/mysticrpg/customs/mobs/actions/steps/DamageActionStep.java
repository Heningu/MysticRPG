package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.CustomDamageHandler;
import org.bukkit.Bukkit;
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
                Bukkit.getLogger().warning("Attempted to damage self. Skipping damage.");
                return;
            }
            if (livingTarget instanceof Player playerTarget) {
                // Apply custom damage to the player
                CustomDamageHandler customDamageHandler = ModuleManager.getInstance().getModuleInstance(CustomDamageHandler.class);
                if (customDamageHandler != null) {
                    customDamageHandler.applyCustomDamage(playerTarget, damageAmount);
                } else {
                    Bukkit.getLogger().warning("CustomDamageHandler is not loaded. Cannot apply custom damage.");
                }
            } else {
                // For non-player entities, apply default damage
                livingTarget.damage(damageAmount, mobInstance.getEntity());
            }
        } else {
            Bukkit.getLogger().warning("No valid target to apply damage. Target is " + (target != null ? target.getName() : "null"));
        }
    }
}
