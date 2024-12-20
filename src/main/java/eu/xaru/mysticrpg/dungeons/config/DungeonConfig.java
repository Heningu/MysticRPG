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
}