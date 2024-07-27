package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarManager {

    private final Main plugin;

    public ActionBarManager(Main plugin, UiProvider uiProvider) {
        this.plugin = plugin;
        startUpdatingActionBar();
    }

    private void startUpdatingActionBar() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateActionBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second (20 ticks)
    }

    private void updateActionBar(Player player) {
        int hp = (int) player.getHealth();
        int maxHp = (int) player.getMaxHealth();
        int mana = 100; // Replace with actual mana value
        int maxMana = 100; // Replace with actual max mana value

        String message = ChatColor.RED.toString() + hp + "/" + maxHp + " ♥ | " + ChatColor.BLUE + mana + "/" + maxMana + " ֎ ";
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
