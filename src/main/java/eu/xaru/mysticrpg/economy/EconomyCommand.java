package eu.xaru.mysticrpg.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand implements CommandExecutor {
    private final EconomyManager economyManager;

    public EconomyCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 1 && args[0].equalsIgnoreCase("balance")) {
                double balance = economyManager.getBalance(player);
                player.sendMessage("Your balance: $" + balance);
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    try {
                        double amount = Double.parseDouble(args[2]);
                        economyManager.sendMoney(player, target, amount);
                        player.sendMessage("You sent $" + amount + " to " + target.getName());
                        target.sendMessage("You received $" + amount + " from " + player.getName());
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid amount.");
                    }
                } else {
                    player.sendMessage("Player not found.");
                }
                return true;
            }
        } else if (sender.hasPermission("mysticrpg.admin")) {
            if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    double balance = economyManager.getBalance(target);
                    sender.sendMessage(target.getName() + "'s balance: $" + balance);
                } else {
                    sender.sendMessage("Player not found.");
                }
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    try {
                        double amount = Double.parseDouble(args[2]);
                        economyManager.setBalance(target, amount);
                        sender.sendMessage("Set " + target.getName() + "'s balance to $" + amount);
                        target.sendMessage("Your balance was set to $" + amount);
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
