package eu.xaru.mysticrpg.content.modules;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.player.PlayerStats;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsModule implements Module {

    private final Main plugin;
    private final Map<UUID, PlayerStats> playerStatsMap = new HashMap<>();

    public StatsModule(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "StatsModule";
    }

    @Override
    public boolean load() {
        // Load stats from storage (if necessary)
        return true;
    }

    public PlayerStats getPlayerStats(UUID uuid) {
        return playerStatsMap.computeIfAbsent(uuid, PlayerStats::new);
    }

    public void savePlayerStats(PlayerStats playerStats) {
        // Save stats to storage
        playerStatsMap.put(playerStats.getUUID(), playerStats);
    }

    public void updatePlayerStats(Player player) {
        PlayerStats playerStats = getPlayerStats(player.getUniqueId());

        // Set max health first
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(playerStats.getHealth());

        // Ensure current health is within valid range
        double currentHealth = Math.min(playerStats.getHealth(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setHealth(currentHealth);

        // Update walk speed
        player.setWalkSpeed((float) playerStats.getSpeed());

        // Update other attributes as needed
    }
}
