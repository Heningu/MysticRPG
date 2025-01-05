package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.player.stats.StatsModule; // Ensure this import matches your package structure
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

import java.util.logging.Level;

public class ActionBarManager implements Listener {
    private final MysticCore plugin;
    private final PlayerDataCache playerDataCache;
    private final PlayerStatsManager statsManager;
    private final StatsModule statsModule;

    public ActionBarManager(MysticCore plugin, PlayerDataCache playerDataCache, PlayerStatsManager statsManager) {
        this.plugin = plugin;
        this.playerDataCache = playerDataCache;
        this.statsManager = statsManager;

        // Get StatsModule instance so we can recalculate stats with temp attributes
        this.statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        if (statsModule == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "StatsModule not found. ActionBarManager cannot recalculate stats.");
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startActionBarTask();
    }

    public void updateActionBar(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().warning("No cached data for " + player.getName());
            return;
        }

        // Recalculate player's stats to include temp stats from equipment
        PlayerStats stats;
        if (statsModule != null) {
            // Use the recalculation method to get up-to-date temp stats
            stats = statsModule.recalculatePlayerStatsFor(player);
        } else {
            // Fallback if StatsModule is not available (Not recommended)
            // This will only show base stats
            DebugLogger.getInstance().log(Level.WARNING, "StatsModule is null, cannot recalc stats for ActionBar.");
            stats = statsManager.loadStats(player);
        }

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

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        updateActionBar(onlinePlayer);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }
}
