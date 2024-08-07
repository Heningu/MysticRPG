package eu.xaru.mysticrpg.stats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StatsCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reset", "increase");
        } else if (args.length == 2) {
            return null; // Return player names
        } else if (args.length == 3 && args[0].equalsIgnoreCase("increase")) {
            return Arrays.asList("vitality", "intelligence", "dexterity", "strength");
        }

        return Collections.emptyList();
    }
}
