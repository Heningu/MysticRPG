package eu.xaru.mysticrpg.dungeons.config;

import eu.xaru.mysticrpg.dungeons.loot.LootTable;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class DungeonConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, DungeonConfig> dungeonConfigs;
    private final LootTableManager lootTableManager;

    public DungeonConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dungeonConfigs = new HashMap<>();
        this.lootTableManager = new LootTableManager(plugin);
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
                DebugLogger.getInstance().log(Level.INFO, "Loaded dungeon config: " + dungeonConfig.getId(), 0);
            }
        }
    }

    private void createDefaultDungeonConfigs(File configDir) {
        // Implement if needed
    }

    private DungeonConfig parseConfig(FileConfiguration config) {
        DungeonConfig dungeonConfig = new DungeonConfig();

        dungeonConfig.setId(config.getString("id"));
        dungeonConfig.setName(config.getString("name"));
        dungeonConfig.setMinPlayers(config.getInt("minPlayers", 1));
        dungeonConfig.setMaxPlayers(config.getInt("maxPlayers", 5));
        dungeonConfig.setDifficulty(config.getString("difficulty", "Normal"));
        dungeonConfig.setWorldName(config.getString("worldName"));
        dungeonConfig.setLevelRequirement(config.getInt("levelRequirement", 1));

        if (dungeonConfig.getId() == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Dungeon config is missing an ID. Skipping...", 0);
            return null;
        }

        if (dungeonConfig.getWorldName() == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Dungeon config '" + dungeonConfig.getId() + "' is missing a world name.", 0);
            return null;
        }

        World world = Bukkit.getWorld(dungeonConfig.getWorldName());
        if (world == null) {
            WorldCreator creator = new WorldCreator(dungeonConfig.getWorldName());
            world = creator.createWorld();
            if (world == null) {
                DebugLogger.getInstance().log(Level.SEVERE, "World '" + dungeonConfig.getWorldName() + "' could not be loaded.", 0);
                return null;
            }
        }

        // Spawn location
        if (config.contains("spawnLocation")) {
            double spawnX = config.getDouble("spawnLocation.x");
            double spawnY = config.getDouble("spawnLocation.y");
            double spawnZ = config.getDouble("spawnLocation.z");
            float spawnYaw = (float) config.getDouble("spawnLocation.yaw", 0);
            float spawnPitch = (float) config.getDouble("spawnLocation.pitch", 0);
            Location spawnLocation = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            dungeonConfig.setSpawnLocation(spawnLocation);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "Dungeon config '" + dungeonConfig.getId() + "' is missing a spawn location.", 0);
        }

        // Mob spawn points
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
                    DebugLogger.getInstance().log(Level.WARNING, "Mob spawn point '" + key + "' is missing a mob ID. Skipping...", 0);
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

        // Chest locations
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
                    chestType = Material.CHEST;
                    DebugLogger.getInstance().log(Level.WARNING, "Invalid chest type '" + typeStr + "' at chest '" + key + "'. Defaulting to CHEST.", 0);
                }

                String lootTableId = chestSection.getString("loot_table");
                if (lootTableId == null || lootTableManager.getLootTable(lootTableId) == null) {
                    lootTableId = assignDefaultLootTable(chestType);
                    DebugLogger.getInstance().log(Level.WARNING, "Loot table missing or invalid for chest '" + key + "'. Assigned default loot table '" + lootTableId + "'.", 0);
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

        // Portal position
        if (config.contains("portalPos1")) {
            double portalX = config.getDouble("portalPos1.x");
            double portalY = config.getDouble("portalPos1.y");
            double portalZ = config.getDouble("portalPos1.z");
            float portalYaw = (float) config.getDouble("portalPos1.yaw", 0);
            float portalPitch = (float) config.getDouble("portalPos1.pitch", 0);
            Location portalLocation = new Location(world, portalX, portalY, portalZ, portalYaw, portalPitch);
            dungeonConfig.setPortalPos1(portalLocation);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "Dungeon config '" + dungeonConfig.getId() + "' is missing a portal position.", 0);
        }

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

        fileConfig.set("id", config.getId());
        fileConfig.set("name", config.getName());
        fileConfig.set("minPlayers", config.getMinPlayers());
        fileConfig.set("maxPlayers", config.getMaxPlayers());
        fileConfig.set("difficulty", config.getDifficulty());
        fileConfig.set("worldName", config.getWorldName());
        fileConfig.set("levelRequirement", config.getLevelRequirement());

        if (config.getSpawnLocation() != null) {
            fileConfig.set("spawnLocation.x", config.getSpawnLocation().getX());
            fileConfig.set("spawnLocation.y", config.getSpawnLocation().getY());
            fileConfig.set("spawnLocation.z", config.getSpawnLocation().getZ());
            fileConfig.set("spawnLocation.yaw", config.getSpawnLocation().getYaw());
            fileConfig.set("spawnLocation.pitch", config.getSpawnLocation().getPitch());
        }

        ConfigurationSection mobSpawnPointsSection = fileConfig.createSection("mob_spawn_points");
        int mobIndex = 0;
        for (DungeonConfig.MobSpawnPoint spawnPoint : config.getMobSpawnPoints()) {
            ConfigurationSection spawnSection = mobSpawnPointsSection.createSection("spawn" + mobIndex++);
            spawnSection.set("x", spawnPoint.getLocation().getX());
            spawnSection.set("y", spawnPoint.getLocation().getY());
            spawnSection.set("z", spawnPoint.getLocation().getZ());
            spawnSection.set("yaw", spawnPoint.getLocation().getYaw());
            spawnSection.set("pitch", spawnPoint.getLocation().getPitch());
            spawnSection.set("mob_id", spawnPoint.getMobId());
        }

        ConfigurationSection chestLocationsSection = fileConfig.createSection("chest_locations");
        int chestIndex = 0;
        for (DungeonConfig.ChestLocation chestLocation : config.getChestLocations()) {
            ConfigurationSection chestSection = chestLocationsSection.createSection("chest" + chestIndex++);
            chestSection.set("x", chestLocation.getLocation().getX());
            chestSection.set("y", chestLocation.getLocation().getY());
            chestSection.set("z", chestLocation.getLocation().getZ());
            chestSection.set("yaw", chestLocation.getLocation().getYaw());
            chestSection.set("pitch", chestLocation.getLocation().getPitch());
            chestSection.set("type", chestLocation.getType().toString());
            chestSection.set("loot_table", chestLocation.getLootTableId());
        }

        if (config.getPortalPos1() != null) {
            fileConfig.set("portalPos1.x", config.getPortalPos1().getX());
            fileConfig.set("portalPos1.y", config.getPortalPos1().getY());
            fileConfig.set("portalPos1.z", config.getPortalPos1().getZ());
            fileConfig.set("portalPos1.yaw", config.getPortalPos1().getYaw());
            fileConfig.set("portalPos1.pitch", config.getPortalPos1().getPitch());
        }

        try {
            fileConfig.save(configFile);
            DebugLogger.getInstance().log(Level.INFO, "Dungeon config saved: " + config.getId(), 0);
            dungeonConfigs.put(config.getId(), config);
        } catch (Exception e) {
            DebugLogger.getInstance().log(Level.SEVERE, "Failed to save dungeon config:", e, 0);
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

        if (lootTableManager.getLootTable(lootTableId) == null) {
            LootTable lootTable = new LootTable(lootTableId);
            if (lootTableId.equals("default_loot")) {
                lootTable.addItem("material", "DIAMOND", 1, 1.0);
            } else if (lootTableId.equals("elite_loot")) {
                lootTable.addItem("material", "NETHER_STAR", 1, 1.0);
            }
            lootTableManager.saveLootTable(lootTable);
        }

        return lootTableId;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }
}