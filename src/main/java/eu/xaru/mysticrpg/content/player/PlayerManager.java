package eu.xaru.mysticrpg.content.player;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.classes.PlayerClass;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private final Main plugin;
    private final Map<UUID, PlayerClass> playerClasses = new HashMap<>();

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setPlayerClass(Player player, PlayerClass playerClass) {
        playerClasses.put(player.getUniqueId(), playerClass);
        // Additional logic for setting player class (e.g., updating stats) can be added here
    }

    public PlayerClass getPlayerClass(Player player) {
        return playerClasses.get(player.getUniqueId());
    }

    public void resetPlayerClass(Player player) {
        playerClasses.remove(player.getUniqueId());
        // Additional logic for resetting player class can be added here
    }

    public boolean hasClass(Player player) {
        return playerClasses.containsKey(player.getUniqueId());
    }

    public void loadPlayer(Player player) {
        // Logic for loading player data (e.g., from storage)
    }

    public void savePlayer(Player player) {
        // Logic for saving player data (e.g., to storage)
    }

    public void initializePlayer(Player player) {
        // Initialize base stats, class, XP, etc.
        // This is just an example; customize it according to your needs
        player.setHealth(20.0); // Set base health
        player.setLevel(0); // Set XP to 0
        // Additional initialization logic here
    }
}
