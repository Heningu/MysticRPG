/*package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class PlayerDataManager {
    private final MysticCore plugin;
    private final File dataFolder;
    private final Map<UUID, old_playerdata> playerDataMap = new HashMap<>();

    public PlayerDataManager(MysticCore plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public old_playerdata getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        old_playerdata playerData = playerDataMap.computeIfAbsent(uuid, k -> loadPlayerData(player));
        return playerData;
    }

    private old_playerdata loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".json");
        if (playerFile.exists()) {
            return new old_playerdata(playerFile);
        } else {
            return new old_playerdata(playerFile); // initialize with default values
        }
    }

    public void save(Player player) {
        old_playerdata data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.save();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            old_playerdata data = playerDataMap.get(uuid);
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
}*/
