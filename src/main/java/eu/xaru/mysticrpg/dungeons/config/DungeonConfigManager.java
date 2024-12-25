package eu.xaru.mysticrpg.dungeons.config;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.dungeons.loot.LootTable;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
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

        // Only .yml files, but we’re using your new config system
        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (configFiles == null) return;

        for (File file : configFiles) {
            // "userFileName" for DynamicConfigManager is "dungeons/<filename>"
            String userFileName = "dungeons/" + file.getName();
            DynamicConfigManager.loadConfig(userFileName, userFileName);
            // Then retrieve by the exact same key:
            DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
            if (config == null) {
                continue;
            }

            DungeonConfig dungeonConfig = parseConfig(config);
            if (dungeonConfig != null) {
                dungeonConfigs.put(dungeonConfig.getId(), dungeonConfig);
                DebugLogger.getInstance().log(Level.INFO, "Loaded dungeon config: " + dungeonConfig.getId(), 0);
            }
        }
    }

    private DungeonConfig parseConfig(DynamicConfig config) {
        DungeonConfig dc = new DungeonConfig();
        dc.setId(config.getString("id", null));
        dc.setName(config.getString("name", "Unknown"));
        dc.setMinPlayers(config.getInt("minPlayers", 1));
        dc.setMaxPlayers(config.getInt("maxPlayers", 5));
        dc.setDifficulty(config.getString("difficulty", "Normal"));
        dc.setWorldName(config.getString("worldName", null));
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
            double sx = config.getDouble("spawnLocation.x", 0);
            double sy = config.getDouble("spawnLocation.y", 0);
            double sz = config.getDouble("spawnLocation.z", 0);
            float syaw   = (float) config.getDouble("spawnLocation.yaw", 0);
            float spitch = (float) config.getDouble("spawnLocation.pitch", 0);
            dc.setSpawnLocation(new Location(w, sx, sy, sz, syaw, spitch));
        }

        // mob spawn points
        if (config.contains("mob_spawn_points")) {
            Object mobObj = config.get("mob_spawn_points");
            if (mobObj instanceof Map<?,?> mobMap) {
                List<DungeonConfig.MobSpawnPoint> mobList = new ArrayList<>();
                for (Map.Entry<?, ?> e : mobMap.entrySet()) {
                    if (e.getValue() instanceof Map<?,?> ms) {
                        double x = parseDouble(ms.get("x"), 0);
                        double y = parseDouble(ms.get("y"), 0);
                        double z = parseDouble(ms.get("z"), 0);
                        float yaw = (float) parseDouble(ms.get("yaw"), 0);
                        float pit = (float) parseDouble(ms.get("pitch"), 0);
                        String mobId = parseString(ms.get("mob_id"), "zombie");

                        Location loc = new Location(w, x, y, z, yaw, pit);
                        DungeonConfig.MobSpawnPoint mp = new DungeonConfig.MobSpawnPoint();
                        mp.setLocation(loc);
                        mp.setMobId(mobId);
                        mobList.add(mp);
                    }
                }
                dc.setMobSpawnPoints(mobList);
            }
        }

        // chest locations
        if (config.contains("chest_locations")) {
            Object chestObj = config.get("chest_locations");
            if (chestObj instanceof Map<?,?> chestMap) {
                List<DungeonConfig.ChestLocation> chestList = new ArrayList<>();
                for (Map.Entry<?,?> e : chestMap.entrySet()) {
                    if (e.getValue() instanceof Map<?,?> c2) {
                        double x = parseDouble(c2.get("x"), 0);
                        double y = parseDouble(c2.get("y"), 0);
                        double z = parseDouble(c2.get("z"), 0);
                        float yaw = (float) parseDouble(c2.get("yaw"), 0);
                        float pit = (float) parseDouble(c2.get("pitch"), 0);
                        String typeStr = parseString(c2.get("type"), "CHEST");
                        Material mat = Material.matchMaterial(typeStr.toUpperCase());
                        if (mat == null) {
                            mat = Material.CHEST;
                        }
                        String lootId = parseString(c2.get("loot_table"), "default_loot");

                        Location loc = new Location(w, x, y, z, yaw, pit);
                        DungeonConfig.ChestLocation cl = new DungeonConfig.ChestLocation();
                        cl.setLocation(loc);
                        cl.setType(mat);
                        cl.setLootTableId(lootId);
                        chestList.add(cl);
                    }
                }
                dc.setChestLocations(chestList);
            }
        }

        // portal
        if (config.contains("portalPos1")) {
            double px = config.getDouble("portalPos1.x", 0);
            double py = config.getDouble("portalPos1.y", 0);
            double pz = config.getDouble("portalPos1.z", 0);
            float pyaw   = (float) config.getDouble("portalPos1.yaw", 0);
            float ppitch = (float) config.getDouble("portalPos1.pitch", 0);
            dc.setPortalPos1(new Location(w, px, py, pz, pyaw, ppitch));
        }

        // Doors
        if (config.contains("doors")) {
            Object doorsObj = config.get("doors");
            if (doorsObj instanceof Map<?,?> doorsMap) {
                List<DungeonConfig.DoorData> doorList = new ArrayList<>();
                for (Map.Entry<?,?> e : doorsMap.entrySet()) {
                    if (e.getValue() instanceof Map<?,?> ds) {
                        DungeonConfig.DoorData dd = new DungeonConfig.DoorData();
                        dd.setDoorId(parseString(ds.get("id"), "unknown"));
                        dd.setX1(parseDouble(ds.get("x1"), 0));
                        dd.setY1(parseDouble(ds.get("y1"), 0));
                        dd.setZ1(parseDouble(ds.get("z1"), 0));
                        dd.setX2(parseDouble(ds.get("x2"), 0));
                        dd.setY2(parseDouble(ds.get("y2"), 0));
                        dd.setZ2(parseDouble(ds.get("z2"), 0));
                        dd.setTriggerType(parseString(ds.get("trigger"), "none"));

                        // NEW: load keyItemId from config
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
     * using DynamicConfig. Overwrites any existing config with the same ID.
     */
    public void saveDungeonConfig(DungeonConfig config) {
        File configDir = new File(plugin.getDataFolder(), "dungeons");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // We'll store it under "dungeons/<id>.yml"
        String userFileName = "dungeons/" + config.getId() + ".yml";
        // Force a reload to have a fresh config object
        DynamicConfigManager.loadConfig(userFileName, userFileName);
        DynamicConfig dcfg = DynamicConfigManager.getConfig(userFileName);
        if (dcfg == null) {
            // fallback
            DebugLogger.getInstance().log(Level.SEVERE, "Could not create dynamic config for: " + userFileName, 0);
            return;
        }

        // Top-level fields
        dcfg.set("id", config.getId());
        dcfg.set("name", config.getName());
        dcfg.set("minPlayers", config.getMinPlayers());
        dcfg.set("maxPlayers", config.getMaxPlayers());
        dcfg.set("difficulty", config.getDifficulty());
        dcfg.set("worldName", config.getWorldName());
        dcfg.set("levelRequirement", config.getLevelRequirement());

        // spawn
        if (config.getSpawnLocation() != null) {
            Location sloc = config.getSpawnLocation();
            dcfg.set("spawnLocation.x", sloc.getX());
            dcfg.set("spawnLocation.y", sloc.getY());
            dcfg.set("spawnLocation.z", sloc.getZ());
            dcfg.set("spawnLocation.yaw", sloc.getYaw());
            dcfg.set("spawnLocation.pitch", sloc.getPitch());
        }

        // mob spawns (store as a map of sub-maps)
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
        dcfg.set("mob_spawn_points", mobSec);

        // chest locations
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
        dcfg.set("chest_locations", chestSec);

        // portal
        if (config.getPortalPos1() != null) {
            Location pLoc = config.getPortalPos1();
            dcfg.set("portalPos1.x", pLoc.getX());
            dcfg.set("portalPos1.y", pLoc.getY());
            dcfg.set("portalPos1.z", pLoc.getZ());
            dcfg.set("portalPos1.yaw", pLoc.getYaw());
            dcfg.set("portalPos1.pitch", pLoc.getPitch());
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
            // IMPORTANT: Save the keyItemId
            ds.put("keyItemId", dd.getKeyItemId());

            doorsSec.put("door" + doorIndex++, ds);
        }
        dcfg.set("doors", doorsSec);

        // Finally, write to disk
        try {
            dcfg.saveIfNeeded(); // writes if changed
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

    // Helper for safely reading a double
    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    // Helper for safely reading a string
    private String parseString(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }
}
