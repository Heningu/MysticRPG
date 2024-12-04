package eu.xaru.mysticrpg.customs.mobs.bossbar;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class MobBossBarHandler {

    private final CustomMobInstance mobInstance;
    private final BossBar bossBar;
    private final Set<Player> playersInRange = new HashSet<>();

    public MobBossBarHandler(CustomMobInstance mobInstance) {
        this.mobInstance = mobInstance;

        // Create the boss bar based on the mob's configuration
        String title = mobInstance.getCustomMob().getBossBarConfig().getTitle()
                .replace("%level%", String.valueOf(mobInstance.getCustomMob().getLevel()));

        bossBar = Bukkit.createBossBar(
                title,
                mobInstance.getCustomMob().getBossBarConfig().getColor(),
                mobInstance.getCustomMob().getBossBarConfig().getStyle()
        );

        // Initially set the boss bar progress based on mob's health
        updateBossBar();
    }

    /**
     * Updates the boss bar's progress based on the mob's current health.
     */
    public void updateBossBar() {
        double progress = mobInstance.getCurrentHp() / mobInstance.getCustomMob().getHealth();
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    /**
     * Adds a player to the boss bar view.
     *
     * @param player The player to add.
     */
    public void addPlayer(Player player) {
        bossBar.addPlayer(player);
        playersInRange.add(player);
    }

    /**
     * Removes a player from the boss bar view.
     *
     * @param player The player to remove.
     */
    public void removePlayer(Player player) {
        bossBar.removePlayer(player);
        playersInRange.remove(player);
    }

    /**
     * Removes all players and hides the boss bar.
     */
    public void removeAllPlayers() {
        bossBar.removeAll();
        playersInRange.clear();
    }

    /**
     * Gets the set of players currently viewing the boss bar.
     *
     * @return Set of players.
     */
    public Set<Player> getPlayersInRange() {
        return playersInRange;
    }

    public double getRange() {
        return mobInstance.getCustomMob().getBossBarConfig().getRange();
    }
}
