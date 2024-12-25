package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.ConfigFormat;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages multiple DynamicConfig objects, each representing a separate file.
 */
public final class DynamicConfigManager {

    private static JavaPlugin plugin;
    private static final Map<String, DynamicConfig> configs = new HashMap<>();

    private DynamicConfigManager() {}

    public static void init(JavaPlugin plug) {
        plugin = plug;
    }

    /**
     * Loads (or reloads) the config with the given resourceName and userFileName,
     * defaulting to YAML format.
     *
     * @return the newly loaded DynamicConfig
     */
    public static DynamicConfig loadConfig(String resourceName, String userFileName) {
        return loadConfig(resourceName, userFileName, ConfigFormat.YAML);
    }

    /**
     * Overload that allows specifying config format (YAML, JSON, TOML).
     */
    public static DynamicConfig loadConfig(String resourceName, String userFileName, ConfigFormat format) {
        if (plugin == null) {
            throw new IllegalStateException("DynamicConfigManager not initialized. Call init() first!");
        }
        configs.remove(userFileName);

        File targetFile = new File(plugin.getDataFolder(), userFileName);
        DynamicConfig dcfg = new DynamicConfig(plugin, resourceName, targetFile, format);
        dcfg.reload();

        configs.put(userFileName, dcfg);
        return dcfg;
    }

    public static DynamicConfig getConfig(String userFileName) {
        return configs.get(userFileName);
    }

    /**
     * Saves all configs if needed (sync).
     */
    public static void saveAll() {
        for (DynamicConfig dc : configs.values()) {
            dc.saveIfNeeded();
        }
    }

    /**
     * Saves all configs asynchronously, returning a future that completes when done.
     */
    public static CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            for (DynamicConfig dc : configs.values()) {
                dc.saveIfNeeded();
            }
        });
    }

    // Provide convenience getters for anyone who wants them
    public static int getInt(String fileName, String path, int fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getInt at path=" + path);
            return fallback;
        }
        return cfg.getInt(path, fallback);
    }

    public static long getLong(String fileName, String path, long fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getLong at path=" + path);
            return fallback;
        }
        return cfg.getLong(path, fallback);
    }

    public static double getDouble(String fileName, String path, double fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getDouble at path=" + path);
            return fallback;
        }
        return cfg.getDouble(path, fallback);
    }

    public static boolean getBoolean(String fileName, String path, boolean fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getBoolean at path=" + path);
            return fallback;
        }
        return cfg.getBoolean(path, fallback);
    }

    public static String getString(String fileName, String path, String fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getString at path=" + path);
            return fallback;
        }
        return cfg.getString(path, fallback);
    }

    public static List<?> getList(String fileName, String path, List<?> fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getList at path=" + path);
            return fallback;
        }
        return cfg.getList(path, fallback);
    }

    public static List<String> getStringList(String fileName, String path, List<String> fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getStringList at path=" + path);
            return fallback;
        }
        return cfg.getStringList(path, fallback);
    }

    public static List<Map<?, ?>> getMapList(String fileName, String path, List<Map<?,?>> fallback) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg == null) {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't getMapList at path=" + path);
            return fallback;
        }
        return cfg.getMapList(path, fallback);
    }

    public static void set(String fileName, String path, Object value) {
        DynamicConfig cfg = configs.get(fileName);
        if (cfg != null) {
            cfg.set(path, value);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "Config '" + fileName + "' not loaded. Can't set path=" + path);
        }
    }
}
