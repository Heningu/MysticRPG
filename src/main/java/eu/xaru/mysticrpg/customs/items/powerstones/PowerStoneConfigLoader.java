package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.customs.items.Rarity;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Loads a PowerStone's configuration from a YAML file using Bukkit's YamlConfiguration.
 */
public class PowerStoneConfigLoader {

    private final File configFile;

    public PowerStoneConfigLoader(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Loads a PowerStone object from the specified configuration file.
     *
     * @return The loaded PowerStone
     * @throws Exception if any required fields are missing or invalid
     */
    public PowerStone loadPowerStone() throws Exception {
        // 1) Create a YamlConfiguration and load the file
        YamlConfiguration ycfg = new YamlConfiguration();
        ycfg.load(configFile);

        // 2) Retrieve values from ycfg, with fallback or exceptions if missing
        String id = ycfg.getString("id", "");
        if (id.isEmpty()) {
            throw new IllegalArgumentException(
                    "PowerStone ID is missing in file: " + configFile.getName());
        }

        String name = ycfg.getString("name", "Power Stone");
        String materialName = ycfg.getString("material", "STONE").toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material '" + materialName
                    + "' for power stone '" + id + "' in file: " + configFile.getName());
        }

        int customModelData = ycfg.getInt("custom_model_data", 0);
        String description = ycfg.getString("description", "");

        String effectName = ycfg.getString("effect", "");
        if (effectName.isEmpty()) {
            throw new IllegalArgumentException("Effect is missing for power stone '" + id
                    + "' in file: " + configFile.getName());
        }

        String applicableTo = ycfg.getString("applicable_to", "ALL");

        String rarityName = ycfg.getString("rarity", "COMMON").toUpperCase();
        Rarity rarity;
        try {
            rarity = Rarity.valueOf(rarityName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid rarity '" + rarityName
                    + "' for power stone '" + id + "'.", e);
        }

        // 3) Create and return the PowerStone
        return new PowerStone(id, name, material, customModelData, description,
                effectName, applicableTo, rarity);
    }
}
