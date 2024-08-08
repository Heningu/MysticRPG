package eu.xaru.mysticrpg.leveling;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelingCommand implements CommandExecutor {
    private final LevelingManager levelingManager;
    private final LevelingMenu levelingMenu;

    public LevelingCommand(LevelingManager levelingManager, LevelingMenu levelingMenu) {
        this.levelingManager = levelingManager;
        this.levelingMenu = levelingMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                levelingMenu.openLevelingMenu(player, 1); // Open the first page (0-based index)
                return true;
            }
        } else if (sender.hasPermission("mysticrpg.admin")) {
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    try {
                        int amount = Integer.parseInt(args[2]);
                        levelingManager.addXp(target, amount);
                        sender.sendMessage("Gave " + amount + " XP to " + target.getName());
                        target.sendMessage("You received " + amount + " XP.");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Invalid amount.");
                    }
                } else {
                    sender.sendMessage("Player not found.");
                }
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    try {
                        int amount = Integer.parseInt(args[2]);
                        levelingManager.setXp(target, amount);
                        sender.sendMessage("Set " + target.getName() + "'s XP to " + amount);
                        target.sendMessage("Your XP was set to " + amount);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Invalid amount.");
                    }
                } else {
                    sender.sendMessage("Player not found.");
                }
                return true;
            }
        }
        return false;
    }
}
