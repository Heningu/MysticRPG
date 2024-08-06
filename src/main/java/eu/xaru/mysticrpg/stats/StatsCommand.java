package eu.xaru.mysticrpg.stats;

import eu.xaru.mysticrpg.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    private final Main plugin;
    private final StatMenu statMenu;

    public StatsCommand(Main plugin, StatMenu statMenu) {
        this.plugin = plugin;
        this.statMenu = statMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            statMenu.openStatMenu(player);
            return true;
        }
        sender.sendMessage("This command can only be used by players.");
        return false;
    }
}
