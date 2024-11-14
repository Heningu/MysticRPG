// File: eu/xaru/mysticrpg/dungeons/instance/DungeonInstance.java

package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.instance.puzzles.PuzzleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
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
    private final DebugLoggerModule logger;
    private final DungeonConfig config;
    private final List<UUID> playerUUIDs;
    private final List<Player> playersInInstance;
    private final DungeonManager dungeonManager;
    private World instanceWorld;
    private boolean isRunning;
    private final DungeonEnemyManager dungeonEnemyManager;
    private final ChestManager chestManager;
    private final PuzzleManager puzzleManager;

    public DungeonInstance(
            JavaPlugin plugin,
            DebugLoggerModule logger,
            String dungeonId,
            List<UUID> playerUUIDs,
            DungeonConfigManager configManager,
            DungeonManager dungeonManager
    ) {
        this.instanceId = UUID.randomUUID();
        this.plugin = plugin;
        this.logger = logger;
        this.config = configManager.getDungeonConfig(dungeonId);
        this.playerUUIDs = playerUUIDs;
        this.playersInInstance = new CopyOnWriteArrayList<>();
        this.isRunning = false;
        this.dungeonManager = dungeonManager; // Assign dungeonManager before initializing dependent components
        this.dungeonEnemyManager = new DungeonEnemyManager(plugin, this, config, logger);
        this.chestManager = new ChestManager(plugin, this, config, logger);
        this.puzzleManager = new PuzzleManager(this, config);
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

    public void start() {
        // Run the entire start process synchronously
        Bukkit.getScheduler().runTask(plugin, () -> {
            createInstanceWorld();
            teleportPlayersToInstance();
            initializeInstance();
            isRunning = true;
            logger.log(Level.INFO, "Dungeon instance " + instanceId + " started.", 0);
        });
    }

    public void stop() {
        isRunning = false;
        removePlayersFromInstance();
        unloadInstanceWorld();
        logger.log(Level.INFO, "Dungeon instance " + instanceId + " stopped.", 0);
    }

    private void createInstanceWorld() {
        String templateWorldName = config.getWorldName(); // Use the world name from the config
        if (templateWorldName == null) {
            logger.log(Level.SEVERE, "Dungeon configuration does not contain a world name for dungeon ID: " + config.getId(), 0);
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
                logger.log(Level.SEVERE, "Template world '" + templateWorldName + "' could not be loaded.", 0);
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
            logger.log(Level.SEVERE, "Failed to create instance world: " + e.getMessage(), 0);
        }

        // Verify that instanceWorld is not null
        if (instanceWorld == null) {
            logger.log(Level.SEVERE, "Instance world '" + instanceWorldName + "' could not be loaded.", 0);
        }
    }

    private void unloadInstanceWorld() {
        if (instanceWorld != null) {
            Bukkit.unloadWorld(instanceWorld, false);
            deleteWorld(instanceWorld.getWorldFolder());
        }
    }

    private void teleportPlayersToInstance() {
        Location configSpawnLocation = config.getSpawnLocation();
        if (configSpawnLocation == null || instanceWorld == null) {
            logger.log(Level.SEVERE, "Spawn location is not set or instance world is null in the dungeon configuration.", 0);
            // Stop the dungeon instance since it cannot proceed without a spawn location
            stop();
            return;
        }
        Location spawnLocation = configSpawnLocation.clone();
        spawnLocation.setWorld(instanceWorld);
        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playersInInstance.add(player);
                player.teleport(spawnLocation);
                hideOtherPlayers(player);
            } else {
                logger.log(Level.WARNING, "Player with UUID " + uuid + " is not online. Skipping...", 0);
            }
        }
    }

    private void removePlayersFromInstance() {
        for (Player player : playersInInstance) {
            // Teleport back to main world spawn or a safe location
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            showAllPlayers(player);
        }
        playersInInstance.clear();
    }

    private void initializeInstance() {
        dungeonEnemyManager.spawnMobs();
        chestManager.placeChests();
        puzzleManager.initializePuzzles();
    }

    private void hideOtherPlayers(Player player) {
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!playersInInstance.contains(otherPlayer)) {
                player.hidePlayer(plugin, otherPlayer);
            }
        }
    }

    private void showAllPlayers(Player player) {
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            player.showPlayer(plugin, otherPlayer);
        }
    }

    public void removePlayer(Player player) {
        playersInInstance.remove(player);
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        showAllPlayers(player);
        if (playersInInstance.isEmpty()) {
            dungeonManager.checkAndRemoveInstance(this);
        }
    }

    public boolean areAllMonstersDefeated() {
        return dungeonEnemyManager.areAllMonstersDefeated();
    }

    public void endDungeon() {
        for (Player player : new ArrayList<>(playersInInstance)) {
            player.sendMessage("You have completed the dungeon!");
            removePlayer(player);
        }
    }

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

    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorld(file);
                }
            }
            path.delete();
        }
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }
}
