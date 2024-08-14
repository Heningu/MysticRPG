package eu.xaru.mysticrpg.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EconomyCommandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("balance", "send", "check", "set");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("set"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("set"))) {
            return Collections.singletonList("<amount>");
        }
        return Collections.emptyList();
    }
}
