// File: eu/xaru/mysticrpg/dungeons/doors/DoorManager.java

package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DoorManager {
    private final JavaPlugin plugin;
    private final Map<String, Door> doors = new HashMap<>();
    private final Map<String, BukkitTask> doorParticleTasks = new HashMap<>();

    /**
     * Constructor for DoorManager.
     *
     * @param plugin The main JavaPlugin instance.
     */
    public DoorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a new door to the manager.
     *
     * @param door The Door instance to add.
     * @return True if the door was added successfully, false if a door with the same ID already exists.
     */
    public boolean addDoor(Door door) {
        if (doors.containsKey(door.getDoorId())) {
            return false; // Door ID already exists
        }
        doors.put(door.getDoorId(), door);
        return true;
    }

    /**
     * Retrieves a door by its ID.
     *
     * @param doorId The unique identifier of the door.
     * @return The Door instance if found, null otherwise.
     */
    public Door getDoor(String doorId) {
        return doors.get(doorId);
    }

    /**
     * Removes a door by its ID.
     *
     * @param doorId The unique identifier of the door to remove.
     * @return True if the door was removed successfully, false if no such door exists.
     */
    public boolean removeDoor(String doorId) {
        if (!doors.containsKey(doorId)) {
            return false;
        }
        doors.remove(doorId);
        // Also remove any associated particle tasks
        if (doorParticleTasks.containsKey(doorId)) {
            doorParticleTasks.get(doorId).cancel();
            doorParticleTasks.remove(doorId);
        }
        return true;
    }

    /**
     * Builds all doors by placing barrier blocks and starting flame particles within each barrier block.
     */
    public void buildAllDoors() {
        for (Door door : doors.values()) {
            buildDoor(door);
        }
    }

    /**
     * Builds a single door by placing barrier blocks and spawning flame particles.
     *
     * @param door The Door instance to build.
     */
    private void buildDoor(Door door) {
        Location bottomLeft = door.getBottomLeft();
        Location topRight = door.getTopRight();

        World world = bottomLeft.getWorld();
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "World is null for door ID: " + door.getDoorId());
            return;
        }

        // Iterate through the defined area and place barrier blocks
        for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(blockLocation);
                    block.setType(org.bukkit.Material.BARRIER);
                }
            }
        }

        // Start spawning flame particles within each barrier block
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
                for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                    for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                        Location particleLocation = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                        world.spawnParticle(Particle.FLAME, particleLocation, 1, 0, 0, 0, 0.0);
                    }
                }
            }
        }, 0L, 20L); // Every 1 second

        doorParticleTasks.put(door.getDoorId(), particleTask);
    }

    /**
     * Closes all doors by removing barrier blocks and stopping flame particles.
     */
    public void closeAllDoors() {
        for (Door door : doors.values()) {
            closeDoor(door);
        }
    }

    /**
     * Closes a single door by removing barrier blocks and stopping flame particles.
     *
     * @param door The Door instance to close.
     */
    private void closeDoor(Door door) {
        Location bottomLeft = door.getBottomLeft();
        Location topRight = door.getTopRight();

        World world = bottomLeft.getWorld();
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "World is null for door ID: " + door.getDoorId());
            return;
        }

        // Iterate through the defined area and remove barrier blocks
        for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(blockLocation);
                    block.setType(org.bukkit.Material.AIR);
                }
            }
        }

        // Stop spawning flame particles
        if (doorParticleTasks.containsKey(door.getDoorId())) {
            doorParticleTasks.get(door.getDoorId()).cancel();
            doorParticleTasks.remove(door.getDoorId());
        }
    }
}
