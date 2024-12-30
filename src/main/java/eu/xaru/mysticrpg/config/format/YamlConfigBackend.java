package eu.xaru.mysticrpg.config.format;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

/**
 * YAML-based backend using Bukkit's YamlConfiguration.
 */
public class YamlConfigBackend implements IConfigBackend {

    private FileConfiguration config;
    private boolean changed;
    private Set<String> defaultKeys;

    @Override
    public Set<String> load(File file, InputStream resourceIn) throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            boolean created = file.createNewFile();
            if (!created) {
                DebugLogger.getInstance().warn("Failed to create new YAML config file: " + file.getName());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        if (resourceIn != null) {
            // Merge defaults from jar resource
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceIn));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
            defaultKeys = defConfig.getKeys(true);
        } else {
            defaultKeys = Collections.emptySet();
        }

        // Save once to ensure creation/merging
        config.save(file);
        changed = false;
        return defaultKeys;
    }

    @Override
    public void save(File file) throws IOException {
        if (changed) {
            config.save(file);
            changed = false;
        }
    }

    @Override
    public boolean contains(String path) {
        if (config == null) {
            return false;
        }
        return config.contains(path);
    }

    @Override
    public Object get(String path) {
        if (config == null) {
            return null;
        }
        return config.get(path);
    }

    @Override
    public void set(String path, Object value) {
        if (config == null) {
            return;
        }
        config.set(path, value);
        changed = true;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (config == null) {
            return Collections.emptySet();
        }
        return config.getKeys(deep);
    }

    @Override
    public FileConfigurationOptions getOptions() {
        return (config != null) ? config.options() : null;
    }
}
