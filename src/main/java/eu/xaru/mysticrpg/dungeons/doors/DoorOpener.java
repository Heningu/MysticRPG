package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A small utility class that handles the actual removal of a door's blocks.
 */
public class DoorOpener {

    private final JavaPlugin plugin;

    public DoorOpener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void openDoor(Door door) {
        // Schedule on the next tick to avoid event conflicts
        Bukkit.getScheduler().runTask(plugin, () -> removeDoorBlocks(door));
    }

    private void removeDoorBlocks(Door door) {
        Location bl = door.getBottomLeft();
        Location tr = door.getTopRight();
        World w = bl.getWorld();
        if (w == null) {
            plugin.getLogger().warning("DoorOpener: World is null for door " + door.getDoorId());
            return;
        }

        int minX = bl.getBlockX();
        int minY = bl.getBlockY();
        int minZ = bl.getBlockZ();
        int maxX = tr.getBlockX();
        int maxY = tr.getBlockY();
        int maxZ = tr.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    w.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }
}
