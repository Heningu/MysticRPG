package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
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

        // Initialize AI target selectors as an empty list to avoid null checks elsewhere.
        this.aiTargetSelectors = new ArrayList<>();
    }

    // BossBar Management
    public MobBossBarHandler getBossBarHandler() {
        return bossBarHandler;
    }

    // Core Mob Properties
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
        if (lastDamager != null) {
            Bukkit.getLogger().info("Mob " + customMob.getName() + " was last damaged by player UUID: " + lastDamager);
        }
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
        if (target != null) {
            Bukkit.getLogger().info("Mob " + customMob.getName() + " set target to " + target.getName());
        } else {
            Bukkit.getLogger().info("Mob " + customMob.getName() + " lost its target.");
        }
    }

    // Combat and Action Flags
    public boolean isPerformingAction() {
        return isPerformingAction;
    }

    public void setPerformingAction(boolean isPerformingAction) {
        this.isPerformingAction = isPerformingAction;
        Bukkit.getLogger().info("Mob " + customMob.getName() + " is " + (isPerformingAction ? "now" : "no longer") + " performing an action.");
    }

    public boolean isInCombat() {
        return inCombat;
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
        Bukkit.getLogger().info("Mob " + customMob.getName() + " is " + (inCombat ? "in combat." : "out of combat."));
    }

    // Animation Management
    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(String currentAnimation) {
        if (!currentAnimation.equals(this.currentAnimation)) {
            this.currentAnimation = currentAnimation;
            Bukkit.getLogger().info("Mob " + customMob.getName() + " switched animation to: " + currentAnimation);
        }
    }

    // Health Management
    public double getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(double currentHp) {
        this.currentHp = currentHp;
        Bukkit.getLogger().info("Mob " + customMob.getName() + " current HP: " + currentHp + "/" + customMob.getHealth());
    }

    // AI Target Selectors
    public List<String> getAiTargetSelectors() {
        return aiTargetSelectors;
    }

    public void setAiTargetSelectors(List<String> aiTargetSelectors) {
        if (aiTargetSelectors == null) {
            Bukkit.getLogger().warning("Attempted to set null AI Target Selectors for " + customMob.getName());
            this.aiTargetSelectors = new ArrayList<>();
        } else {
            this.aiTargetSelectors = aiTargetSelectors;
            Bukkit.getLogger().info("AI Target Selectors updated for " + customMob.getName() + ": " + aiTargetSelectors);
        }
    }
}
