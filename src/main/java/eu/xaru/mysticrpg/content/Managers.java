package eu.xaru.mysticrpg.content;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.commands.CommandManager;
import eu.xaru.mysticrpg.content.listeners.ListenerManager;
import eu.xaru.mysticrpg.content.menus.MenuManager;
import eu.xaru.mysticrpg.content.modules.ModuleManager;
import eu.xaru.mysticrpg.content.classes.ClassManager;
import eu.xaru.mysticrpg.content.player.PlayerManager;
import eu.xaru.mysticrpg.content.utils.UtilManager;
import eu.xaru.mysticrpg.content.modules.EconomyModule;

public class Managers {
    private final Main plugin;

    private final CommandManager commandManager;
    private final ListenerManager listenerManager;
    private final MenuManager menuManager;
    private final ModuleManager moduleManager;
    private final ClassManager classManager;
    private final UtilManager utilManager;
    private final PlayerManager playerManager;

    public Managers(Main plugin) {
        this.plugin = plugin;

        this.commandManager = new CommandManager(plugin);
        this.listenerManager = new ListenerManager(plugin);
        this.menuManager = new MenuManager(plugin);
        this.moduleManager = new ModuleManager(plugin);
        this.classManager = new ClassManager(plugin);
        this.utilManager = new UtilManager();
        this.playerManager = new PlayerManager(plugin);
    }

    public void loadModules() {
        moduleManager.loadModules();
    }

    public void registerCommands() {
        commandManager.registerCommands();
    }

    public void registerListeners() {
        listenerManager.registerListeners();
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public UtilManager getUtilManager() {
        return utilManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public EconomyModule getEconomyModule() {
        return moduleManager.getEconomyModule();
    }
}
