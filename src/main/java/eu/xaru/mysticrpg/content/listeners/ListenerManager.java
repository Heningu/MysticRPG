package eu.xaru.mysticrpg.content.listeners;

import eu.xaru.mysticrpg.Main;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

public class ListenerManager {
    private final Main plugin;

    public ListenerManager(Main plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(plugin), plugin);
        pm.registerEvents(new LocalStorageListener(plugin), plugin);
        // Register other listeners if needed
    }
}
