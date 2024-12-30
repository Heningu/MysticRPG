package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles reading/writing each player's pet level/xp to local .yml files.
 */
public class PetFileStorage {

    private static File playerDataFolder;

    /**
     * Must be called once in your onEnable or module init:
     */
    public static void init(JavaPlugin plugin) {
        playerDataFolder = new File(plugin.getDataFolder(), "pets/playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    /**
     * Loads the player's pet data from the local file.
     * Returns a map: petId -> [level, xp].
     */
    public static Map<String, PetProgress> loadPlayerPets(Player player) {
        Map<String, PetProgress> result = new HashMap<>();

        File file = getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return result; // empty if no file
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.contains("pets")) {
            for (String petId : yaml.getConfigurationSection("pets").getKeys(false)) {
                int level = yaml.getInt("pets." + petId + ".level", 1);
                int xp    = yaml.getInt("pets." + petId + ".xp", 0);
                result.put(petId, new PetProgress(level, xp));
            }
        }
        return result;
    }

    /**
     * Saves the player's pet data to local file:
     *  pets:
     *    <petId>:
     *       level: <level>
     *       xp: <xp>
     */
    public static void savePlayerPets(Player player, Map<String, PetProgress> data) {
        File file = getPlayerFile(player.getUniqueId());
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, PetProgress> entry : data.entrySet()) {
            String petId = entry.getKey();
            PetProgress prog = entry.getValue();
            yaml.set("pets." + petId + ".level", prog.getLevel());
            yaml.set("pets." + petId + ".xp",    prog.getXp());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            DebugLogger.getInstance().log(Level.SEVERE, "Failed to save pet data for " + player.getName(), 0);
            e.printStackTrace();
        }
    }

    private static File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, "player_" + uuid.toString() + ".yml");
    }

    // A simple holder for the pet's level + xp
    public static class PetProgress {
        private final int level;
        private final int xp;
        public PetProgress(int level, int xp) {
            this.level = level;
            this.xp = xp;
        }
        public int getLevel() { return level; }
        public int getXp()    { return xp; }
    }
}
