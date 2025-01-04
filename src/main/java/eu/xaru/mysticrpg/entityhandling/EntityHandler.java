package eu.xaru.mysticrpg.entityhandling;

import eu.xaru.mysticrpg.npc.customnpc.CustomNPC;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * A singleton manager that tracks all "LinkedEntity" objects at runtime.
 * We spawn them with spawnNPC(...), remove them with deleteNPC(...), etc.
 */
public class EntityHandler {

    private static EntityHandler instance;
    public static EntityHandler getInstance() {
        if (instance == null) {
            instance = new EntityHandler();
        }
        return instance;
    }

    private final Map<String, LinkedEntity> entities = new HashMap<>();
    private final EntityStorage entityStorage;
    private final JavaPlugin plugin;

    private EntityHandler() {
        this.plugin = JavaPlugin.getProvidingPlugin(EntityHandler.class);
        this.entityStorage = new EntityStorage(plugin);
    }

    /**
     * If you want to load from entities.yml, you can call this. But by default,
     * we do NOT rely on it on startup (we rely on CustomNPC logic).
     */
    public void loadAndSpawnAll() {
        DebugLogger.getInstance().log(Level.INFO, "Loading saved entities from file (entities.yml)...", 0);
        List<LinkedEntity> loadedEntities = entityStorage.loadAll();

        for (LinkedEntity le : loadedEntities) {
            if (!entities.containsKey(le.getEntityId())) {
                entities.put(le.getEntityId(), le);
                le.spawnEntities();
                DebugLogger.getInstance().log(Level.INFO,
                        "Spawned saved entity: " + le.getEntityId(), 0);
            } else {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Entity ID clash: " + le.getEntityId() + " is already in memory!", 0);
            }
        }
    }

    /**
     * Removes all stands from the world, saves to disk, and clears memory.
     */
    public void shutdownCleanup() {
        DebugLogger.getInstance().log(Level.INFO, "Removing all managed entities...", 0);
        for (LinkedEntity le : entities.values()) {
            le.despawnEntities();
        }
        entityStorage.saveAll(new ArrayList<>(entities.values()));
        entities.clear();

        DebugLogger.getInstance().log(Level.INFO,
                "All managed entities removed. Ephemeral ones discarded.", 0);
    }

    /**
     * Spawn an NPC by creating a LinkedEntity. "NPC_" + npc.getId() is used as entity ID.
     */
    public void spawnNPC(CustomNPC npc, boolean markAsSaved) {
        String entityId = "NPC_" + npc.getId();
        // If it's already in memory, remove old stands first
        if (entities.containsKey(entityId)) {
            LinkedEntity existing = entities.get(entityId);
            existing.despawnEntities();
            entities.remove(entityId);
        }

        DebugLogger.getInstance().log(Level.INFO,
                "[EntityHandler] Spawning NPC ID='" + npc.getId()
                        + "' with entityId='" + entityId + "', model='" + npc.getModelId() + "'", 0);

        LinkedEntity linkedEntity = new LinkedEntity(
                entityId,
                npc.getLocation(),
                npc.getName(),
                npc.getModelId(),
                markAsSaved
        );
        linkedEntity.spawnEntities();

        entities.put(entityId, linkedEntity);

        if (markAsSaved) {
            entityStorage.saveAll(new ArrayList<>(entities.values()));
        }
    }

    /**
     * Delete the stands for an NPC from memory/world.
     */
    public void deleteNPC(CustomNPC npc) {
        String entityId = "NPC_" + npc.getId();
        removeEntityById(entityId);
    }

    /**
     * More generic removal if you only have entityId.
     */
    public boolean removeEntityById(String entityId) {
        LinkedEntity le = entities.get(entityId);
        if (le == null) {
            return false;
        }
        DebugLogger.getInstance().log(Level.INFO,
                "[EntityHandler] Removing entityId='" + entityId + "' from memory/world", 0);
        le.despawnEntities();
        entities.remove(entityId);
        entityStorage.saveAll(new ArrayList<>(entities.values()));
        return true;
    }

    /**
     * If you want to re-spawn with a new modelId, you can do setModelId(...).
     */
    public boolean updateEntityModel(String entityId, String newModelId) {
        LinkedEntity le = entities.get(entityId);
        if (le == null) {
            return false;
        }
        le.setModelId(newModelId);
        if (le.isPersistent()) {
            entityStorage.saveAll(new ArrayList<>(entities.values()));
        }
        return true;
    }

    // ------------------------------
    // Behavior commands
    // ------------------------------

    /**
     * Rotate the stands for the given NPC to a certain yaw (0..360).
     */
    public boolean rotateNPC(CustomNPC npc, float yaw) {
        String entityId = "NPC_" + npc.getId();
        LinkedEntity le = entities.get(entityId);
        if (le == null) {
            return false;
        }
        plugin.getLogger().info("[EntityHandler] Rotating NPC '" + npc.getId()
                + "' to yaw=" + yaw);
        le.setYaw(yaw);
        return true;
    }

    /**
     * Make the NPC face a certain location.
     */
    public boolean lookAtLocation(CustomNPC npc, Location targetLoc) {
        String entityId = "NPC_" + npc.getId();
        LinkedEntity le = entities.get(entityId);
        if (le == null || targetLoc == null) {
            return false;
        }
        plugin.getLogger().info("[EntityHandler] NPC '" + npc.getId()
                + "' is looking at location " + targetLoc);
        le.lookAt(targetLoc);
        return true;
    }

    /**
     * Debug listing of what's in memory.
     */
    public void listManagedEntities(org.bukkit.command.CommandSender sender) {
        if (entities.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No managed entities in memory.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Currently managed entities:");
        for (LinkedEntity le : entities.values()) {
            sender.sendMessage(ChatColor.AQUA + " - ID: " + le.getEntityId()
                    + " | Saved: " + le.isPersistent()
                    + " | Model: " + (le.getModelId().isEmpty() ? "none" : le.getModelId())
                    + " | Location: "
                    + (le.getSpawnLocation() != null ? le.getSpawnLocation().toString() : "null")
            );
        }
    }
}
