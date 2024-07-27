package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadCfgCommand extends BaseCommand {

    public ReloadCfgCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public String getCommandName() {
        return "reloadcfg";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || sender.isOp()) {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage("MysticRPG configuration reloaded.");
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());

            // Reload other managers if necessary
            plugin.getManagers().loadModules(); // Reload modules if needed
            plugin.getManagers().getMenuManager().loadMenus(); // Reload menus if needed
            plugin.getManagers().getClassManager().loadClasses(); // Reload classes if needed

        } else {
            sender.sendMessage("You do not have permission to run this command.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null; // No tab completion needed
    }
}
