package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.ConfigFormat;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages creation, storage, and saving of DynamicConfigs.
 * Typically, call init() once in onEnable, then load or reload your configs.
 */
public final class DynamicConfigManager {

    private static JavaPlugin plugin;
    private static final Map<String, DynamicConfig> configs = new HashMap<>();
    // Alternatively, if using File-based keys, we might keep a second map. We'll keep it simple.

    private DynamicConfigManager() {
    }

    /**
     * Must be called once in onLoad() or onEnable().
     */
    public static void init(JavaPlugin plug) {
        plugin = plug;
    }

    /**
     * Loads (or reloads) a config at the given relative path (relative to plugin folder),
     * defaulting to YAML format. If the plugin has a resource with the same path,
     * merges it as defaults.
     *
     * @param path e.g. "config.yml" or "custom/items/myitem.json"
     */
    public static DynamicConfig loadConfig(String path) {
        return loadConfig(path, ConfigFormat.YAML);
    }

    public static DynamicConfig loadConfig(File file) {
        return loadConfig(file, ConfigFormat.YAML);
    }

    /**
     * Loads (or reloads) a config at the given relative path with a specified format.
     * If the plugin has a resource with the same path, merges it as defaults.
     *
     * @param path   The relative path under /plugins/MyPlugin/
     * @param format The desired format (YAML, JSON)
     */
    public static DynamicConfig loadConfig(String path, ConfigFormat format) {
        if (plugin == null) {
            throw new IllegalStateException("DynamicConfigManager not initialized. Call init() first!");
        }
        // Remove old config from cache
        configs.remove(path);

        DynamicConfig dcfg = new DynamicConfig(plugin, path, format);
        dcfg.reload();

        // Add back
        configs.put(path, dcfg);
        return dcfg;
    }


    /**
     * Overloaded method: Loads (or reloads) a config from the specified File (may be absolute),
     * with a specified format. Merges defaults if the plugin jar has a resource matching file.getName().
     *
     * @param file   The File pointing to config.
     * @param format The desired format (YAML, JSON)
     */
    public static DynamicConfig loadConfig(File file, ConfigFormat format) {
        if (plugin == null) {
            throw new IllegalStateException("DynamicConfigManager not initialized. Call init() first!");
        }

        String key = file.getAbsolutePath();
        configs.remove(key);

        DynamicConfig dcfg = new DynamicConfig(plugin, file, format);
        dcfg.reload();

        configs.put(key, dcfg);
        return dcfg;
    }

    /**
     * Retrieves the previously loaded config for 'path' (relative), or null if none loaded.
     * This only works if you used the path-based load methods.
     */
    public static DynamicConfig getConfig(String path) {
        return configs.get(path);
    }

    /**
     * Retrieves the previously loaded config for the given File, or null if none loaded.
     */
    public static DynamicConfig getConfig(File file) {
        return configs.get(file.getAbsolutePath());
    }

    /**
     * Saves all loaded configs if needed (synchronously).
     * Typically called in onDisable().
     */
    public static void saveAll() {
        for (DynamicConfig dc : configs.values()) {
            dc.saveIfNeeded();
        }
    }

    /**
     * Saves all configs asynchronously, in a separate thread.
     * Be aware of concurrency if you are also reloading or modifying config on the main thread.
     */
    public static CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            for (DynamicConfig dc : configs.values()) {
                dc.saveIfNeeded();
            }
        });
    }
}
