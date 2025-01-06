package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Represents a live, spawned custom mob in the world.
 * No 'actions' or custom animations. We just handle HP, boss bar, etc.
 */
public class CustomMobInstance {

    private final CustomMob customMob;
    private final Location spawnLocation;
    private final LivingEntity entity;
    private final ModeledEntity modeledEntity;

    private UUID lastDamager;
    private Entity target;
    private boolean inCombat;
    private double currentHp;

    // Optional boss bar
    private MobBossBarHandler bossBarHandler;

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

    public CustomMob getCustomMob()        { return customMob; }
    public Location getSpawnLocation()     { return spawnLocation; }
    public LivingEntity getEntity()        { return entity; }
    public ModeledEntity getModeledEntity() { return modeledEntity; }

    public UUID getLastDamager()           { return lastDamager; }
    public void setLastDamager(UUID damager) {
        this.lastDamager = damager;
    }

    public Entity getTarget()              { return target; }
    public void setTarget(Entity tgt) {
        this.target = tgt;
        DebugLogger.getInstance().log("Mob " + customMob.getName()
                + " set target to " + (tgt != null ? tgt.getName() : "null"));
    }

    public boolean isInCombat() {
        return inCombat;
    }
    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public double getCurrentHp() {
        return currentHp;
    }
    public void setCurrentHp(double hp) {
        this.currentHp = hp;
    }

    public MobBossBarHandler getBossBarHandler() {
        return bossBarHandler;
    }
}
