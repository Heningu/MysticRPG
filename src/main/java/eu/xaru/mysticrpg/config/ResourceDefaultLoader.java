package eu.xaru.mysticrpg.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Merges resource .yml defaults into a user FileConfiguration,
 * returning the set of known default keys from the resource.
 */
public final class ResourceDefaultLoader {

    private ResourceDefaultLoader() {
        // Utility class
    }

    /**
     * Loads defaults from the given InputStream (the .yml in your jar resources)
     * and merges them into the userConfig. Then returns all default keys found in the resource.
     *
     * @param userConfig The FileConfiguration from the user's config file.
     * @param resourceIn The resource InputStream from the plugin jar.
     * @return A set of all keys present in the resource's default YML.
     */
    public static Set<String> mergeDefaults(FileConfiguration userConfig, InputStream resourceIn) {
        FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceIn));
        userConfig.setDefaults(defConfig);
        userConfig.options().copyDefaults(true);
        return defConfig.getKeys(true);
    }
}
