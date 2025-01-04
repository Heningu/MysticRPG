package eu.xaru.mysticrpg.entityhandling;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of LinkedEntity objects to a single YAML file
 * at "plugins/MysticRPG/entities/entities.yml".
 *
 * Only those with isPersistent()==true are saved/loaded here.
 */
public class EntityStorage {

    private final JavaPlugin plugin;
    private final File entitiesFile;
    private final YamlConfiguration yaml;

    public EntityStorage(JavaPlugin plugin) {
        this.plugin = plugin;

        // e.g. plugins/MysticRPG/entities/entities.yml
        File entitiesFolder = new File(plugin.getDataFolder(), "entities");
        if (!entitiesFolder.exists()) {
            entitiesFolder.mkdirs();
        }
        this.entitiesFile = new File(entitiesFolder, "entities.yml");
        this.yaml = new YamlConfiguration();

        // Load from disk if it exists
        if (entitiesFile.exists()) {
            try {
                yaml.load(entitiesFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load entities.yml!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves all persistent LinkedEntities to entities.yml.
     */
    public void saveAll(List<LinkedEntity> allEntities) {
        // Clear existing entries in YAML
        for (String key : yaml.getKeys(false)) {
            yaml.set(key, null);
        }

        // Write each persistent entity
        for (LinkedEntity le : allEntities) {
            if (!le.isPersistent()) {
                continue; // skip ephemeral
            }
            String key = le.getEntityId();
            Location loc = le.getSpawnLocation();

            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x", loc.getX());
            yaml.set(key + ".y", loc.getY());
            yaml.set(key + ".z", loc.getZ());
            yaml.set(key + ".name", le.getDisplayName());
            yaml.set(key + ".modelId", le.getModelId());
        }

        try {
            yaml.save(entitiesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save entities.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Loads all persistent LinkedEntities from entities.yml.
     */
    public List<LinkedEntity> loadAll() {
        List<LinkedEntity> result = new ArrayList<>();
        for (String key : yaml.getKeys(false)) {
            String worldName = yaml.getString(key + ".world", "world");
            double x = yaml.getDouble(key + ".x", 0.0);
            double y = yaml.getDouble(key + ".y", 64.0);
            double z = yaml.getDouble(key + ".z", 0.0);

            String displayName = yaml.getString(key + ".name", key);
            String modelId = yaml.getString(key + ".modelId", "");

            // Reconstruct spawn location
            Location loc = null;
            if (Bukkit.getWorld(worldName) != null) {
                loc = new Location(Bukkit.getWorld(worldName), x, y, z);
            }

            // All these are "persistent"
            LinkedEntity le = new LinkedEntity(key, loc, displayName, modelId, true);
            result.add(le);
        }
        return result;
    }
}
