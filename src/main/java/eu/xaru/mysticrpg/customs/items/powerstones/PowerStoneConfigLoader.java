package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.customs.items.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PowerStoneConfigLoader {

    private final File configFile;

    public PowerStoneConfigLoader(File configFile) {
        this.configFile = configFile;
    }

    public PowerStone loadPowerStone() throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        String id = config.getString("id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("PowerStone ID is missing in file: " + configFile.getName());
        }

        String name = config.getString("name", "Power Stone");
        String materialName = config.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            throw new IllegalArgumentException("Invalid material for power stone " + id + " in file: " + configFile.getName());
        }

        int customModelData = config.getInt("custom_model_data", 0);

        String description = config.getString("description", "");

        String effectName = config.getString("effect");
        if (effectName == null || effectName.isEmpty()) {
            throw new IllegalArgumentException("Effect is missing for power stone " + id + " in file: " + configFile.getName());
        }

        String applicableTo = config.getString("applicable_to", "ALL");

        String rarityName = config.getString("rarity", "COMMON");
        Rarity rarity = Rarity.valueOf(rarityName.toUpperCase());

        PowerStone powerStone = new PowerStone(id, name, material, customModelData, description, effectName, applicableTo, rarity);

        return powerStone;
    }
}
