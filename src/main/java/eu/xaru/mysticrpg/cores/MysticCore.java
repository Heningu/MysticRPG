package eu.xaru.mysticrpg.cores;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPIConfig;
import eu.xaru.mysticrpg.config.ConfigCreator;
//import eu.xaru.mysticrpg.commands.CustomRecipeCommand;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCore extends JavaPlugin {

    private ModuleManager moduleManager;
    private DebugLoggerModule logger;

    @Override
    public void onEnable() {
        try {
            moduleManager.loadAllModules();
            CommandAPI.onEnable();

            if (logger != null) {
                logger.log("Core plugin enabled successfully.", 0);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error during plugin enable. Exception: " + e.getMessage(), e, null);
            }
            getServer().getPluginManager().disablePlugin(this);
        }

        // Create config files and directories
        new ConfigCreator(this).createFiles();
//
//        // CustomMobCreatorModule initialization is already handled by ModuleManager
//        // Register combined listener
//        getServer().getPluginManager().registerEvents(new MainListener(this, adminMenuMain, playerDataManager,
//                levelingManager, levelingMenu, customDamageHandler, partyManager, economyManager, statManager, statMenu, friendsMenu), this);

        // Register commands
       /* if (getCommand("money") != null) {
            getCommand("money").setExecutor(new EconomyCommand(economyManager));
            getCommand("money").setTabCompleter(new EconomyCommandTabCompleter());
        }
        if (getCommand("party") != null) {
            getCommand("party").setExecutor(new PartyCommand(partyManager, this));
            getCommand("party").setTabCompleter(new PartyCommandTabCompleter());
        }
        if (getCommand("admin") != null) {
            getCommand("admin").setExecutor(new AdminCommand(adminMenuMain));
            getCommand("admin").setTabCompleter(new AdminCommandTabCompleter());
        }
        if (getCommand("playerxp") != null) {
            getCommand("playerxp").setExecutor(new LevelingCommand(levelingManager, levelingMenu));
            getCommand("playerxp").setTabCompleter(new PlayerXPCommandTabCompleter());
        }
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(new StatsCommand(this, playerDataManager, statMenu));
            getCommand("stats").setTabCompleter(new StatsCommandTabCompleter());
        }
        if (getCommand("friends") != null) {
            getCommand("friends").setExecutor(new FriendsCommand(this, friendsManager, friendsMenu));
            getCommand("friends").setTabCompleter(new FriendsCommandTabCompleter());
        }
        if (getCommand("level") != null) {
            getCommand("level").setExecutor(new LevelingCommand(levelingManager, levelingMenu));
        }
       if (getCommand("customrecipe") != null) {
           getCommand("customrecipe").setExecutor(new CustomRecipeCommand(recipeManager, playerDataManager));
        }*/
    }

    @Override
    public void onLoad() {

        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));

        // Initialize the module manager and logger
        moduleManager = ModuleManager.getInstance();
        logger = moduleManager.getModuleInstance(DebugLoggerModule.class);

        if (logger != null) {
            logger.log("Core plugin loading...", 0);
        }
    }

    @Override
    public void onDisable() {
        // Shutdown the plugin, unload modules, and clean up resources

        CommandAPI.onDisable();

        try {
            moduleManager.shutdown();
            if (logger != null) {
                logger.log("Core plugin disabled.", 0);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error during plugin disable. Exception: " + e.getMessage(), e, null);
            }
        }

        // Check if playerDataManager is not null before calling saveAll()
//        if (playerDataManager != null) {
//            playerDataManager.saveAll();
        }
    }
