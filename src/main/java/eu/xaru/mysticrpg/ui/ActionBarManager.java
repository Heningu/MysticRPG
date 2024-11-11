package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {
    private final MysticCore plugin;
    private final PlayerDataCache playerDataCache;

    public ActionBarManager(MysticCore plugin, PlayerDataCache playerDataCache) {
        this.plugin = plugin;
        this.playerDataCache = playerDataCache;
        startActionBarTask();
    }

    public void updateActionBar(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());

        if (data == null) {
            Bukkit.getLogger().warning("No cached data found for player " + player.getName());
            return;
        }

        int currentHp = data.getCurrentHp();
        int maxHp = data.getAttributes().getOrDefault("HP", 20);
        int mana = data.getAttributes().getOrDefault("MANA", 20);

        String actionBarText = "§c❤ " + currentHp + "/" + maxHp + " §b❀ " + mana;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.getInstance().$(actionBarText))) ;

        // Logging for debugging
        // Bukkit.getLogger().info("ActionBar updated for player " + player.getName() + ". Displayed HP: " + currentHp + "/" + maxHp);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateActionBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Refresh every 2 seconds (40 ticks)
    }
}
