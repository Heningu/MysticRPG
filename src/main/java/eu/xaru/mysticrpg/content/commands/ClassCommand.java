package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.classes.ClassManager;
import eu.xaru.mysticrpg.content.classes.PlayerClass;
import eu.xaru.mysticrpg.content.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ClassCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClassManager classManager;
    private final PlayerManager playerManager;

    public ClassCommand(Main plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getManagers().getClassManager();
        this.playerManager = plugin.getManagers().getPlayerManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mysticrpg.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /class <list|select> [USER] [CLASS]");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.GOLD + "Available classes:");
            for (String className : classManager.getClassNames()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + className);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("select")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /class select <USER> <CLASS>");
                return true;
            }

            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            String className = args[2];
            PlayerClass playerClass = classManager.getClass(className);
            if (playerClass == null) {
                sender.sendMessage(ChatColor.RED + "Class not found.");
                return true;
            }

            playerManager.setPlayerClass(targetPlayer, playerClass);
            sender.sendMessage(ChatColor.GREEN + "Class " + className + " has been set for player " + targetPlayer.getName() + ".");
            targetPlayer.sendMessage(ChatColor.GREEN + "Your class has been set to " + className + ".");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /class <list|select> [USER] [CLASS]");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("select");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("select")) {
            completions.addAll(classManager.getClassNames());
        }

        return completions;
    }
}
