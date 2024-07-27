package eu.xaru.mysticrpg.content.modules;

import eu.xaru.mysticrpg.Main;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final Main plugin;
    private final List<Module> modules = new ArrayList<>();
    private EconomyModule economyModule;

    public ModuleManager(Main plugin) {
        this.plugin = plugin;
        this.economyModule = new EconomyModule(plugin);
        modules.add(economyModule);
    }

    public void loadModules() {
        for (Module module : modules) {
            if (module.load()) {
                plugin.getLogger().info(module.getName() + " module loaded successfully.");
            } else {
                plugin.getLogger().warning("Failed to load " + module.getName() + " module.");
            }
        }
    }

    public EconomyModule getEconomyModule() {
        return economyModule;
    }
}
