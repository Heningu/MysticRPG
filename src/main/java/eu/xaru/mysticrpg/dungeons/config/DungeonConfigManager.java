// File: eu/xaru/mysticrpg/dungeons/config/DungeonConfigManager.java

package eu.xaru.mysticrpg.dungeons.config;

import eu.xaru.mysticrpg.dungeons.loot.LootTable;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class DungeonConfigManager {

    private final JavaPlugin plugin;
    private final DebugLoggerModule logger;
    private final Map<String, DungeonConfig> dungeonConfigs;
    private final LootTableManager lootTableManager;

    public DungeonConfigManager(JavaPlugin plugin, DebugLoggerModule logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.dungeonConfigs = new HashMap<>();
        this.lootTableManager = new LootTableManager(plugin, logger);
    }

    public void loadConfigs() {
        File configDir = new File(plugin.getDataFolder(), "dungeons");
        if (!configDir.exists()) {
            configDir.mkdirs();
            createDefaultDungeonConfigs(configDir);
        }

        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (configFiles == null) return;

        for (File file : configFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            DungeonConfig dungeonConfig = parseConfig(config);
            if (dungeonConfig != null) {
                dungeonConfigs.put(dungeonConfig.getId(), dungeonConfig);
                logger.log(Level.INFO, "Loaded dungeon config: " + dungeonConfig.getId(), 0);
            }
        }
    }

    private void createDefaultDungeonConfigs(File configDir) {
        // Implementation for creating default dungeon configs (if needed)
    }

    private DungeonConfig parseConfig(FileConfiguration config) {
        DungeonConfig dungeonConfig = new DungeonConfig();

        dungeonConfig.setId(config.getString("id"));
        dungeonConfig.setName(config.getString("name"));
        dungeonConfig.setMinPlayers(config.getInt("minPlayers", 1));
        dungeonConfig.setMaxPlayers(config.getInt("maxPlayers", 5));
        dungeonConfig.setDifficulty(config.getString("difficulty", "Normal"));
        dungeonConfig.setWorldName(config.getString("worldName")); // Ensure worldName is set

        // Validate required fields
        if (dungeonConfig.getId() == null) {
            logger.log(Level.SEVERE, "Dungeon config is missing an ID. Skipping...", 0);
            return null;
        }

        if (dungeonConfig.getWorldName() == null) {
            logger.log(Level.SEVERE, "Dungeon config '" + dungeonConfig.getId() + "' is missing a world name.", 0);
            return null;
        }

        // Ensure the world is loaded
        World world = Bukkit.getWorld(dungeonConfig.getWorldName());
        if (world == null) {
            // Try to load the world
            WorldCreator creator = new WorldCreator(dungeonConfig.getWorldName());
            world = creator.createWorld();
            if (world == null) {
                logger.log(Level.SEVERE, "World '" + dungeonConfig.getWorldName() + "' could not be loaded.", 0);
                return null;
            }
        }

        // Parse spawn location
        if (config.contains("spawnLocation")) {
            double spawnX = config.getDouble("spawnLocation.x");
            double spawnY = config.getDouble("spawnLocation.y");
            double spawnZ = config.getDouble("spawnLocation.z");
            float spawnYaw = (float) config.getDouble("spawnLocation.yaw", 0);
            float spawnPitch = (float) config.getDouble("spawnLocation.pitch", 0);
            Location spawnLocation = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            dungeonConfig.setSpawnLocation(spawnLocation);
        } else {
            logger.log(Level.WARNING, "Dungeon config '" + dungeonConfig.getId() + "' is missing a spawn location.", 0);
        }

        // Parse mob spawn points
        List<DungeonConfig.MobSpawnPoint> mobSpawnPoints = new ArrayList<>();
        if (config.contains("mob_spawn_points")) {
            ConfigurationSection mobSpawnPointsSection = config.getConfigurationSection("mob_spawn_points");
            for (String key : mobSpawnPointsSection.getKeys(false)) {
                ConfigurationSection mobSpawnSection = mobSpawnPointsSection.getConfigurationSection(key);
                double x = mobSpawnSection.getDouble("x");
                double y = mobSpawnSection.getDouble("y");
                double z = mobSpawnSection.getDouble("z");
                float yaw = (float) mobSpawnSection.getDouble("yaw", 0);
                float pitch = (float) mobSpawnSection.getDouble("pitch", 0);
                String mobId = mobSpawnSection.getString("mob_id");

                if (mobId == null) {
                    logger.log(Level.WARNING, "Mob spawn point '" + key + "' is missing a mob ID. Skipping...", 0);
                    continue;
                }

                Location location = new Location(world, x, y, z, yaw, pitch);
                DungeonConfig.MobSpawnPoint mobSpawnPoint = new DungeonConfig.MobSpawnPoint();
                mobSpawnPoint.setLocation(location);
                mobSpawnPoint.setMobId(mobId);

                mobSpawnPoints.add(mobSpawnPoint);
            }
        }
        dungeonConfig.setMobSpawnPoints(mobSpawnPoints);

        // Parse chest locations and loot tables
        List<DungeonConfig.ChestLocation> chestLocations = new ArrayList<>();
        if (config.contains("chest_locations")) {
            ConfigurationSection chestLocationsSection = config.getConfigurationSection("chest_locations");
            for (String key : chestLocationsSection.getKeys(false)) {
                ConfigurationSection chestSection = chestLocationsSection.getConfigurationSection(key);
                double x = chestSection.getDouble("x");
                double y = chestSection.getDouble("y");
                double z = chestSection.getDouble("z");
                float yaw = (float) chestSection.getDouble("yaw", 0);
                float pitch = (float) chestSection.getDouble("pitch", 0);
                String typeStr = chestSection.getString("type", "CHEST");
                Material chestType = Material.matchMaterial(typeStr.toUpperCase());

                if (chestType == null) {
                    chestType = Material.CHEST; // Default to CHEST if type is invalid
                    logger.log(Level.WARNING, "Invalid chest type '" + typeStr + "' at chest '" + key + "'. Defaulting to CHEST.", 0);
                }

                String lootTableId = chestSection.getString("loot_table");
                // Ensure the loot table exists
                if (lootTableId == null || lootTableManager.getLootTable(lootTableId) == null) {
                    lootTableId = assignDefaultLootTable(chestType);
                    logger.log(Level.WARNING, "Loot table missing or invalid for chest '" + key + "'. Assigned default loot table '" + lootTableId + "'.", 0);
                }

                Location location = new Location(world, x, y, z, yaw, pitch);
                DungeonConfig.ChestLocation chestLocation = new DungeonConfig.ChestLocation();
                chestLocation.setLocation(location);
                chestLocation.setType(chestType);
                chestLocation.setLootTableId(lootTableId);

                chestLocations.add(chestLocation);
            }
        }
        dungeonConfig.setChestLocations(chestLocations);

        return dungeonConfig;
    }

    public DungeonConfig getDungeonConfig(String id) {
        return dungeonConfigs.get(id);
    }

    public Collection<DungeonConfig> getAllConfigs() {
        return dungeonConfigs.values();
    }

    public void addDungeonConfig(DungeonConfig config) {
        dungeonConfigs.put(config.getId(), config);
    }

    public void saveDungeonConfig(DungeonConfig config) {
        File configDir = new File(plugin.getDataFolder(), "dungeons");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, config.getId() + ".yml");
        FileConfiguration fileConfig = new YamlConfiguration();

        // Set configuration values
        fileConfig.set("id", config.getId());
        fileConfig.set("name", config.getName());
        fileConfig.set("minPlayers", config.getMinPlayers());
        fileConfig.set("maxPlayers", config.getMaxPlayers());
        fileConfig.set("difficulty", config.getDifficulty());
        fileConfig.set("worldName", config.getWorldName()); // Added line to save worldName

        if (config.getSpawnLocation() != null) {
            fileConfig.set("spawnLocation.x", config.getSpawnLocation().getX());
            fileConfig.set("spawnLocation.y", config.getSpawnLocation().getY());
            fileConfig.set("spawnLocation.z", config.getSpawnLocation().getZ());
            fileConfig.set("spawnLocation.yaw", config.getSpawnLocation().getYaw());
            fileConfig.set("spawnLocation.pitch", config.getSpawnLocation().getPitch());
        } else {
            logger.log(Level.WARNING, "Spawn location is null for dungeon: " + config.getId(), 0);
        }

        // Save mob spawn points
        ConfigurationSection mobSpawnPointsSection = fileConfig.createSection("mob_spawn_points");
        int mobIndex = 0;
        for (DungeonConfig.MobSpawnPoint spawnPoint : config.getMobSpawnPoints()) {
            if (spawnPoint.getLocation() == null || spawnPoint.getMobId() == null) {
                logger.log(Level.WARNING, "Invalid mob spawn point at index " + mobIndex + ". Skipping...", 0);
                continue;
            }
            ConfigurationSection spawnSection = mobSpawnPointsSection.createSection("spawn" + mobIndex++);
            spawnSection.set("x", spawnPoint.getLocation().getX());
            spawnSection.set("y", spawnPoint.getLocation().getY());
            spawnSection.set("z", spawnPoint.getLocation().getZ());
            spawnSection.set("yaw", spawnPoint.getLocation().getYaw());
            spawnSection.set("pitch", spawnPoint.getLocation().getPitch());
            spawnSection.set("mob_id", spawnPoint.getMobId());
        }

        // Save chest locations
        ConfigurationSection chestLocationsSection = fileConfig.createSection("chest_locations");
        int chestIndex = 0;
        for (DungeonConfig.ChestLocation chestLocation : config.getChestLocations()) {
            if (chestLocation.getLocation() == null || chestLocation.getType() == null || chestLocation.getLootTableId() == null) {
                logger.log(Level.WARNING, "Invalid chest location at index " + chestIndex + ". Skipping...", 0);
                continue;
            }
            ConfigurationSection chestSection = chestLocationsSection.createSection("chest" + chestIndex++);

            chestSection.set("x", chestLocation.getLocation().getX());
            chestSection.set("y", chestLocation.getLocation().getY());
            chestSection.set("z", chestLocation.getLocation().getZ());
            chestSection.set("yaw", chestLocation.getLocation().getYaw());
            chestSection.set("pitch", chestLocation.getLocation().getPitch());
            chestSection.set("type", chestLocation.getType().toString());
            chestSection.set("loot_table", chestLocation.getLootTableId());
        }

        try {
            fileConfig.save(configFile);
            logger.log(Level.INFO, "Dungeon config saved: " + config.getId(), 0);

            // Update the in-memory dungeonConfigs map
            dungeonConfigs.put(config.getId(), config);
            logger.log(Level.INFO, "Dungeon config updated in memory: " + config.getId(), 0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save dungeon config: " + e.getMessage(), 0);
        }
    }

    private String assignDefaultLootTable(Material chestType) {
        String lootTableId;
        if (chestType == Material.CHEST) {
            lootTableId = "default_loot";
        } else if (chestType == Material.TRAPPED_CHEST) {
            lootTableId = "elite_loot";
        } else {
            lootTableId = "default_loot";
        }

        // Check if loot table exists; if not, create it
        if (lootTableManager.getLootTable(lootTableId) == null) {
            LootTable lootTable = new LootTable(lootTableId);
            if (lootTableId.equals("default_loot")) {
                lootTable.addItem("material", "DIAMOND", 1, 1.0); // 100% chance of 1 diamond
            } else if (lootTableId.equals("elite_loot")) {
                lootTable.addItem("material", "NETHER_STAR", 1, 1.0); // 100% chance of 1 nether star
            }
            lootTableManager.saveLootTable(lootTable);
        }

        return lootTableId;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }
}
