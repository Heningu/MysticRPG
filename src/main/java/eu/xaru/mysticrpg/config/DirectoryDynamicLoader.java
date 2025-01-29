package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.config.format.ConfigFormat;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads .yml, .yaml, .json, or .toml files in a subdirectory as DynamicConfigs
 * (with optional parse to T objects).
 */
public class DirectoryDynamicLoader {

    private final JavaPlugin plugin;

    public DirectoryDynamicLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all files in the given sub-directory that match .yml/.yaml/.json/.toml
     * and returns the resulting DynamicConfigs.
     */
    public List<DynamicConfig> loadConfigs(String subDirName) {
        List<DynamicConfig> result = new ArrayList<>();

        File subDir = new File(plugin.getDataFolder(), subDirName);
        if (!subDir.exists() && !subDir.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create subdir: " + subDirName);
            return result;
        }

        File[] files = subDir.listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".yml") || name.endsWith(".yaml")
                    || name.endsWith(".json") || name.endsWith(".toml")) {

                ConfigFormat format = ConfigFormat.YAML;
                if (name.endsWith(".json")) {
                    format = ConfigFormat.JSON;
                }
                // if .toml => you'd need a separate format or skip

                // We build a relative path to plugin folder
                String relativePath = subDirName + "/" + file.getName();
                DebugLogger.getInstance().log(Level.INFO,
                        "Loading DynamicConfig: " + relativePath, 0);

                // We can either call:
                //   DynamicConfig dcfg = DynamicConfigManager.loadConfig(relativePath, format);
                // Or the new overload with direct File usage:
                DynamicConfig dcfg = DynamicConfigManager.loadConfig(file, format);

                result.add(dcfg);
            }
        }

        return result;
    }

    /**
     * Loads and parses each config into T using the parser.
     * @return a List of T objects, ignoring any config that fails to parse.
     */
    public <T> List<T> loadAndParse(String subDirName, DynamicConfigParser<T> parser) {
        List<T> output = new ArrayList<>();
        List<DynamicConfig> cfgs = loadConfigs(subDirName);
        for (DynamicConfig dcfg : cfgs) {
            try {
                T obj = parser.parse(dcfg);
                if (obj != null) {
                    output.add(obj);
                }
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to parse " + dcfg.getFileName() + ":", e);
            }
        }
        return output;
    }
}
