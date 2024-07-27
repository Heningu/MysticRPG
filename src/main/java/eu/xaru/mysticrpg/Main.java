package eu.xaru.mysticrpg;

import eu.xaru.mysticrpg.config.Config;
import eu.xaru.mysticrpg.config.DefaultConfigCreator;
import eu.xaru.mysticrpg.content.Managers;
import eu.xaru.mysticrpg.storage.LocalStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;

    private Config config;
    private LocalStorage localStorage;
    private Managers managers;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        Logger.log("Enabling MysticRPG...");

        this.config = new Config(this);
        this.localStorage = new LocalStorage(this);
        this.managers = new Managers(this);

        new DefaultConfigCreator(this).createDefaultFiles();

        managers.loadModules();
        managers.registerCommands();
        managers.registerListeners();

        Logger.log("All done! MysticRPG is ready to go.");
    }

    @Override
    public void onDisable() {
        Logger.log("Shutting down.");
    }

    public Config getConfigManager() {
        return config;
    }

    public LocalStorage getLocalStorage() {
        return localStorage;
    }

    public Managers getManagers() {
        return managers;
    }
}
