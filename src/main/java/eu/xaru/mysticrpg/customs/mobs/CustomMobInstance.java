package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class CustomMobInstance {

    private final CustomMob customMob;
    private final Location spawnLocation;
    private final String assignedArea;
    private final UUID mobUUID;
    private final LivingEntity entity;

    // Custom HP tracking
    private double currentHp;

    public CustomMobInstance(CustomMob customMob, Location spawnLocation, LivingEntity entity) {
        this.customMob = customMob;
        this.spawnLocation = spawnLocation;
        this.entity = entity;
        this.mobUUID = entity.getUniqueId();
        this.assignedArea = null;  // Assigned area is optional

        // Initialize current HP to max HP
        this.currentHp = customMob.getHealth();
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
}
