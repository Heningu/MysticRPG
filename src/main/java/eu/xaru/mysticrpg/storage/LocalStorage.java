package eu.xaru.mysticrpg.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalStorage {

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final File dataFolder;

    public LocalStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData loadPlayerData(UUID playerUUID) {
        if (playerDataMap.containsKey(playerUUID)) {
            return playerDataMap.get(playerUUID);
        }

        File playerFile = new File(dataFolder, playerUUID + ".yml");
        if (!playerFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        PlayerData playerData = new PlayerData(playerUUID);
        playerData.setHp(config.getInt("hp", 20));
        playerData.setMaxHp(config.getInt("maxHp", 20));
        playerData.setMana(config.getInt("mana", 10));
        playerData.setMaxMana(config.getInt("maxMana", 10));
        playerData.setCoins(config.getInt("coins", 0));
        playerData.setSkillPoints(config.getInt("skillPoints", 0));

        playerDataMap.put(playerUUID, playerData);
        return playerData;
    }

    public void savePlayerData(PlayerData playerData) {
        File playerFile = new File(dataFolder, playerData.getUUID() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("hp", playerData.getHp());
        config.set("maxHp", playerData.getMaxHp());
        config.set("mana", playerData.getMana());
        config.set("maxMana", playerData.getMaxMana());
        config.set("coins", playerData.getCoins());
        config.set("skillPoints", playerData.getSkillPoints());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + playerData.getUUID());
            e.printStackTrace();
        }

        playerDataMap.put(playerData.getUUID(), playerData);
    }
}
