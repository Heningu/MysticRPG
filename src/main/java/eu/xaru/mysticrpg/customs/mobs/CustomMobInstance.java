package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

public class CustomMobInstance {

    private final CustomMob customMob;
    private final Location spawnLocation;
    private final LivingEntity entity;
    private final ModeledEntity modeledEntity;
    private UUID lastDamager;
    private Entity target;
    private boolean isPerformingAction = false;
    private boolean inCombat = false;
    private String currentAnimation;
    private double currentHp;
    private MobBossBarHandler bossBarHandler;
    private List<String> aiTargetSelectors;



    public CustomMobInstance(CustomMob customMob, Location spawnLocation, LivingEntity entity, ModeledEntity modeledEntity) {
        this.customMob = customMob;
        this.spawnLocation = spawnLocation;
        this.entity = entity;
        this.modeledEntity = modeledEntity;
        this.currentHp = customMob.getHealth();

        if (customMob.getBossBarConfig() != null && customMob.getBossBarConfig().isEnabled()) {
            this.bossBarHandler = new MobBossBarHandler(this);
        }
    }

    public MobBossBarHandler getBossBarHandler() {
        return bossBarHandler;
    }


    public CustomMob getCustomMob() {
        return customMob;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public ModeledEntity getModeledEntity() {
        return modeledEntity;
    }

    public UUID getLastDamager() {
        return lastDamager;
    }

    public void setLastDamager(UUID lastDamager) {
        this.lastDamager = lastDamager;
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
        Bukkit.getLogger().info("Mob " + customMob.getName() + " set target to " + (target != null ? target.getName() : "null"));
    }

    public boolean isPerformingAction() {
        return isPerformingAction;
    }

    public void setPerformingAction(boolean isPerformingAction) {
        this.isPerformingAction = isPerformingAction;
    }

    public boolean isInCombat() {
        return inCombat;
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(String currentAnimation) {
        this.currentAnimation = currentAnimation;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(double currentHp) {
        this.currentHp = currentHp;
    }
    // Add the getter method
    public List<String> getAiTargetSelectors() {
        return aiTargetSelectors;
    }

    // Add the setter method
    public void setAiTargetSelectors(List<String> aiTargetSelectors) {
        this.aiTargetSelectors = aiTargetSelectors;
    }

}
