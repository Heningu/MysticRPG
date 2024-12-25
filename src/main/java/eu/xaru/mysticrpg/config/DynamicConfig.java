package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.*;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DynamicConfig {

    private final JavaPlugin plugin;
    private final String resourcePath;  // e.g. "config.yml"
    private final File fileOnDisk;      // e.g. /plugins/MyPlugin/config.yml

    private final IConfigBackend backend;
    private Set<String> defaultKeys;
    private boolean changed;

    /**
     * Creates a DynamicConfig using the specified backend format
     * (YAML, JSON, TOML).
     *
     * @param plugin       Owning plugin
     * @param resourcePath The resource in the JAR (may be null if none)
     * @param fileOnDisk   The user file to read/write
     * @param format       The desired config format
     */
    public DynamicConfig(JavaPlugin plugin,
                         String resourcePath,
                         File fileOnDisk,
                         ConfigFormat format) {
        this.plugin = plugin;
        this.resourcePath = resourcePath;
        this.fileOnDisk = fileOnDisk;

        switch (format) {
            case YAML -> this.backend = new YamlConfigBackend();
            case JSON -> this.backend = new JsonConfigBackend();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    public void reload() {
        try {
            InputStream resourceIn = null;
            if (resourcePath != null) {
                resourceIn = plugin.getResource(resourcePath);
            }
            defaultKeys = backend.load(fileOnDisk, resourceIn);
            changed = false;
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to reload config: " + fileOnDisk.getName(), e);
        }
    }

    public void saveIfNeeded() {
        try {
            backend.save(fileOnDisk);
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to save config: " + fileOnDisk.getName(), e);
        }
    }

    private void handleMissingPath(String path, Object fallback) {
        DebugLogger.getInstance().warn("[DynamicConfig] Path '" + path + "' not found. Creating with default=" + fallback);
        if (defaultKeys != null && !defaultKeys.contains(path)) {
            DebugLogger.getInstance().warn("[DynamicConfig] Also, path '" + path + "' is NOT in resource '" + resourcePath + "'. " +
                    "Please add it to your default .yml if it's permanent!");
        }
        backend.set(path, fallback);
        changed = true;
    }

    // Basic getters

    public int getInt(String path, int fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return fallback;
    }

    public long getLong(String path, long fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return fallback;
    }

    public double getDouble(String path, double fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return fallback;
    }

    public boolean getBoolean(String path, boolean fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return fallback;
    }

    public String getString(String path, String fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val != null) {
            return String.valueOf(val);
        }
        return fallback;
    }

    public List<?> getList(String path, List<?> fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
        if (val instanceof List<?>) {
            return (List<?>)val;
        }
        return fallback;
    }

    public List<String> getStringList(String path, List<String> fallback) {
        List<?> list = getList(path, fallback);
        List<String> strList = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                strList.add(String.valueOf(o));
            }
        }
        return strList.isEmpty() ? fallback : strList;
    }

    public List<Map<?,?>> getMapList(String path, List<Map<?,?>> fallback) {
        if (!backend.contains(path)) {
            handleMissingPath(path, fallback);
        }
        Object val = backend.get(path);
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

    public Object get(String path) {
        return backend.get(path);
    }

    public boolean contains(String path) {
        return backend.contains(path);
    }

    public void set(String path, Object value) {
        backend.set(path, value);
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
        return backend.getOptions(); // May be null if not YAML
    }

    public boolean isChanged() {
        return false;
    }

}
