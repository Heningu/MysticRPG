package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.menus.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuCommand extends BaseCommand {

    private final MenuManager menuManager;

    public MenuCommand(Main plugin) {
        super(plugin);
        this.menuManager = plugin.getManagers().getMenuManager();
    }

    @Override
    public String getCommandName() {
        return "menu";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                String menuName = args[0];
                menuManager.openMenu(player, menuName);
                return true;
            }
            player.sendMessage("Usage: /menu <menuName>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(menuManager.getMenuNames());
        }
        return Arrays.asList();
    }
}
