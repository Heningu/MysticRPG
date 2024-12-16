package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.player.stats.events.PlayerStatsChangedEvent;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager implements Listener {
    private final MysticCore plugin;
    private final PlayerDataCache playerDataCache;
    private final PlayerStatsManager statsManager;

    public ActionBarManager(MysticCore plugin, PlayerDataCache playerDataCache, PlayerStatsManager statsManager) {
        this.plugin = plugin;
        this.playerDataCache = playerDataCache;
        this.statsManager = statsManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startActionBarTask(); // Start the repeating task to keep the actionbar visible
    }

    public void updateActionBar(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().warning("No cached data for " + player.getName());
            return;
        }

        PlayerStats stats = statsManager.loadStats(player);
        int currentHp = data.getCurrentHp();
        int maxHp = (int) stats.getEffectiveStat(StatType.HEALTH);
        int mana = (int) stats.getEffectiveStat(StatType.MANA);

        String actionBarText = String.format("§c❤ %d/%d §b❀ %d", currentHp, maxHp, mana);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.getInstance().$(actionBarText)));
    }

    @EventHandler
    public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
        Player player = event.getPlayer();
        updateActionBar(player);
    }

    /**
     * Continuously update the action bar for all online players at a fixed interval,
     * ensuring the action bar never disappears.
     */
    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    updateActionBar(onlinePlayer);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // refresh every 2 seconds (40 ticks)
    }
}
