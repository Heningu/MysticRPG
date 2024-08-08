package eu.xaru.mysticrpg;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final Main plugin;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }
}
