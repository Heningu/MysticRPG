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

    private DungeonConfig parseConfig(FileConfiguration config) {
        DungeonConfig dc = new DungeonConfig();
        dc.setId(config.getString("id"));
        dc.setName(config.getString("name"));
        dc.setMinPlayers(config.getInt("minPlayers", 1));
        dc.setMaxPlayers(config.getInt("maxPlayers", 5));
        dc.setDifficulty(config.getString("difficulty", "Normal"));
        dc.setWorldName(config.getString("worldName"));
        dc.setLevelRequirement(config.getInt("levelRequirement", 1));

        if (dc.getId() == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Dungeon config missing ID. Skipping...", 0);
            return null;
        }
        if (dc.getWorldName() == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Dungeon '" + dc.getId() + "' missing world name.", 0);
            return null;
        }

        World w = Bukkit.getWorld(dc.getWorldName());
        if (w == null) {
            w = new WorldCreator(dc.getWorldName()).createWorld();
            if (w == null) {
                DebugLogger.getInstance().log(Level.SEVERE, "World '" + dc.getWorldName() + "' can't be loaded.", 0);
                return null;
            }
        }

        // spawnLocation
        if (config.contains("spawnLocation")) {
            double sx = config.getDouble("spawnLocation.x");
            double sy = config.getDouble("spawnLocation.y");
            double sz = config.getDouble("spawnLocation.z");
            float syaw   = (float) config.getDouble("spawnLocation.yaw", 0);
            float spitch = (float) config.getDouble("spawnLocation.pitch", 0);
            dc.setSpawnLocation(new Location(w, sx, sy, sz, syaw, spitch));
        }

        // mob spawn points
        if (config.contains("mob_spawn_points")) {
            ConfigurationSection mobSec = config.getConfigurationSection("mob_spawn_points");
            List<DungeonConfig.MobSpawnPoint> mobList = new ArrayList<>();
            for (String key : mobSec.getKeys(false)) {
                ConfigurationSection ms = mobSec.getConfigurationSection(key);
                double x = ms.getDouble("x");
                double y = ms.getDouble("y");
                double z = ms.getDouble("z");
                float yaw = (float) ms.getDouble("yaw", 0f);
                float pit = (float) ms.getDouble("pitch", 0f);
                String mobId = ms.getString("mob_id", "zombie");
                Location loc = new Location(w, x, y, z, yaw, pit);

                DungeonConfig.MobSpawnPoint mp = new DungeonConfig.MobSpawnPoint();
                mp.setLocation(loc);
                mp.setMobId(mobId);
                mobList.add(mp);
            }
            dc.setMobSpawnPoints(mobList);
        }

        // chest locations
        if (config.contains("chest_locations")) {
            ConfigurationSection chestSec = config.getConfigurationSection("chest_locations");
            List<DungeonConfig.ChestLocation> chestList = new ArrayList<>();
            for (String ck : chestSec.getKeys(false)) {
                ConfigurationSection c2 = chestSec.getConfigurationSection(ck);
                double x = c2.getDouble("x");
                double y = c2.getDouble("y");
                double z = c2.getDouble("z");
                float yaw = (float) c2.getDouble("yaw", 0f);
                float pit = (float) c2.getDouble("pitch", 0f);
                String typeStr = c2.getString("type", "CHEST");
                Material mat = Material.matchMaterial(typeStr.toUpperCase());
                if (mat == null) {
                    mat = Material.CHEST;
                }
                String lootId = c2.getString("loot_table", "default_loot");
                Location loc = new Location(w, x, y, z, yaw, pit);

                DungeonConfig.ChestLocation cl = new DungeonConfig.ChestLocation();
                cl.setLocation(loc);
                cl.setType(mat);
                cl.setLootTableId(lootId);
                chestList.add(cl);
            }
            dc.setChestLocations(chestList);
        }

        // portal
        if (config.contains("portalPos1")) {
            double px = config.getDouble("portalPos1.x");
            double py = config.getDouble("portalPos1.y");
            double pz = config.getDouble("portalPos1.z");
            float pyaw   = (float) config.getDouble("portalPos1.yaw", 0);
            float ppitch = (float) config.getDouble("portalPos1.pitch", 0);
            Location pLoc = new Location(w, px, py, pz, pyaw, ppitch);
            dc.setPortalPos1(pLoc);
        }

        // Doors
        if (config.contains("doors")) {
            ConfigurationSection doorsSec = config.getConfigurationSection("doors");
            List<DungeonConfig.DoorData> doorList = new ArrayList<>();
            for (String doorKey : doorsSec.getKeys(false)) {
                ConfigurationSection ds = doorsSec.getConfigurationSection(doorKey);
                DungeonConfig.DoorData dd = new DungeonConfig.DoorData();
                dd.setDoorId(ds.getString("id", "unknown"));
                dd.setX1(ds.getDouble("x1"));
                dd.setY1(ds.getDouble("y1"));
                dd.setZ1(ds.getDouble("z1"));
                dd.setX2(ds.getDouble("x2"));
                dd.setY2(ds.getDouble("y2"));
                dd.setZ2(ds.getDouble("z2"));
                dd.setTriggerType(ds.getString("trigger", "none"));
                doorList.add(dd);
            }
            dc.setDoors(doorList);
        }

        return dc;
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
        YamlConfiguration fileConfig = new YamlConfiguration();

        fileConfig.set("id", config.getId());
        fileConfig.set("name", config.getName());
        fileConfig.set("minPlayers", config.getMinPlayers());
        fileConfig.set("maxPlayers", config.getMaxPlayers());
        fileConfig.set("difficulty", config.getDifficulty());
        fileConfig.set("worldName", config.getWorldName());
        fileConfig.set("levelRequirement", config.getLevelRequirement());

        // spawn
        if (config.getSpawnLocation() != null) {
            fileConfig.set("spawnLocation.x", config.getSpawnLocation().getX());
            fileConfig.set("spawnLocation.y", config.getSpawnLocation().getY());
            fileConfig.set("spawnLocation.z", config.getSpawnLocation().getZ());
            fileConfig.set("spawnLocation.yaw", config.getSpawnLocation().getYaw());
            fileConfig.set("spawnLocation.pitch", config.getSpawnLocation().getPitch());
        }

        // mob spawns
        ConfigurationSection mobSec = fileConfig.createSection("mob_spawn_points");
        int mobIndex = 0;
        for (DungeonConfig.MobSpawnPoint sp : config.getMobSpawnPoints()) {
            ConfigurationSection msc = mobSec.createSection("spawn" + mobIndex++);
            msc.set("x", sp.getLocation().getX());
            msc.set("y", sp.getLocation().getY());
            msc.set("z", sp.getLocation().getZ());
            msc.set("yaw", sp.getLocation().getYaw());
            msc.set("pitch", sp.getLocation().getPitch());
            msc.set("mob_id", sp.getMobId());
        }

        // chests
        ConfigurationSection chestSec = fileConfig.createSection("chest_locations");
        int chestIndex = 0;
        for (DungeonConfig.ChestLocation cl : config.getChestLocations()) {
            ConfigurationSection csc = chestSec.createSection("chest" + chestIndex++);
            csc.set("x", cl.getLocation().getX());
            csc.set("y", cl.getLocation().getY());
            csc.set("z", cl.getLocation().getZ());
            csc.set("yaw", cl.getLocation().getYaw());
            csc.set("pitch", cl.getLocation().getPitch());
            csc.set("type", cl.getType().toString());
            csc.set("loot_table", cl.getLootTableId());
        }

        // portal
        if (config.getPortalPos1() != null) {
            fileConfig.set("portalPos1.x", config.getPortalPos1().getX());
            fileConfig.set("portalPos1.y", config.getPortalPos1().getY());
            fileConfig.set("portalPos1.z", config.getPortalPos1().getZ());
            fileConfig.set("portalPos1.yaw", config.getPortalPos1().getYaw());
            fileConfig.set("portalPos1.pitch", config.getPortalPos1().getPitch());
        }

        // doors
        ConfigurationSection doorsSec = fileConfig.createSection("doors");
        int doorIndex = 0;
        for (DungeonConfig.DoorData dd : config.getDoors()) {
            ConfigurationSection ds = doorsSec.createSection("door" + doorIndex++);
            ds.set("id", dd.getDoorId());
            ds.set("x1", dd.getX1());
            ds.set("y1", dd.getY1());
            ds.set("z1", dd.getZ1());
            ds.set("x2", dd.getX2());
            ds.set("y2", dd.getY2());
            ds.set("z2", dd.getZ2());
            ds.set("trigger", dd.getTriggerType());
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
        String lootTableId = "default_loot";
        if (chestType == Material.TRAPPED_CHEST) {
            lootTableId = "elite_loot";
        }

        if (lootTableManager.getLootTable(lootTableId) == null) {
            LootTable lt = new LootTable(lootTableId);
            if (lootTableId.equals("default_loot")) {
                lt.addItem("material", "DIAMOND", 1, 1.0);
            } else if (lootTableId.equals("elite_loot")) {
                lt.addItem("material", "NETHER_STAR", 1, 1.0);
            }
            lootTableManager.saveLootTable(lt);
        }
        return lootTableId;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }
}
