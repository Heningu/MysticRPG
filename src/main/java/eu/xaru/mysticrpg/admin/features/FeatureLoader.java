package eu.xaru.mysticrpg.admin.features;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.admin.players.PlayerBanFeature;
import eu.xaru.mysticrpg.admin.players.PlayerStatsFeature;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FeatureLoader {
    private final MysticCore plugin;
    private final List<PlayerFeature> playerFeatures;

    public FeatureLoader(MysticCore plugin) {
        this.plugin = plugin;
        this.playerFeatures = new ArrayList<>();
        loadPlayerFeatures();
    }

    private void loadPlayerFeatures() {
        playerFeatures.add(new PlayerBanFeature(plugin));
        playerFeatures.add(new PlayerStatsFeature(plugin));
        // Add other features here
    }

    public void loadMainFeatures(Player player) {
        // Logic to display main features in the admin menu
        openMainMenu(player);
    }

    public void openMainMenu(Player player) {
        plugin.getAdminMenuMain().openAdminMenu(player);
    }

    public List<PlayerFeature> getPlayerFeatures() {
        return playerFeatures;
    }

    public Map<UUID, Player> getPlayerEditMap() {
        return plugin.getAdminMenuMain().getPlayerEditMap();
    }
}
