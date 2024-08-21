package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.cores.MysticCore;

public class ConfigManager {
    private final MysticCore plugin;

    public ConfigManager(MysticCore plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }
}
