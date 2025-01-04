package eu.xaru.mysticrpg.entityhandling;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * A singleton manager that tracks all plugin-related entities at runtime.
 * We *can* load them from disk using loadAndSpawnAll(), but we've chosen not
 * to call it on startup. Instead, we spawn old NPCs via npc.spawn().
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
     * If you ever want to spawn from entities.yml, you can call this. But by default,
     * we do NOT call it on startup, letting the CustomNPC logic do the spawns.
     */
    public void loadAndSpawnAll() {
        DebugLogger.getInstance().log(Level.INFO, "Loading saved entities from file...", 0);

        List<LinkedEntity> loadedEntities = entityStorage.loadAll();
        for (LinkedEntity le : loadedEntities) {
            if (!entities.containsKey(le.getEntityId())) {
                entities.put(le.getEntityId(), le);
                le.spawnEntities(); // attach model if modelId != ""
                DebugLogger.getInstance().log(Level.INFO,
                        "Spawned saved entity: " + le.getEntityId(), 0);
            } else {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Entity ID clash: " + le.getEntityId() + " is already in memory!", 0);
            }
        }
    }

    public void shutdownCleanup() {
        DebugLogger.getInstance().log(Level.INFO, "Removing all managed entities...", 0);
        for (LinkedEntity le : entities.values()) {
            le.despawnEntities();
        }
        entityStorage.saveAll(new ArrayList<>(entities.values()));

        entities.clear();
        DebugLogger.getInstance().log(Level.INFO,
                "All managed entities removed. Ephemeral ones are discarded.", 0);
    }

    /**
     * Called by npc.spawn() for brand-new or forcibly-spawned NPCs.
     */
    public LinkedEntity createLinkedEntity(String entityId,
                                           Location location,
                                           String name,
                                           String modelId,
                                           boolean markAsSaved) {
        LinkedEntity linkedEntity = new LinkedEntity(entityId, location, name, modelId, markAsSaved);
        entities.put(entityId, linkedEntity);

        linkedEntity.spawnEntities();

        if (markAsSaved) {
            entityStorage.saveAll(new ArrayList<>(entities.values()));
        }
        return linkedEntity;
    }

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

    public boolean removeEntityById(String entityId) {
        LinkedEntity le = entities.remove(entityId);
        if (le == null) {
            return false;
        }
        le.despawnEntities();
        entityStorage.saveAll(new ArrayList<>(entities.values()));
        return true;
    }

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
                    + (le.getSpawnLocation() != null
                    ? le.getSpawnLocation().toString()
                    : "null")
            );
        }
    }
}
