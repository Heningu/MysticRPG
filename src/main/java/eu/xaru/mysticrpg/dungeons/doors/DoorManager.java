package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DoorManager {
    private final JavaPlugin plugin;
    private final Map<String, Door> doors = new HashMap<>();
    private final Map<String, BukkitTask> doorParticleTasks = new HashMap<>();

    public DoorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean addDoor(Door door) {
        if (doors.containsKey(door.getDoorId())) {
            return false; // Door ID already exists
        }
        doors.put(door.getDoorId(), door);
        return true;
    }

    public Door getDoor(String doorId) {
        return doors.get(doorId);
    }

    public boolean removeDoor(String doorId) {
        if (!doors.containsKey(doorId)) {
            return false;
        }
        doors.remove(doorId);
        if (doorParticleTasks.containsKey(doorId)) {
            doorParticleTasks.get(doorId).cancel();
            doorParticleTasks.remove(doorId);
        }
        return true;
    }

    public void setDoorTrigger(String doorId, String triggerType) {
        Door door = doors.get(doorId);
        if (door != null) {
            door.setTriggerType(triggerType);
        }
    }

    public void buildAllDoors() {
        for (Door door : doors.values()) {
            buildDoor(door);
        }
    }

    public void buildDoor(Door door) {
        Location bottomLeft = door.getBottomLeft();
        Location topRight = door.getTopRight();

        World world = bottomLeft.getWorld();
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "World is null for door ID: " + door.getDoorId());
            return;
        }

        for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(blockLocation);
                    block.setType(Material.BARRIER);
                }
            }
        }

        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
                for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                    for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                        Location particleLocation = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                        world.spawnParticle(Particle.FLAME, particleLocation, 1, 0, 0, 0, 0.0);
                    }
                }
            }
        }, 0L, 20L);

        doorParticleTasks.put(door.getDoorId(), particleTask);
    }

    public void closeAllDoors() {
        for (Door door : doors.values()) {
            closeDoor(door);
        }
    }

    private void closeDoor(Door door) {
        Location bottomLeft = door.getBottomLeft();
        Location topRight = door.getTopRight();

        World world = bottomLeft.getWorld();
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "World is null for door ID: " + door.getDoorId());
            return;
        }

        for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(blockLocation);
                    block.setType(Material.AIR);
                }
            }
        }

        if (doorParticleTasks.containsKey(door.getDoorId())) {
            doorParticleTasks.get(door.getDoorId()).cancel();
            doorParticleTasks.remove(door.getDoorId());
        }
    }

    public void finalizeDoors() {
        for (Door door : doors.values()) {
            finalizeDoor(door);
        }
    }

    private void finalizeDoor(Door door) {
        if (doorParticleTasks.containsKey(door.getDoorId())) {
            doorParticleTasks.get(door.getDoorId()).cancel();
            doorParticleTasks.remove(door.getDoorId());
        }

        Location bottomLeft = door.getBottomLeft();
        Location topRight = door.getTopRight();

        World world = bottomLeft.getWorld();
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "World is null for door ID: " + door.getDoorId());
            return;
        }

        // Replace barrier with bedrock
        for (double x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (double y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                for (double z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(blockLocation);
                    if (block.getType() == Material.BARRIER) {
                        block.setType(Material.BEDROCK);
                    }
                }
            }
        }
    }
}
