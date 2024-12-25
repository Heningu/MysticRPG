package eu.xaru.mysticrpg.dungeons.doors;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * DoorManager handles door data in memory for setup and for dungeon usage.
 */
public class DoorManager {

    private final JavaPlugin plugin;
    private final Map<String, Door> doors = new HashMap<>();
    private final Map<String, BukkitTask> particleTasks = new HashMap<>();

    public DoorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a door to the in-memory map.
     */
    public boolean addDoor(Door door) {
        if (doors.containsKey(door.getDoorId())) {
            return false;
        }
        doors.put(door.getDoorId(), door);
        return true;
    }

    /**
     * Returns the in-memory Door by ID.
     */
    public Door getDoor(String doorId) {
        return doors.get(doorId);
    }

    /**
     * Removes the door from memory (and stops any particles).
     */
    public boolean removeDoor(String doorId) {
        if (!doors.containsKey(doorId)) {
            return false;
        }
        doors.remove(doorId);
        if (particleTasks.containsKey(doorId)) {
            particleTasks.get(doorId).cancel();
            particleTasks.remove(doorId);
        }
        return true;
    }

    /**
     * Just sets the trigger on the Door object in memory.
     * Does NOT persist to config automatically.
     */
    public void setDoorTrigger(String doorId, String trigger) {
        Door door = doors.get(doorId);
        if (door != null) {
            door.setTriggerType(trigger);
        }
    }

    /**
     * (New) Sets the door trigger in memory AND updates the relevant
     * DoorData in the config, then saves the config, guaranteeing
     * that the trigger persists across server restarts.
     *
     * @param doorId         The ID of the door to update
     * @param trigger        The new trigger type (e.g. "leftclick", "doorkey", etc.)
     * @param keyItemId      The custom item ID required if trigger = doorkey
     * @param dungeonConfig  The config object that holds the door definitions
     * @param configManager  The manager that can save dungeon configs
     */
    public void setDoorTriggerAndSave(
            String doorId,
            String trigger,
            String keyItemId,
            DungeonConfig dungeonConfig,
            DungeonConfigManager configManager
    ) {
        Door door = doors.get(doorId);
        if (door != null) {
            door.setTriggerType(trigger);

            // If the trigger is doorkey, set the door's required key ID:
            if ("doorkey".equalsIgnoreCase(trigger)) {
                door.setRequiredKeyItemId(keyItemId);
            } else {
                // If some other trigger, clear any existing key requirement
                door.setRequiredKeyItemId(null);
            }

            // Update the matching DoorData in the config
            for (DungeonConfig.DoorData dd : dungeonConfig.getDoors()) {
                if (dd.getDoorId().equals(doorId)) {
                    dd.setTriggerType(trigger);
                    // Also persist the key ID to config
                    if ("doorkey".equalsIgnoreCase(trigger)) {
                        dd.setKeyItemId(keyItemId);
                    } else {
                        dd.setKeyItemId(null);
                    }
                    break;
                }
            }
            // Finally, save the config so the trigger is persisted
            configManager.saveDungeonConfig(dungeonConfig);
        }
    }

    /**
     * Returns a collection of all in-memory doors.
     */
    public Collection<Door> getAllDoors() {
        return doors.values();
    }

    // Other methods below are unchanged...
    // buildAllDoors(), buildDoor(), closeAllDoors(),
    // placeDoorAsStone(), finalizeDoors()

    public void buildAllDoors() {
        for (Door door : doors.values()) {
            buildDoor(door);
        }
    }

    public void buildDoor(Door door) {
        World w = door.getBottomLeft().getWorld();
        if (w == null) {
            plugin.getLogger().log(Level.WARNING,
                    "Cannot build door " + door.getDoorId() + ": world is null.");
            return;
        }

        int minX = door.getBottomLeft().getBlockX();
        int minY = door.getBottomLeft().getBlockY();
        int minZ = door.getBottomLeft().getBlockZ();
        int maxX = door.getTopRight().getBlockX();
        int maxY = door.getTopRight().getBlockY();
        int maxZ = door.getTopRight().getBlockZ();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location particleLoc = new Location(w, x + 0.5, y + 0.5, z + 0.5);
                        w.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.0);
                    }
                }
            }
        }, 0L, 20L);

        particleTasks.put(door.getDoorId(), task);
    }

    public void closeAllDoors() {
        for (Door d : doors.values()) {
            closeDoor(d);
        }
    }

    private void closeDoor(Door door) {
        if (particleTasks.containsKey(door.getDoorId())) {
            particleTasks.get(door.getDoorId()).cancel();
            particleTasks.remove(door.getDoorId());
        }
    }

    public void placeDoorAsStone(Door door) {
        World w = door.getBottomLeft().getWorld();
        if (w == null) {
            plugin.getLogger().log(Level.WARNING,
                    "World is null for door " + door.getDoorId());
            return;
        }

        int minX = door.getBottomLeft().getBlockX();
        int minY = door.getBottomLeft().getBlockY();
        int minZ = door.getBottomLeft().getBlockZ();
        int maxX = door.getTopRight().getBlockX();
        int maxY = door.getTopRight().getBlockY();
        int maxZ = door.getTopRight().getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    w.getBlockAt(x, y, z).setType(Material.STONE);
                }
            }
        }
    }

    public void finalizeDoors() {
        for (Door d : doors.values()) {
            if (particleTasks.containsKey(d.getDoorId())) {
                particleTasks.get(d.getDoorId()).cancel();
                particleTasks.remove(d.getDoorId());
            }
        }
    }
}
