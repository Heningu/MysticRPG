package eu.xaru.mysticrpg.entityhandling;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a combined NPC-like entity that uses two ArmorStands:
 * 1) A bottom ArmorStand (invisible) that holds the ModelEngine model (if any).
 * 2) A top ArmorStand (invisible) that holds the NPC's display name as a nametag.
 */
public class LinkedEntity {

    private final String entityId;
    private final boolean persistent;
    private Location spawnLocation;

    private String displayName;
    private String modelId; // e.g. "miner" or "" if no model

    private ArmorStand modelStand;
    private ArmorStand nameStand;

    public LinkedEntity(String entityId, Location spawnLocation,
                        String displayName, String modelId, boolean persistent) {
        this.entityId = entityId;
        this.spawnLocation = spawnLocation;
        this.displayName = displayName;
        this.modelId = modelId;
        this.persistent = persistent;
    }

    public void spawnEntities() {
        JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().info(
                "[LinkedEntity] spawnEntities() called for entityId='" + entityId
                        + "' at location: " + (spawnLocation != null ? spawnLocation.toString() : "null")
                        + " with modelId='" + modelId + "'"
        );

        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().warning(
                    "[LinkedEntity] spawnEntities() aborted: spawnLocation/world is null for entityId='" + entityId + "'"
            );
            return;
        }
        despawnEntities();

        // Bottom stand (model)
        modelStand = (ArmorStand) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ARMOR_STAND);
        modelStand.setInvisible(true);
        modelStand.setCustomNameVisible(false);
        modelStand.setGravity(false);
        modelStand.setInvulnerable(true);
        modelStand.addScoreboardTag("XaruLinkedEntity_" + entityId + "_model");

        // Top stand (name) => slightly above
        Location nameLoc = spawnLocation.clone().add(0, 0.3, 0);
        nameStand = (ArmorStand) spawnLocation.getWorld().spawnEntity(nameLoc, EntityType.ARMOR_STAND);
        nameStand.setInvisible(true);
        nameStand.setCustomNameVisible(true);
        nameStand.setGravity(false);
        nameStand.setInvulnerable(true);
        nameStand.addScoreboardTag("XaruLinkedEntity_" + entityId + "_name");

        setDisplayName(displayName);
        attachModel(modelId);
    }

    private void attachModel(String newModelId) {
        if (newModelId == null || newModelId.isEmpty()) {
            JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().info(
                    "[LinkedEntity] attachModel() - No modelId provided for entityId='" + entityId + "'"
            );
            return;
        }
        if (modelStand == null || modelStand.isDead()) {
            JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().warning(
                    "[LinkedEntity] attachModel() aborted: modelStand is null or dead for entityId='" + entityId + "'"
            );
            return;
        }

        JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().info(
                "[LinkedEntity] Attaching modelId='" + newModelId + "' to entityId='" + entityId + "'"
        );

        try {
            ModeledEntity me = ModelEngineAPI.createModeledEntity(modelStand);
            if (me == null) {
                JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().warning(
                        "[LinkedEntity] ModeledEntity is null for entityId='" + entityId
                                + "' with modelId='" + newModelId + "'."
                );
                return;
            }

            ActiveModel am = ModelEngineAPI.createActiveModel(newModelId);
            if (am == null) {
                JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().warning(
                        "[LinkedEntity] ActiveModel is null for modelId='" + newModelId + "'."
                );
                return;
            }

            me.addModel(am, true);
            me.setBaseEntityVisible(false);

            JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().info(
                    "[LinkedEntity] Successfully attached modelId='" + newModelId
                            + "' to entityId='" + entityId + "'."
            );

        } catch (Exception e) {
            JavaPlugin.getProvidingPlugin(LinkedEntity.class).getLogger().severe(
                    "Failed to attach ModelEngine model '" + newModelId
                            + "' to LinkedEntity '" + entityId + "'!"
            );
            e.printStackTrace();
        }
    }

    public void setModelId(String newModelId) {
        this.modelId = newModelId;
        despawnEntities();
        spawnEntities();
    }

    public void despawnEntities() {
        if (modelStand != null && !modelStand.isDead()) {
            modelStand.remove();
        }
        if (nameStand != null && !nameStand.isDead()) {
            nameStand.remove();
        }
        modelStand = null;
        nameStand = null;
    }

    public void setDisplayName(String newName) {
        this.displayName = newName;
        if (nameStand != null && !nameStand.isDead()) {
            nameStand.setCustomName(ChatColor.translateAlternateColorCodes('&',
                    newName != null ? newName : ""));
        }
    }

    /**
     * Rotate stands around the yaw axis. (We ignore pitch here for simplicity.)
     */
    public void setYaw(float yaw) {
        if (modelStand != null && !modelStand.isDead()) {
            modelStand.setRotation(yaw, modelStand.getLocation().getPitch());
        }
        if (nameStand != null && !nameStand.isDead()) {
            nameStand.setRotation(yaw, nameStand.getLocation().getPitch());
        }
    }

    /**
     * A simple "look at" method ignoring pitch. We just compute yaw difference.
     */
    public void lookAt(Location target) {
        if (target == null || spawnLocation == null) {
            return;
        }
        double dx = target.getX() - spawnLocation.getX();
        double dz = target.getZ() - spawnLocation.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        setYaw(yaw);
    }

    public void interact(org.bukkit.entity.Player player) {
        player.sendMessage(ChatColor.YELLOW + "You interacted with " + entityId + "!");
    }

    // Getters
    public String getEntityId()        { return entityId; }
    public boolean isPersistent()      { return persistent; }
    public Location getSpawnLocation() { return spawnLocation; }
    public String getModelId()         { return modelId; }
    public String getDisplayName()     { return displayName; }
}
