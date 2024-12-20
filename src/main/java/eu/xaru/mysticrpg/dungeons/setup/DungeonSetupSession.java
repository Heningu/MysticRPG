package eu.xaru.mysticrpg.dungeons.setup;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.doors.Door;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DungeonSetupSession {

    private final Player player;
    private final DungeonConfig config;
    private boolean isSettingPortal;
    private Location portalPos1;

    private boolean isSettingDoor;
    private String currentDoorId;
    private Location doorCorner1;
    private final DoorManager doorManager;

    public DungeonSetupSession(Player player, String dungeonId, DoorManager doorManager) {
        this.player = player;
        this.config = new DungeonConfig();
        this.config.setId(dungeonId);
        this.doorManager = doorManager;
    }

    public Player getPlayer() {
        return player;
    }

    public DungeonConfig getConfig() {
        return config;
    }

    public void setSpawnLocation(Location location) {
        config.setSpawnLocation(location);
        player.sendMessage("Dungeon spawn location set.");
    }

    public void addMobSpawnPoint(String mobId, Location location) {
        DungeonConfig.MobSpawnPoint spawnPoint = new DungeonConfig.MobSpawnPoint();
        spawnPoint.setMobId(mobId);
        spawnPoint.setLocation(location);
        config.getMobSpawnPoints().add(spawnPoint);
        player.sendMessage("Mob spawn point added.");
    }

    public void addChestLocation(String chestType, Location location) {
        DungeonConfig.ChestLocation chestLocation = new DungeonConfig.ChestLocation();
        chestLocation.setLocation(location);

        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(chestType.toUpperCase());
        if (mat == null) {
            mat = org.bukkit.Material.CHEST;
            player.sendMessage("Invalid chest type provided. Defaulting to CHEST.");
        }
        chestLocation.setType(mat);
        chestLocation.setLootTableId(mat == org.bukkit.Material.TRAPPED_CHEST ? "elite_loot" : "default_loot");
        config.getChestLocations().add(chestLocation);
        player.sendMessage("Chest of type '" + mat.toString() + "' added with loot table: " + chestLocation.getLootTableId());
    }

    // Portal Setup
    public void startPortalSetup() {
        isSettingPortal = true;
        portalPos1 = null;
        player.sendMessage("Portal setup started. Please click on the portal location.");
    }

    public boolean isSettingPortal() {
        return isSettingPortal;
    }

    public void setPortalPos1(Location location) {
        portalPos1 = location;
        config.setPortalPos1(location);
        isSettingPortal = false;
        player.sendMessage("Portal position set at: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        player.sendMessage("Portal setup complete. Remember to save the dungeon configuration.");
    }

    public Location getPortalPos1() {
        return portalPos1;
    }

    // Door setup
    public void startDoorSetup(String doorId) {
        isSettingDoor = true;
        currentDoorId = doorId;
        doorCorner1 = null;
        player.sendMessage(ChatColor.GREEN + "Door setup started for Door ID: " + doorId + ". Click the first corner block.");
    }

    public boolean isSettingDoor() {
        return isSettingDoor;
    }

    public String getCurrentDoorId() {
        return currentDoorId;
    }

    public void setDoorCorner(Location location) {
        if (doorCorner1 == null) {
            doorCorner1 = location;
            player.sendMessage(ChatColor.GREEN + "First corner set. Now click the second corner (opposite corner).");
        } else {
            Location corner2 = location;

            double minX = Math.min(doorCorner1.getX(), corner2.getX());
            double minY = Math.min(doorCorner1.getY(), corner2.getY());
            double minZ = Math.min(doorCorner1.getZ(), corner2.getZ());

            double maxX = Math.max(doorCorner1.getX(), corner2.getX());
            double maxY = Math.max(doorCorner1.getY(), corner2.getY());
            double maxZ = Math.max(doorCorner1.getZ(), corner2.getZ());

            Location bottomLeft = new Location(doorCorner1.getWorld(), minX, minY, minZ);
            Location topRight = new Location(doorCorner1.getWorld(), maxX, maxY, maxZ);

            Door door = new Door(currentDoorId, bottomLeft, topRight);
            if (!doorManager.addDoor(door)) {
                player.sendMessage(ChatColor.RED + "A door with that ID already exists or an error occurred.");
            } else {
                // Add door data to config
                DungeonConfig.DoorData dd = new DungeonConfig.DoorData();
                dd.setDoorId(currentDoorId);
                dd.setX1(minX);
                dd.setY1(minY);
                dd.setZ1(minZ);
                dd.setX2(maxX);
                dd.setY2(maxY);
                dd.setZ2(maxZ);
                dd.setTriggerType("none");
                config.getDoors().add(dd);

                player.sendMessage(ChatColor.GREEN + "Door '" + currentDoorId + "' created successfully.");
                // Show placeholder now
                doorManager.buildDoor(door);
            }

            isSettingDoor = false;
            currentDoorId = null;
            doorCorner1 = null;
        }
    }
}
