package eu.xaru.mysticrpg;

import eu.xaru.mysticrpg.cores.MysticCore;

public class ConfigManager {
    private final MysticCore plugin;

    public ConfigManager(MysticCore plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }
}
