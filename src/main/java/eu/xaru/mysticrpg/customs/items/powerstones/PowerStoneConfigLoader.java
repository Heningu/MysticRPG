package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.customs.items.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.File;

/**
 * Loads a PowerStone's configuration from a YAML file using the DynamicConfig system.
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
        // 1) Convert this file path into something recognized by DynamicConfigManager
        //    For example: "customs/items/powerstones/<filename>.yml"
        //    We'll reuse configFile.getName() to keep the same name.
        String userFileName = "custom/items/powerstones/" + configFile.getName();

        // 2) Use the manager to load the config (resourceName == userFileName for simplicity)
        DynamicConfigManager.loadConfig(userFileName);

        // 3) Retrieve the DynamicConfig
        DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
        if (config == null) {
            throw new IllegalStateException("Failed to retrieve DynamicConfig for: " + userFileName);
        }

        // 4) Now retrieve values from the DynamicConfig
        //    (We supply fallback values or raise exceptions if missing, as needed)

        String id = config.getString("id", "");
        if (id.isEmpty()) {
            throw new IllegalArgumentException(
                    "PowerStone ID is missing in file: " + configFile.getName());
        }

        String name = config.getString("name", "Power Stone");
        String materialName = config.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            throw new IllegalArgumentException("Invalid material for power stone '" + id
                    + "' in file: " + configFile.getName());
        }

        int customModelData = config.getInt("custom_model_data", 0);
        String description = config.getString("description", "");

        String effectName = config.getString("effect", "");
        if (effectName.isEmpty()) {
            throw new IllegalArgumentException("Effect is missing for power stone '" + id
                    + "' in file: " + configFile.getName());
        }

        String applicableTo = config.getString("applicable_to", "ALL");

        String rarityName = config.getString("rarity", "COMMON").toUpperCase();
        Rarity rarity;
        try {
            rarity = Rarity.valueOf(rarityName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid rarity '" + rarityName
                    + "' for power stone '" + id + "'.", e);
        }

        // 5) Create and return the PowerStone
        return new PowerStone(id, name, material, customModelData, description,
                effectName, applicableTo, rarity);
    }
}
