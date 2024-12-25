package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.ConfigFormat;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class DynamicConfigManager {

    private static JavaPlugin plugin;
    private static final Map<String, DynamicConfig> configs = new HashMap<>();

    private DynamicConfigManager() {}

    /**
     * Must be called once in onLoad() or onEnable().
     */
    public static void init(JavaPlugin plug) {
        plugin = plug;
    }

    /**
     * Loads (or reloads) a config at the given path (relative to plugin folder),
     * defaulting to YAML format. If the plugin has a resource with the same path,
     * merges it as defaults.
     *
     * @param path e.g. "config.yml" or "custom/items/myitem.json"
     */
    public static DynamicConfig loadConfig(String path) {
        return loadConfig(path, ConfigFormat.YAML);
    }

    /**
     * Loads (or reloads) a config at the given path with a specified format.
     * If the plugin has a resource with the same path, merges it as defaults.
     *
     * @param path   The relative path under /plugins/MyPlugin/
     * @param format The desired format (YAML, JSON)
     */
    public static DynamicConfig loadConfig(String path, ConfigFormat format) {
        if (plugin == null) {
            throw new IllegalStateException("DynamicConfigManager not initialized. Call init() first!");
        }
        configs.remove(path);

        DynamicConfig dcfg = new DynamicConfig(plugin, path, format);
        dcfg.reload();

        configs.put(path, dcfg);
        return dcfg;
    }

    /**
     * Retrieves the previously loaded config for 'path', or null if none loaded.
     */
    public static DynamicConfig getConfig(String path) {
        return configs.get(path);
    }

    /**
     * Saves all loaded configs if needed (sync).
     */
    public static void saveAll() {
        for (DynamicConfig dc : configs.values()) {
            dc.saveIfNeeded();
        }
    }

    /**
     * Saves all configs asynchronously.
     */
    public static CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            for (DynamicConfig dc : configs.values()) {
                dc.saveIfNeeded();
            }
        });
    }
}
