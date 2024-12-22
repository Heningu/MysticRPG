package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig.DoorData;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.doors.Door;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
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
import java.util.*;
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

    private long startTime;
    private final Map<UUID, Integer> monsterKills = new HashMap<>();

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

        this.startTime = System.currentTimeMillis();
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

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public void start() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            createInstanceWorld();
            adjustPortalLocationToInstanceWorld();
            teleportPlayersToInstance();
            initializeInstance();
            isRunning = true;
            DebugLogger.getInstance().log(Level.INFO,
                    "Dungeon instance " + instanceId + " started.", 0);
        });
    }

    public void stop() {
        isRunning = false;
        removePlayersFromInstance();
        unloadInstanceWorld();
        portalManager.stopPortal();
        DebugLogger.getInstance().log(Level.INFO,
                "Dungeon instance " + instanceId + " stopped.", 0);
    }

    private void createInstanceWorld() {
        String templateWorldName = config.getWorldName();
        if (templateWorldName == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Dungeon config has no world name for ID: " + config.getId(), 0);
            return;
        }
        String instanceWorldName = "dungeon_instance_" + instanceId;

        World templateWorld = Bukkit.getWorld(templateWorldName);
        if (templateWorld == null) {
            WorldCreator creator = new WorldCreator(templateWorldName);
            templateWorld = creator.createWorld();
            if (templateWorld == null) {
                DebugLogger.getInstance().log(Level.SEVERE,
                        "Template world '" + templateWorldName + "' could not be loaded.", 0);
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
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Failed to create instance world:", e, 0);
        }

        if (instanceWorld == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Instance world '" + instanceWorldName + "' could not be loaded.", 0);
        } else {
            DebugLogger.getInstance().log(Level.INFO,
                    "Instance world '" + instanceWorldName + "' created successfully.", 0);
        }
    }

    private void adjustPortalLocationToInstanceWorld() {
        Location originalPortal = config.getPortalPos1();
        if (originalPortal != null && instanceWorld != null) {
            Location instancePortal = new Location(
                    instanceWorld,
                    originalPortal.getX() + 0.5,
                    originalPortal.getY(),
                    originalPortal.getZ() + 0.5,
                    originalPortal.getYaw(),
                    originalPortal.getPitch()
            );
            config.setPortalPos1(instancePortal);
            DebugLogger.getInstance().log(Level.INFO,
                    "Portal position adjusted to instance world '"
                            + instanceWorld.getName() + "'.", 0);
        } else {
            DebugLogger.getInstance().log(Level.WARNING,
                    "Cannot adjust portal position: PortalPos1 or instanceWorld is null.", 0);
        }
    }

    private void unloadInstanceWorld() {
        if (instanceWorld != null) {
            Bukkit.unloadWorld(instanceWorld, false);
            deleteWorld(instanceWorld.getWorldFolder());
            DebugLogger.getInstance().log(Level.INFO,
                    "Instance world '" + instanceWorld.getName() + "' unloaded and deleted.", 0);
        }
    }

    private void teleportPlayersToInstance() {
        Location configSpawn = config.getSpawnLocation();
        if (configSpawn == null || instanceWorld == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Spawn location is not set or instance world is null in the dungeon config.", 0);
            stop();
            return;
        }
        Location spawnLocation = configSpawn.clone();
        spawnLocation.setWorld(instanceWorld);

        DebugLogger.getInstance().log(Level.INFO,
                "Teleporting players to instance spawn location: "
                        + spawnLocation.getWorld().getName()
                        + " X: " + spawnLocation.getBlockX()
                        + " Y: " + spawnLocation.getBlockY()
                        + " Z: " + spawnLocation.getBlockZ(),
                0);

        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playersInInstance.add(player);
                player.teleport(spawnLocation);
                player.sendMessage(ChatColor.GREEN
                        + "You have been teleported to the dungeon instance.");
            } else {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Player with UUID " + uuid + " is not online. Skipping...", 0);
            }
        }

        // Now show & hide players
        for (Player p : playersInInstance) {
            showAllPlayers(p);
            hideNonInstancePlayers(p);
        }
    }

    private void removePlayersFromInstance() {
        for (Player player : playersInInstance) {
            Location mainSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(mainSpawn);
            showAllPlayers(player);
            player.sendMessage(ChatColor.YELLOW
                    + "You have been removed from the dungeon instance.");
        }
        playersInInstance.clear();
    }

    /**
     * Key logic to place doors as STONE (if they still exist in memory),
     * referencing the *correct* instance world. (FIX APPLIED)
     */
    private void initializeInstance() {
        // Spawn stuff
        dungeonEnemyManager.spawnMobs();
        chestManager.placeChests();
        puzzleManager.initializePuzzles();
        portalManager.placeFinishPortal();

        DoorManager doorManager = dungeonManager.getSetupManager().getDoorManager();
        if (doorManager != null) {
            for (DoorData dd : config.getDoors()) {
                // Remove any old door reference from memory
                if (doorManager.getDoor(dd.getDoorId()) != null) {
                    doorManager.removeDoor(dd.getDoorId());
                }

                // Create a brand-new Door object referencing the instance world
                Location bottomLeft = new Location(instanceWorld, dd.getX1(), dd.getY1(), dd.getZ1());
                Location topRight   = new Location(instanceWorld, dd.getX2(), dd.getY2(), dd.getZ2());
                Door instanceDoor = new Door(dd.getDoorId(), bottomLeft, topRight);
                instanceDoor.setTriggerType(dd.getTriggerType());

                // Store it in the DoorManager
                doorManager.addDoor(instanceDoor);

                // Fill the door region with STONE
                doorManager.placeDoorAsStone(instanceDoor);
            }
        }

        Location portal = config.getPortalPos1();
        if (portal != null) {
            DebugLogger.getInstance().log(Level.INFO,
                    "Portal Location - World: " + portal.getWorld().getName()
                            + " X: " + portal.getX()
                            + " Y: " + portal.getY()
                            + " Z: " + portal.getZ(),
                    0);
        }
    }

    private void hideNonInstancePlayers(Player player) {
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
        Location mainSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(mainSpawn);
        showAllPlayers(player);
        player.sendMessage(ChatColor.YELLOW
                + "You have been removed from the dungeon instance.");
        DebugLogger.getInstance().log(Level.INFO,
                "Player " + player.getName() + " removed from dungeon instance " + instanceId, 0);

        if (playersInInstance.isEmpty()) {
            dungeonManager.checkAndRemoveInstance(this);
        }
    }

    public void endDungeon() {
        int timeSpent = getTimeSpent();
        int minutes = timeSpent / 60;
        int seconds = timeSpent % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        for (Player p : new ArrayList<>(playersInInstance)) {
            int kills = getMonstersKilledByPlayer(p);
            p.sendMessage(ChatColor.GOLD + "You have completed the dungeon!");
            p.sendMessage(ChatColor.GREEN + "Cleared in: " + ChatColor.WHITE + formattedTime
                    + ChatColor.GREEN + ", Monsters killed: " + ChatColor.WHITE + kills);
            removePlayer(p);
        }
        DebugLogger.getInstance().log(Level.INFO,
                "Dungeon instance " + instanceId + " has been completed.", 0);
    }

    private void copyWorld(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
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
                for (File f : files) {
                    deleteWorld(f);
                }
            }
            if (path.delete()) {
                DebugLogger.getInstance().log(Level.INFO,
                        "Deleted world folder: " + path.getName(), 0);
            } else {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Failed to delete world folder: " + path.getName(), 0);
            }
        }
    }

    public void incrementMonsterKill(Player player) {
        int oldCount = monsterKills.getOrDefault(player.getUniqueId(), 0);
        int newCount = oldCount + 1;
        monsterKills.put(player.getUniqueId(), newCount);
        DebugLogger.getInstance().log("incrementMonsterKill: "
                + player.getName() + " now has " + newCount + " kills.");
    }

    public int getMonstersKilledByPlayer(Player player) {
        return monsterKills.getOrDefault(player.getUniqueId(), 0);
    }

    public int getTimeSpent() {
        long now = System.currentTimeMillis();
        return (int)((now - startTime)/1000);
    }

    public List<Player> getPartyMembers() {
        return getPlayersInInstance();
    }
}
