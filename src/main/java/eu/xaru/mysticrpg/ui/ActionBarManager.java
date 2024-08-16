package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;

    public ActionBarManager(MysticCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        startActionBarTask();
    }

    public void updateActionBar(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        int currentHp = data.getCurrentHp();
        int maxHp = data.getHp();
        int mana = data.getMana();

        String actionBarText = "§c❤ " + currentHp + "/" + maxHp + " §b💧 " + mana;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarText));

        // Logging for debugging
        Bukkit.getLogger().info("ActionBar updated for player " + player.getName() + ". Displayed HP: " + currentHp + "/" + maxHp);
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
