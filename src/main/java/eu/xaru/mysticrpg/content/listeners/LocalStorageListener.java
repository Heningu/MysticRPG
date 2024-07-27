package eu.xaru.mysticrpg.content.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LocalStorageListener implements Listener {

    private final Main plugin;

    public LocalStorageListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLocalStorage().loadPlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getLocalStorage().loadPlayerData(player.getUniqueId());
        if (playerData != null) {
            plugin.getLocalStorage().savePlayerData(playerData);
        }
    }
}
