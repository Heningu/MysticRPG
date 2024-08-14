package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class PlayerDataManager {
    private final Main plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public PlayerDataManager(Main plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = playerDataMap.computeIfAbsent(uuid, k -> loadPlayerData(player));
        return playerData;
    }

    private PlayerData loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".json");
        if (playerFile.exists()) {
            return new PlayerData(playerFile);
        } else {
            return new PlayerData(playerFile); // initialize with default values
        }
    }

    public void save(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.save();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            PlayerData data = playerDataMap.get(uuid);
            File playerFile = new File(dataFolder, uuid.toString() + ".json");
            data.save(playerFile);
        }
    }

    public List<Player> getAllPlayers() {
        List<Player> allPlayers = new ArrayList<>();
        for (UUID uuid : playerDataMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                allPlayers.add(player);
            }
        }
        return allPlayers;
    }
}
