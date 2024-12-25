package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.ConfigFormat;
import eu.xaru.mysticrpg.config.format.IConfigBackend;
import eu.xaru.mysticrpg.config.format.JsonConfigBackend;
import eu.xaru.mysticrpg.config.format.YamlConfigBackend;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DynamicConfig {

    private final JavaPlugin plugin;
    private final String path;           // e.g. "config.yml" or "whatever.json"
    private final File fileOnDisk;       // /plugins/MyPlugin/<path>
    private final IConfigBackend backend;

    private Set<String> defaultKeys;
    private boolean changed;

    /**
     * Creates a DynamicConfig using a single path for both resource lookup and on-disk file.
     * If plugin.getResource(path) is non-null, that data is merged as defaults.
     */
    public DynamicConfig(JavaPlugin plugin, String path, ConfigFormat format) {
        this.plugin = plugin;
        this.path = path;
        this.fileOnDisk = new File(plugin.getDataFolder(), path);

        switch (format) {
            case YAML -> this.backend = new YamlConfigBackend();
            case JSON -> this.backend = new JsonConfigBackend();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    public void reload() {
        try {
            InputStream resourceIn = plugin.getResource(path);
            defaultKeys = backend.load(fileOnDisk, resourceIn);
            changed = false;
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to reload config: " + path, e);
        }
    }

    public void saveIfNeeded() {
        try {
            backend.save(fileOnDisk);
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to save config: " + path, e);
        }
    }

    private void handleMissingPath(String path, Object fallback) {
        DebugLogger.getInstance().warn("[DynamicConfig] Path '" + path + "' not found. Creating with default=" + fallback);
        if (defaultKeys != null && !defaultKeys.contains(path)) {
            DebugLogger.getInstance().warn("[DynamicConfig] Also, path '" + path + "' is NOT in resource '" + this.path + "'. " +
                    "Please add it to your default .yml if it's permanent!");
        }
        backend.set(path, fallback);
        changed = true;
    }

    // Basic getters

    public int getInt(String node, int fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return fallback;
    }

    public long getLong(String node, long fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return fallback;
    }

    public double getDouble(String node, double fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return fallback;
    }

    public boolean getBoolean(String node, boolean fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return fallback;
    }

    public String getString(String node, String fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val != null) {
            return String.valueOf(val);
        }
        return fallback;
    }

    public List<?> getList(String node, List<?> fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof List<?>) {
            return (List<?>) val;
        }
        return fallback;
    }

    public List<String> getStringList(String node, List<String> fallback) {
        List<?> list = getList(node, fallback);
        List<String> strList = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                strList.add(String.valueOf(o));
            }
        }
        return strList.isEmpty() ? fallback : strList;
    }

    public List<Map<?,?>> getMapList(String node, List<Map<?,?>> fallback) {
        if (!backend.contains(node)) {
            handleMissingPath(node, fallback);
        }
        Object val = backend.get(node);
        if (val instanceof List<?> rawList) {
            List<Map<?,?>> newList = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?,?> m) {
                    newList.add(m);
                }
            }
            return newList;
        }
        return fallback;
    }

    public Object get(String node) {
        return backend.get(node);
    }

    public boolean contains(String node) {
        return backend.contains(node);
    }

    public void set(String node, Object value) {
        backend.set(node, value);
        changed = true;
    }

    public Set<String> getKeys(boolean deep) {
        return backend.getKeys(deep);
    }

    public String getFileName() {
        return fileOnDisk.getName();
    }

    public String getFileNameWithoutExtension() {
        String name = fileOnDisk.getName();
        int idx = name.lastIndexOf(".");
        if (idx == -1) return name;
        return name.substring(0, idx);
    }

    public FileConfigurationOptions getOptions() {
        return backend.getOptions(); // May be null if JSON
    }

    public boolean isChanged() {
        return changed;
    }
}
