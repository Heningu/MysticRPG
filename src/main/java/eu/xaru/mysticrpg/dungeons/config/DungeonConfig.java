// File: eu/xaru/mysticrpg/dungeons/config/DungeonConfig.java

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
    private int difficultyLevel;
    private Location spawnLocation;
    private List<MobSpawnPoint> mobSpawnPoints = new ArrayList<>();
    private List<ChestLocation> chestLocations = new ArrayList<>();

    // Getters and Setters for all fields

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
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

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
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

        // Getters and Setters

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

        // Getters and Setters

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
}
