package eu.xaru.mysticrpg.player.leveling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerXPCommandTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "set");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set"))) {
            return null; // Allow player names to be auto-completed
        }
        return Collections.emptyList();
    }
}
