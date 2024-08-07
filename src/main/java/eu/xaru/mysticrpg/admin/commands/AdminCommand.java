package eu.xaru.mysticrpg.admin.commands;

import eu.xaru.mysticrpg.admin.AdminMenuMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final AdminMenuMain adminMenuMain;

    public AdminCommand(AdminMenuMain adminMenuMain) {
        this.adminMenuMain = adminMenuMain;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mysticrpg.admin")) {
                adminMenuMain.openAdminMenu(player);
                return true;
            }
        }
        sender.sendMessage("You do not have permission to use this command.");
        return false;
    }
}
