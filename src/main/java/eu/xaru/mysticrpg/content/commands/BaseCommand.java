package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final Main plugin;

    public BaseCommand(Main plugin) {
        this.plugin = plugin;
    }

    public abstract String getCommandName();
}
