package eu.xaru.mysticrpg.dungeons.config;

import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles reading and writing of .yml configs in /plugins/MyPlugin/dungeons/,
 * using the built-in Bukkit/Spigot YamlConfiguration directly.
 */
public class DungeonConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, DungeonConfig> dungeonConfigs;
    private final LootTableManager lootTableManager;

    public DungeonConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dungeonConfigs = new HashMap<>();
        this.lootTableManager = new LootTableManager(plugin);
    }

    /**
     * Loads all *.yml files in "plugins/MyPlugin/dungeons" into memory,
     * parsing each into a DungeonConfig object.
     */
    public void loadConfigs() {
        File configDir = new File(plugin.getDataFolder(), "dungeons");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File[] configFiles = configDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (configFiles == null) return;

        for (File file : configFiles) {
            YamlConfiguration ycfg = new YamlConfiguration();
            try {
                ycfg.load(file);
            } catch (Exception e) {
                DebugLogger.getInstance().log(Level.SEVERE,
                        "Failed to load dungeon config: " + file.getName(), e);
                continue;
            }

            // Convert the YAML data into a DungeonConfig object
            DungeonConfig dungeonConfig = parseConfig(ycfg);
            if (dungeonConfig != null) {
                // Optionally store the file name or path if you need it
                dungeonConfigs.put(dungeonConfig.getId(), dungeonConfig);
                DebugLogger.getInstance().log(Level.INFO,
                        "Loaded dungeon config: " + dungeonConfig.getId(), 0);
            }
        }
    }

    /**
     * Reads fields from YamlConfiguration and returns a DungeonConfig.
     * Returns null if critical data (e.g., "id", "worldName") is missing or invalid.
     */
    private DungeonConfig parseConfig(YamlConfiguration ycfg) {
        DungeonConfig dc = new DungeonConfig();
        dc.setId(ycfg.getString("id", null));
        dc.setName(ycfg.getString("name", "Unknown"));
        dc.setMinPlayers(ycfg.getInt("minPlayers", 1));
        dc.setMaxPlayers(ycfg.getInt("maxPlayers", 5));
        dc.setDifficulty(ycfg.getString("difficulty", "Normal"));
        dc.setWorldName(ycfg.getString("worldName", null));
        dc.setLevelRequirement(ycfg.getInt("levelRequirement", 1));

        if (dc.getId() == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Dungeon config missing 'id'. Skipping...", 0);
            return null;
        }
        if (dc.getWorldName() == null) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Dungeon '" + dc.getId() + "' missing 'worldName'. Skipping...", 0);
            return null;
        }

        // Attempt to load or create the world
        World w = Bukkit.getWorld(dc.getWorldName());
        if (w == null) {
            w = new WorldCreator(dc.getWorldName()).createWorld();
            if (w == null) {
                DebugLogger.getInstance().log(Level.SEVERE,
                        "Dungeon '" + dc.getId() + "': could not load/create world '"
                                + dc.getWorldName() + "'.", 0);
                return null;
            }
        }

        // spawnLocation
        if (ycfg.contains("spawnLocation")) {
            double sx = ycfg.getDouble("spawnLocation.x", 0);
            double sy = ycfg.getDouble("spawnLocation.y", 0);
            double sz = ycfg.getDouble("spawnLocation.z", 0);
            float syaw   = (float) ycfg.getDouble("spawnLocation.yaw", 0);
            float spitch = (float) ycfg.getDouble("spawnLocation.pitch", 0);
            dc.setSpawnLocation(new Location(w, sx, sy, sz, syaw, spitch));
        }

        // mob_spawn_points => stored as a Map
        if (ycfg.contains("mob_spawn_points")) {
            Object mobObj = ycfg.get("mob_spawn_points");
            if (mobObj instanceof Map<?, ?> mobMap) {
                List<DungeonConfig.MobSpawnPoint> mobList = new ArrayList<>();
                for (Map.Entry<?, ?> e : mobMap.entrySet()) {
                    if (e.getValue() instanceof Map<?, ?> ms) {
                        double x = parseDouble(ms.get("x"), 0);
                        double y = parseDouble(ms.get("y"), 0);
                        double z = parseDouble(ms.get("z"), 0);
                        float yaw = (float) parseDouble(ms.get("yaw"), 0);
                        float pitch = (float) parseDouble(ms.get("pitch"), 0);
                        String mobId = parseString(ms.get("mob_id"), "zombie");

                        DungeonConfig.MobSpawnPoint mp = new DungeonConfig.MobSpawnPoint();
                        mp.setLocation(new Location(w, x, y, z, yaw, pitch));
                        mp.setMobId(mobId);

                        mobList.add(mp);
                    }
                }
                dc.setMobSpawnPoints(mobList);
            }
        }

        // chest_locations => similar approach
        if (ycfg.contains("chest_locations")) {
            Object chestObj = ycfg.get("chest_locations");
            if (chestObj instanceof Map<?, ?> chestMap) {
                List<DungeonConfig.ChestLocation> chestList = new ArrayList<>();
                for (Map.Entry<?, ?> e : chestMap.entrySet()) {
                    if (e.getValue() instanceof Map<?, ?> c2) {
                        double x = parseDouble(c2.get("x"), 0);
                        double y = parseDouble(c2.get("y"), 0);
                        double z = parseDouble(c2.get("z"), 0);
                        float yaw = (float) parseDouble(c2.get("yaw"), 0);
                        float pitch = (float) parseDouble(c2.get("pitch"), 0);
                        String typeStr = parseString(c2.get("type"), "CHEST").toUpperCase();
                        Material mat = Material.matchMaterial(typeStr);
                        if (mat == null) {
                            mat = Material.CHEST;
                        }
                        String lootId = parseString(c2.get("loot_table"), "default_loot");

                        DungeonConfig.ChestLocation cl = new DungeonConfig.ChestLocation();
                        cl.setLocation(new Location(w, x, y, z, yaw, pitch));
                        cl.setType(mat);
                        cl.setLootTableId(lootId);
                        chestList.add(cl);
                    }
                }
                dc.setChestLocations(chestList);
            }
        }

        // portalPos1
        if (ycfg.contains("portalPos1")) {
            double px = ycfg.getDouble("portalPos1.x", 0);
            double py = ycfg.getDouble("portalPos1.y", 0);
            double pz = ycfg.getDouble("portalPos1.z", 0);
            float pyaw   = (float) ycfg.getDouble("portalPos1.yaw", 0);
            float ppitch = (float) ycfg.getDouble("portalPos1.pitch", 0);
            dc.setPortalPos1(new Location(w, px, py, pz, pyaw, ppitch));
        }

        // doors => Map-based
        if (ycfg.contains("doors")) {
            Object doorsObj = ycfg.get("doors");
            if (doorsObj instanceof Map<?, ?> doorsMap) {
                List<DungeonConfig.DoorData> doorList = new ArrayList<>();
                for (Map.Entry<?, ?> e : doorsMap.entrySet()) {
                    if (e.getValue() instanceof Map<?, ?> ds) {
                        DungeonConfig.DoorData dd = new DungeonConfig.DoorData();
                        dd.setDoorId(parseString(ds.get("id"), "unknown"));
                        dd.setX1(parseDouble(ds.get("x1"), 0));
                        dd.setY1(parseDouble(ds.get("y1"), 0));
                        dd.setZ1(parseDouble(ds.get("z1"), 0));
                        dd.setX2(parseDouble(ds.get("x2"), 0));
                        dd.setY2(parseDouble(ds.get("y2"), 0));
                        dd.setZ2(parseDouble(ds.get("z2"), 0));
                        dd.setTriggerType(parseString(ds.get("trigger"), "none"));
                        dd.setKeyItemId(parseString(ds.get("keyItemId"), null));
                        doorList.add(dd);
                    }
                }
                dc.setDoors(doorList);
            }
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

    /**
     * Saves the given DungeonConfig as a .yml in "dungeons/<id>.yml"
     * using YamlConfiguration, overwriting any existing config with the same ID.
     */
    public void saveDungeonConfig(DungeonConfig config) {
        File configDir = new File(plugin.getDataFolder(), "dungeons");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, config.getId() + ".yml");
        YamlConfiguration ycfg = new YamlConfiguration();

        ycfg.set("id", config.getId());
        ycfg.set("name", config.getName());
        ycfg.set("minPlayers", config.getMinPlayers());
        ycfg.set("maxPlayers", config.getMaxPlayers());
        ycfg.set("difficulty", config.getDifficulty());
        ycfg.set("worldName", config.getWorldName());
        ycfg.set("levelRequirement", config.getLevelRequirement());

        if (config.getSpawnLocation() != null) {
            Location sloc = config.getSpawnLocation();
            ycfg.set("spawnLocation.x", sloc.getX());
            ycfg.set("spawnLocation.y", sloc.getY());
            ycfg.set("spawnLocation.z", sloc.getZ());
            ycfg.set("spawnLocation.yaw", sloc.getYaw());
            ycfg.set("spawnLocation.pitch", sloc.getPitch());
        }

        // mob_spawn_points => store as Map
        Map<String, Object> mobSec = new LinkedHashMap<>();
        int mobIndex = 0;
        for (DungeonConfig.MobSpawnPoint sp : config.getMobSpawnPoints()) {
            Map<String, Object> spawnSec = new LinkedHashMap<>();
            spawnSec.put("x", sp.getLocation().getX());
            spawnSec.put("y", sp.getLocation().getY());
            spawnSec.put("z", sp.getLocation().getZ());
            spawnSec.put("yaw", sp.getLocation().getYaw());
            spawnSec.put("pitch", sp.getLocation().getPitch());
            spawnSec.put("mob_id", sp.getMobId());
            mobSec.put("spawn" + mobIndex++, spawnSec);
        }
        ycfg.set("mob_spawn_points", mobSec);

        // chest_locations
        Map<String, Object> chestSec = new LinkedHashMap<>();
        int chestIndex = 0;
        for (DungeonConfig.ChestLocation cl : config.getChestLocations()) {
            Map<String, Object> csc = new LinkedHashMap<>();
            csc.put("x", cl.getLocation().getX());
            csc.put("y", cl.getLocation().getY());
            csc.put("z", cl.getLocation().getZ());
            csc.put("yaw", cl.getLocation().getYaw());
            csc.put("pitch", cl.getLocation().getPitch());
            csc.put("type", cl.getType().toString());
            csc.put("loot_table", cl.getLootTableId());
            chestSec.put("chest" + chestIndex++, csc);
        }
        ycfg.set("chest_locations", chestSec);

        // portalPos1
        if (config.getPortalPos1() != null) {
            Location pLoc = config.getPortalPos1();
            ycfg.set("portalPos1.x", pLoc.getX());
            ycfg.set("portalPos1.y", pLoc.getY());
            ycfg.set("portalPos1.z", pLoc.getZ());
            ycfg.set("portalPos1.yaw", pLoc.getYaw());
            ycfg.set("portalPos1.pitch", pLoc.getPitch());
        }

        // doors
        Map<String, Object> doorsSec = new LinkedHashMap<>();
        int doorIndex = 0;
        for (DungeonConfig.DoorData dd : config.getDoors()) {
            Map<String, Object> ds = new LinkedHashMap<>();
            ds.put("id", dd.getDoorId());
            ds.put("x1", dd.getX1());
            ds.put("y1", dd.getY1());
            ds.put("z1", dd.getZ1());
            ds.put("x2", dd.getX2());
            ds.put("y2", dd.getY2());
            ds.put("z2", dd.getZ2());
            ds.put("trigger", dd.getTriggerType());
            ds.put("keyItemId", dd.getKeyItemId());
            doorsSec.put("door" + doorIndex++, ds);
        }
        ycfg.set("doors", doorsSec);

        try {
            ycfg.save(file);
            DebugLogger.getInstance().log(Level.INFO,
                    "Dungeon config saved: " + config.getId(), 0);
            // Keep it in memory too
            dungeonConfigs.put(config.getId(), config);
        } catch (IOException e) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Failed to save dungeon config '" + config.getId() + "':", e, 0);
        }
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }

    // -----------------------
    // Helper parse methods
    // -----------------------
    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String parseString(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }
}
