package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;

public class CommandManager {
    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        if (plugin.getCommand("money") != null) {
            plugin.getCommand("money").setExecutor(new EconomyCommands(plugin));
        }
        if (plugin.getCommand("amoney") != null) {
            plugin.getCommand("amoney").setExecutor(new EconomyCommands(plugin));
        }
        if (plugin.getCommand("menu") != null) {
            plugin.getCommand("menu").setExecutor(new MenuCommand(plugin));
        }
        if (plugin.getCommand("class") != null) {
            plugin.getCommand("class").setExecutor(new ClassCommand(plugin));
        }
        if (plugin.getCommand("reloadcfg") != null) {
            plugin.getCommand("reloadcfg").setExecutor(new ReloadCfgCommand(plugin));
        }
    }
}
