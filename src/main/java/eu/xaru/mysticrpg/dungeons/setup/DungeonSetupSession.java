package eu.xaru.mysticrpg.dungeons.setup;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig.DoorData;
import eu.xaru.mysticrpg.dungeons.doors.Door;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DungeonSetupSession {

    private final Player player;
    private final DungeonConfig config;

    // Portal setup
    private boolean isSettingPortal;
    private Location portalPos1;

    // Door setup
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

    // ------------------------------
    // Spawn / Mobs / Chests
    // ------------------------------
    public void setSpawnLocation(Location location) {
        config.setSpawnLocation(location);
        player.sendMessage("Dungeon spawn location set.");
    }

    public void addMobSpawnPoint(String mobId, Location location) {
        DungeonConfig.MobSpawnPoint sp = new DungeonConfig.MobSpawnPoint();
        sp.setMobId(mobId);
        sp.setLocation(location);
        config.getMobSpawnPoints().add(sp);
        player.sendMessage("Mob spawn point added for: " + mobId);
    }

    public void addChestLocation(String chestType, Location loc) {
        DungeonConfig.ChestLocation chestLoc = new DungeonConfig.ChestLocation();
        chestLoc.setLocation(loc);

        Material mat = Material.matchMaterial(chestType.toUpperCase());
        if (mat == null) {
            mat = Material.CHEST;
            player.sendMessage("Invalid chest type provided. Defaulting to CHEST.");
        }
        chestLoc.setType(mat);
        chestLoc.setLootTableId(mat == Material.TRAPPED_CHEST ? "elite_loot" : "default_loot");
        config.getChestLocations().add(chestLoc);

        player.sendMessage("Chest of type '" + mat + "' with loot: " + chestLoc.getLootTableId());
    }

    // ------------------------------
    // Portal
    // ------------------------------
    public void startPortalSetup() {
        isSettingPortal = true;
        portalPos1 = null;
        player.sendMessage("Portal setup started. Please RIGHT-click the portal location block.");
    }

    public boolean isSettingPortal() {
        return isSettingPortal;
    }

    public void setPortalPos1(Location location) {
        portalPos1 = location;
        config.setPortalPos1(location);
        isSettingPortal = false;

        player.sendMessage("Portal location set at: (" + location.getBlockX()
                + ", " + location.getBlockY() + ", " + location.getBlockZ() + ").");
        player.sendMessage("Portal setup complete. Use /ds end when finished.");
    }

    // ------------------------------
    // Door Setup
    // ------------------------------
    public void startDoorSetup(String doorId) {
        isSettingDoor = true;
        currentDoorId = doorId;
        doorCorner1 = null;

        player.sendMessage(ChatColor.GREEN
                + "Door setup started for ID: " + doorId
                + ". RIGHT-click the first corner block now.");
    }

    public boolean isSettingDoor() {
        return isSettingDoor;
    }

    public String getCurrentDoorId() {
        return currentDoorId;
    }

    public void setDoorCorner(Location loc) {
        if (doorCorner1 == null) {
            // first corner
            doorCorner1 = loc;
            player.sendMessage(ChatColor.GREEN + "First corner set at ("
                    + loc.getBlockX() + ", "
                    + loc.getBlockY() + ", "
                    + loc.getBlockZ() + "). Now RIGHT-click the opposite corner.");
        } else {
            // second corner
            Location corner2 = loc;
            int x1 = doorCorner1.getBlockX();
            int y1 = doorCorner1.getBlockY();
            int z1 = doorCorner1.getBlockZ();

            int x2 = corner2.getBlockX();
            int y2 = corner2.getBlockY();
            int z2 = corner2.getBlockZ();

            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);

            Location bottomLeft = new Location(doorCorner1.getWorld(), minX, minY, minZ);
            Location topRight   = new Location(doorCorner1.getWorld(), maxX, maxY, maxZ);

            Door door = new Door(currentDoorId, bottomLeft, topRight);
            boolean added = doorManager.addDoor(door);
            if (!added) {
                player.sendMessage(ChatColor.RED
                        + "Door with ID '" + currentDoorId + "' already exists!");
            } else {
                // add to config
                DoorData dd = new DoorData();
                dd.setDoorId(currentDoorId);
                dd.setX1(minX); dd.setY1(minY); dd.setZ1(minZ);
                dd.setX2(maxX); dd.setY2(maxY); dd.setZ2(maxZ);
                dd.setTriggerType("none");
                config.getDoors().add(dd);

                // Show only flame
                doorManager.buildDoor(door);

                player.sendMessage(ChatColor.GREEN + "Door '" + currentDoorId
                        + "' set from (" + minX + "," + minY + "," + minZ + ") to ("
                        + maxX + "," + maxY + "," + maxZ + ").");
            }

            isSettingDoor = false;
            currentDoorId = null;
            doorCorner1 = null;
        }
    }
}
