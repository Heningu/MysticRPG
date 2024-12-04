package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class CustomMobInstance {

    private final CustomMob customMob;
    private final Location spawnLocation;
    private final String assignedArea;
    private final UUID mobUUID;
    private final LivingEntity entity;
    private UUID lastDamager;

    // Custom HP tracking
    private double currentHp;

    // New field to store the modeled entity
    private final ModeledEntity modeledEntity;

    public CustomMobInstance(CustomMob customMob, Location spawnLocation, LivingEntity entity, ModeledEntity modeledEntity) {
        this.customMob = customMob;
        this.spawnLocation = spawnLocation;
        this.entity = entity;
        this.mobUUID = entity.getUniqueId();
        this.assignedArea = null;  // Assigned area is optional
        this.currentHp = customMob.getHealth();
        this.modeledEntity = modeledEntity;
    }

    // Getters and setters
    public CustomMob getCustomMob() {
        return customMob;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public String getAssignedArea() {
        return assignedArea;
    }

    public UUID getMobUUID() {
        return mobUUID;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(double currentHp) {
        this.currentHp = currentHp;
    }

    public UUID getLastDamager() {
        return lastDamager;
    }

    public void setLastDamager(UUID lastDamager) {
        this.lastDamager = lastDamager;
    }

    public ModeledEntity getModeledEntity() {
        return modeledEntity;
    }
}
