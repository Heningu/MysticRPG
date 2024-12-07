// File: eu/xaru/mysticrpg/dungeons/instance/DungeonInstance.java

package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.instance.puzzles.PuzzleManager;
import eu.xaru.mysticrpg.dungeons.portals.PortalManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class DungeonInstance {

    private final UUID instanceId;
    private final JavaPlugin plugin;
    
    private final DungeonConfig config;
    private final List<UUID> playerUUIDs;
    private final List<Player> playersInInstance;
    private final DungeonManager dungeonManager;
    private World instanceWorld;
    private boolean isRunning;
    private final DungeonEnemyManager dungeonEnemyManager;
    private final ChestManager chestManager;
    private final PuzzleManager puzzleManager;
    private final PortalManager portalManager;

    /**
     * Constructor for DungeonInstance.
     *
     * @param plugin         The main JavaPlugin instance.
              The logger for debugging and information.
     * @param dungeonId      The ID of the dungeon configuration to use.
     * @param playerUUIDs    The list of player UUIDs participating in the dungeon.
     * @param configManager  The manager handling dungeon configurations.
     * @param dungeonManager The main dungeon manager.
     */
    public DungeonInstance(
            JavaPlugin plugin,
            
            String dungeonId,
            List<UUID> playerUUIDs,
            DungeonConfigManager configManager,
            DungeonManager dungeonManager
    ) {
        this.instanceId = UUID.randomUUID();
        this.plugin = plugin;
 
        this.config = configManager.getDungeonConfig(dungeonId);
        this.playerUUIDs = playerUUIDs;
        this.playersInInstance = new CopyOnWriteArrayList<>();
        this.isRunning = false;
        this.dungeonManager = dungeonManager;
        this.dungeonEnemyManager = new DungeonEnemyManager(plugin, this, config);
        this.chestManager = new ChestManager(plugin, this, config);
        this.puzzleManager = new PuzzleManager(this, config);
        this.portalManager = new PortalManager(this, config, plugin);
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public World getInstanceWorld() {
        return instanceWorld;
    }

    public List<Player> getPlayersInInstance() {
        return playersInInstance;
    }

    public boolean containsPlayer(UUID playerUUID) {
        return playerUUIDs.contains(playerUUID);
    }

    public DungeonConfig getConfig() {
        return config;
    }

    /**
     * Provides access to the DungeonManager.
     *
     * @return The DungeonManager instance.
     */
    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    /**
     * Provides access to the PortalManager.
     *
     * @return The PortalManager instance.
     */
    public PortalManager getPortalManager() {
        return portalManager;
    }

    /**
     * Starts the dungeon instance.
     */
    public void start() {
        // Run the entire start process synchronously to ensure proper world loading and player teleportation
        Bukkit.getScheduler().runTask(plugin, () -> {
            createInstanceWorld();
            adjustPortalLocationToInstanceWorld(); // Center the portal location
            teleportPlayersToInstance();
            initializeInstance();
            isRunning = true;
            DebugLogger.getInstance().log(Level.INFO, "Dungeon instance " + instanceId + " started.", 0);
        });
    }

    /**
     * Stops the dungeon instance, removing players and unloading the world.
     */
    public void stop() {
        isRunning = false;
        removePlayersFromInstance();
        unloadInstanceWorld();
        // Stop portal particle effects
        portalManager.stopPortal();
        DebugLogger.getInstance().log(Level.INFO, "Dungeon instance " + instanceId + " stopped.", 0);
    }

    /**
     * Creates a copy of the template world for the dungeon instance.
     */
    private void createInstanceWorld() {
        String templateWorldName = config.getWorldName();
        if (templateWorldName == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Dungeon configuration does not contain a world name for dungeon ID: " + config.getId(), 0);
            return;
        }
        String instanceWorldName = "dungeon_instance_" + instanceId;

        // Ensure the template world is loaded
        World templateWorld = Bukkit.getWorld(templateWorldName);
        if (templateWorld == null) {
            // Try to load the world
            WorldCreator creator = new WorldCreator(templateWorldName);
            templateWorld = creator.createWorld();
            if (templateWorld == null) {
                DebugLogger.getInstance().log(Level.SEVERE, "Template world '" + templateWorldName + "' could not be loaded.", 0);
                return;
            }
        }

        File templateWorldFolder = templateWorld.getWorldFolder();
        File instanceWorldFolder = new File(Bukkit.getWorldContainer(), instanceWorldName);

        try {
            copyWorld(templateWorldFolder, instanceWorldFolder);
            WorldCreator worldCreator = new WorldCreator(instanceWorldName);
            instanceWorld = worldCreator.createWorld();
            instanceWorld.setAutoSave(false);
            instanceWorld.setDifficulty(Difficulty.NORMAL);
        } catch (Exception e) {
            DebugLogger.getInstance().log(Level.SEVERE, "Failed to create instance world:", e, 0);
        }

        if (instanceWorld == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Instance world '" + instanceWorldName + "' could not be loaded.", 0);
        } else {
            DebugLogger.getInstance().log(Level.INFO, "Instance world '" + instanceWorldName + "' created successfully.", 0);
        }
    }

    /**
     * Adjusts the portal location to the instance world by centering it.
     */
    private void adjustPortalLocationToInstanceWorld() {
        Location originalPortal = config.getPortalPos1();
        if (originalPortal != null && instanceWorld != null) {
            // Create a new Location object in the instance world with centered coordinates
            Location instancePortal = new Location(
                    instanceWorld,
                    originalPortal.getX() + 0.5, // Center X
                    originalPortal.getY(),       // Y remains the same
                    originalPortal.getZ() + 0.5, // Center Z
                    originalPortal.getYaw(),
                    originalPortal.getPitch()
            );
            config.setPortalPos1(instancePortal);
            DebugLogger.getInstance().log(Level.INFO, "Portal position adjusted to instance world '" + instanceWorld.getName() + "'.", 0);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "Cannot adjust portal position: PortalPos1 or InstanceWorld is null.", 0);
        }
    }

    /**
     * Unloads the instance world and deletes its folder.
     */
    private void unloadInstanceWorld() {
        if (instanceWorld != null) {
            Bukkit.unloadWorld(instanceWorld, false);
            deleteWorld(instanceWorld.getWorldFolder());
            DebugLogger.getInstance().log(Level.INFO, "Instance world '" + instanceWorld.getName() + "' unloaded and deleted.", 0);
        }
    }

    /**
     * Teleports all participating players to the instance world's spawn location.
     */
    private void teleportPlayersToInstance() {
        Location configSpawnLocation = config.getSpawnLocation();
        if (configSpawnLocation == null || instanceWorld == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Spawn location is not set or instance world is null in the dungeon configuration.", 0);
            // Stop the dungeon instance since it cannot proceed without a spawn location
            stop();
            return;
        }
        Location spawnLocation = configSpawnLocation.clone();
        spawnLocation.setWorld(instanceWorld);

        // Log spawn location for debugging
        DebugLogger.getInstance().log(Level.INFO, "Teleporting players to instance spawn location: " +
                spawnLocation.getWorld().getName() + " X: " + spawnLocation.getBlockX() +
                " Y: " + spawnLocation.getBlockY() + " Z: " + spawnLocation.getBlockZ(), 0);

        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playersInInstance.add(player);
                player.teleport(spawnLocation);
                hideOtherPlayers(player);
                player.sendMessage(ChatColor.GREEN + "You have been teleported to the dungeon instance.");
            } else {
                DebugLogger.getInstance().log(Level.WARNING, "Player with UUID " + uuid + " is not online. Skipping...", 0);
            }
        }
    }

    /**
     * Removes all players from the dungeon instance and teleports them back to the main world's spawn.
     */
    private void removePlayersFromInstance() {
        for (Player player : playersInInstance) {
            // Teleport back to main world spawn or a safe location
            Location mainSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(mainSpawn);
            showAllPlayers(player);
            player.sendMessage(ChatColor.YELLOW + "You have been removed from the dungeon instance.");
        }
        playersInInstance.clear();
    }

    /**
     * Initializes various components of the dungeon instance, including enemies, chests, puzzles, and portals.
     */
    private void initializeInstance() {
        dungeonEnemyManager.spawnMobs();
        chestManager.placeChests();
        puzzleManager.initializePuzzles();
        portalManager.placeFinishPortal(); // Start particle effects at the portal location

        // Log the portal location for verification
        Location portal = config.getPortalPos1();
        if (portal != null) {
            DebugLogger.getInstance().log(Level.INFO, "Portal Location - World: " + portal.getWorld().getName() +
                    " X: " + portal.getX() + " Y: " + portal.getY() + " Z: " + portal.getZ(), 0);
        } else {
            DebugLogger.getInstance().log(Level.SEVERE, "Portal position is not set for dungeon instance " + instanceId, 0);
        }
    }

    /**
     * Hides all other online players from the specified player to prevent interference.
     *
     * @param player The player to hide others from.
     */
    private void hideOtherPlayers(Player player) {
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!playersInInstance.contains(otherPlayer)) {
                player.hidePlayer(plugin, otherPlayer);
            }
        }
    }

    /**
     * Shows all previously hidden players to the specified player.
     *
     * @param player The player to show others to.
     */
    private void showAllPlayers(Player player) {
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            player.showPlayer(plugin, otherPlayer);
        }
    }

    /**
     * Removes a single player from the dungeon instance.
     *
     * @param player The player to remove.
     */
    public void removePlayer(Player player) {
        playersInInstance.remove(player);
        // Teleport back to main world spawn or a safe location
        Location mainSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(mainSpawn);
        showAllPlayers(player);
        player.sendMessage(ChatColor.YELLOW + "You have been removed from the dungeon instance.");
        DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " removed from dungeon instance " + instanceId, 0);

        if (playersInInstance.isEmpty()) {
            dungeonManager.checkAndRemoveInstance(this);
        }
    }

    /**
     * Ends the dungeon, notifying all players and removing them from the instance.
     */
    public void endDungeon() {
        for (Player player : new ArrayList<>(playersInInstance)) {
            player.sendMessage(ChatColor.GOLD + "You have completed the dungeon!");
            removePlayer(player);
        }
        DebugLogger.getInstance().log(Level.INFO, "Dungeon instance " + instanceId + " has been completed.", 0);
    }

    /**
     * Copies the template world to create a new instance world.
     *
     * @param source The source world folder.
     * @param target The target world folder.
     * @throws IOException If an I/O error occurs during copying.
     */
    private void copyWorld(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    // Skip copying session.lock and uid.dat files
                    if (file.equals("session.lock") || file.equals("uid.dat")) {
                        continue;
                    }
                    File srcFile = new File(source, file);
                    File destFile = new File(target, file);
                    copyWorld(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Deletes the specified world folder and all its contents.
     *
     * @param path The world folder to delete.
     */
    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorld(file);
                }
            }
            if (path.delete()) {
                DebugLogger.getInstance().log(Level.INFO, "Deleted world folder: " + path.getName(), 0);
            } else {
                DebugLogger.getInstance().log(Level.WARNING, "Failed to delete world folder: " + path.getName(), 0);
            }
        }
    }
}
