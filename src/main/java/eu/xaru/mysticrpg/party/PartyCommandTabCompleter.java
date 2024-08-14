package eu.xaru.mysticrpg.party;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PartyCommandTabCompleter implements TabCompleter {

    // Updated list of subcommands
    private static final List<String> SUBCOMMANDS = Arrays.asList("create", "invite", "accept", "decline", "leave", "list");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            // Provide player names for auto-completion when using the "invite" subcommand
            return null; // Returning null allows Bukkit to handle player name completion
        }
        return Collections.emptyList(); // No other completions are needed
    }
}
