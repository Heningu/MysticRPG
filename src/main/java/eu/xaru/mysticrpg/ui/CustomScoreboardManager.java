package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.modules.Module;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class CustomScoreboardManager implements Module {
    private final Main plugin;

    public CustomScoreboardManager(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CustomScoreboardManager";
    }

    @Override
    public boolean load() {
        return true;
    }

    public void updateScoreboard(Player player) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        player.setScoreboard(board);
    }
}
