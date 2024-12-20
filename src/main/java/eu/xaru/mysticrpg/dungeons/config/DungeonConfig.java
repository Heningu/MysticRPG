package eu.xaru.mysticrpg.dungeons.config;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class DungeonConfig {

    private String id;
    private String name;
    private int minPlayers;
    private int maxPlayers;
    private String difficulty;
    private Location spawnLocation;
    private List<MobSpawnPoint> mobSpawnPoints = new ArrayList<>();
    private List<ChestLocation> chestLocations = new ArrayList<>();
    private String worldName;
    private Location portalPos1;
    private int levelRequirement = 1; // Default to 1

    // Add door data
    private List<DoorData> doors = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getName() {
        if (name == null || name.isEmpty()) {
            return "Unnamed Dungeon";
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getDifficulty() {
        if (difficulty == null || difficulty.isEmpty()) {
            return "Normal";
        }
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public List<MobSpawnPoint> getMobSpawnPoints() {
        return mobSpawnPoints;
    }

    public void setMobSpawnPoints(List<MobSpawnPoint> mobSpawnPoints) {
        this.mobSpawnPoints = mobSpawnPoints;
    }

    public List<ChestLocation> getChestLocations() {
        return chestLocations;
    }

    public void setChestLocations(List<ChestLocation> chestLocations) {
        this.chestLocations = chestLocations;
    }

    public Location getPortalPos1() {
        return portalPos1;
    }

    public void setPortalPos1(Location portalPos1) {
        this.portalPos1 = portalPos1;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public void setLevelRequirement(int levelRequirement) {
        this.levelRequirement = levelRequirement;
    }

    public List<DoorData> getDoors() {
        return doors;
    }

    public void setDoors(List<DoorData> doors) {
        this.doors = doors;
    }

    public static class MobSpawnPoint {
        private Location location;
        private String mobId;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getMobId() {
            return mobId;
        }

        public void setMobId(String mobId) {
            this.mobId = mobId;
        }
    }

    public static class ChestLocation {
        private Location location;
        private Material type;
        private String lootTableId;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public Material getType() {
            return type;
        }

        public void setType(Material type) {
            this.type = type;
        }

        public String getLootTableId() {
            return lootTableId;
        }

        public void setLootTableId(String lootTableId) {
            this.lootTableId = lootTableId;
        }

    }

    public static class DoorData {
        private String doorId;
        private double x1, y1, z1;
        private double x2, y2, z2;
        private String triggerType;

        public String getDoorId() {
            return doorId;
        }

        public void setDoorId(String doorId) {
            this.doorId = doorId;
        }

        public double getX1() { return x1; }
        public double getY1() { return y1; }
        public double getZ1() { return z1; }
        public double getX2() { return x2; }
        public double getY2() { return y2; }
        public double getZ2() { return z2; }

        public void setX1(double x1) { this.x1 = x1; }
        public void setY1(double y1) { this.y1 = y1; }
        public void setZ1(double z1) { this.z1 = z1; }
        public void setX2(double x2) { this.x2 = x2; }
        public void setY2(double y2) { this.y2 = y2; }
        public void setZ2(double z2) { this.z2 = z2; }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }
    }
}
