// File: eu/xaru/mysticrpg/dungeons/portals/PortalManager.java

package eu.xaru.mysticrpg.dungeons.portals;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * Manages portal-related functionalities within a dungeon instance, including particle effects and holograms.
 */
public class PortalManager {

    private final DungeonInstance dungeonInstance;
    private final DungeonConfig dungeonConfig;
    private final JavaPlugin plugin;
    private BukkitTask particleTask;
    private final double particleRadius = 1.0; // Radius around the portal location for particles
    private final double particleHeightOffset = 1.5; // Elevated Y-offset to keep particles above the floor
    private final int particleCount = 12; // Number of particles per cycle

    // Fields for hologram management
    private String exitHologramId;
    private Hologram exitHologram;

    /**
     * Constructor for PortalManager.
     *
     * @param dungeonInstance The current dungeon instance.
     * @param dungeonConfig    The configuration of the dungeon.
     * @param plugin           The main JavaPlugin instance.
     */
    public PortalManager(DungeonInstance dungeonInstance, DungeonConfig dungeonConfig, JavaPlugin plugin) {
        this.dungeonInstance = dungeonInstance;
        this.dungeonConfig = dungeonConfig;
        this.plugin = plugin;
    }

    /**
     * Initiates the finish portal by starting particle effects and creating a hologram at the saved location.
     */
    public void placeFinishPortal() {
        Location portalLocation = dungeonConfig.getPortalPos1();

        if (portalLocation == null) {
            plugin.getLogger().log(Level.WARNING, "Portal position is not set in the dungeon configuration.");
            return;
        }

        // Ensure the position is in the instance world
        World world = dungeonInstance.getInstanceWorld();
        if (!portalLocation.getWorld().equals(world)) {
            plugin.getLogger().log(Level.WARNING, "Portal position is not in the instance world.");
            return;
        }

        // Start the particle effect
        startParticleEffect(portalLocation);

        // Create the hologram
        createExitHologram(portalLocation);
    }

    /**
     * Starts a repeating task to spawn particles at the given location.
     *
     * @param location The location to spawn particles.
     */
    private void startParticleEffect(Location location) {
        plugin.getLogger().info("Starting particle effect at: " +
                location.getWorld().getName() + " X: " + location.getX() +
                " Y: " + location.getY() + " Z: " + location.getZ());

        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Particle effect: FLAME particles in a tighter circle above the portal
            double radius = particleRadius;
            double y = location.getY() + particleHeightOffset; // Elevated Y-offset

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = location.getX() + radius * Math.cos(angle);
                double z = location.getZ() + radius * Math.sin(angle);
                Location particleLocation = new Location(location.getWorld(), x, y, z);
                // Spawn a single particle with minimal spread and speed
                location.getWorld().spawnParticle(Particle.FLAME, particleLocation, 1, 0, 0, 0, 0.0);
            }
        }, 0L, 10L); // Every 0.5 seconds (10 ticks)
    }

    /**
     * Creates a hologram above the exit portal with the text "Exit the dungeon".
     *
     * @param portalLocation The location of the portal.
     */
    private void createExitHologram(Location portalLocation) {
        // Generate a unique hologram ID based on the dungeon instance UUID
        exitHologramId = "dungeon_instance_" + dungeonInstance.getInstanceId() + "_exit_hologram";

        // Center the hologram within the block by adding 0.5 to x and z
        Location hologramLocation = portalLocation.clone();
        hologramLocation.setX(hologramLocation.getX());
        hologramLocation.setZ(hologramLocation.getZ());
        hologramLocation.setY(hologramLocation.getY() + 2);

        // Create the hologram using DHAPI
        exitHologram = DHAPI.createHologram(exitHologramId, hologramLocation);
        if (exitHologram == null) {
            plugin.getLogger().log(Level.WARNING, "Failed to create hologram with ID: " + exitHologramId);
            return;
        }

        // Add the text "Exit the dungeon" to the hologram
        DHAPI.addHologramLine(exitHologram, ChatColor.GREEN + "Exit the dungeon");

        plugin.getLogger().info("Hologram created with ID: " + exitHologramId + " at " + hologramLocation);
    }

    /**
     * Stops the particle effect and removes the hologram.
     */
    public void stopPortal() {
        // Stop particle effects
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
            plugin.getLogger().info("Stopped particle effect for dungeon instance " + dungeonInstance.getInstanceId());
        }

        // Remove the hologram
        if (exitHologramId != null) {
            DHAPI.removeHologram(exitHologramId);
            plugin.getLogger().info("Removed hologram with ID: " + exitHologramId);
            exitHologramId = null;
            exitHologram = null;
        }
    }

    /**
     * Teleports the player and marks the dungeon as completed.
     *
     * @param player The player to teleport.
     */
    public void handlePlayerEntry(Player player) {
        // Define the teleport location (e.g., dungeon lobby or player spawn)
        Location teleportLocation = Bukkit.getWorld("world").getSpawnLocation(); // Adjust as needed

        player.teleport(teleportLocation);
        // Mark the dungeon as completed
        dungeonInstance.endDungeon();
    }

    /**
     * Checks if a player is within the portal radius.
     *
     * @param playerLocation The location of the player.
     * @return True if the player is within the portal area, false otherwise.
     */
    public boolean isPlayerInPortal(Location playerLocation) {
        Location portalLocation = dungeonConfig.getPortalPos1();
        if (portalLocation == null) {
            return false;
        }

        // Ensure both locations are in the same world
        if (!playerLocation.getWorld().equals(portalLocation.getWorld())) {
            return false;
        }

        double distance = playerLocation.distance(portalLocation);
        return distance <= particleRadius;
    }
}
