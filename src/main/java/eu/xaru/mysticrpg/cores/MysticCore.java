package eu.xaru.mysticrpg.cores;

import eu.xaru.mysticrpg.ConfigCreator;
import eu.xaru.mysticrpg.CustomDamageHandler;
import eu.xaru.mysticrpg.admin.AdminMenuMain;
import eu.xaru.mysticrpg.admin.commands.AdminCommand;
import eu.xaru.mysticrpg.admin.commands.AdminCommandTabCompleter;
import eu.xaru.mysticrpg.economy.EconomyCommand;
import eu.xaru.mysticrpg.economy.EconomyCommandTabCompleter;
import eu.xaru.mysticrpg.economy.EconomyManager;
import eu.xaru.mysticrpg.leveling.LevelingCommand;
import eu.xaru.mysticrpg.leveling.LevelingManager;
import eu.xaru.mysticrpg.leveling.PlayerXPCommandTabCompleter;
import eu.xaru.mysticrpg.leveling.LevelingMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.party.PartyCommand;
import eu.xaru.mysticrpg.party.PartyCommandTabCompleter;
import eu.xaru.mysticrpg.party.PartyManager;
import eu.xaru.mysticrpg.stats.StatManager;
import eu.xaru.mysticrpg.stats.StatMenu;
import eu.xaru.mysticrpg.stats.StatsCommand;
import eu.xaru.mysticrpg.stats.StatsCommandTabCompleter;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import eu.xaru.mysticrpg.ui.ActionBarManager;
import eu.xaru.mysticrpg.ui.CustomScoreboardManager;
import eu.xaru.mysticrpg.listeners.MainListener;
import eu.xaru.mysticrpg.friends.FriendsCommand;
import eu.xaru.mysticrpg.friends.FriendsCommandTabCompleter;
import eu.xaru.mysticrpg.friends.FriendsManager;
import eu.xaru.mysticrpg.friends.FriendsMenu;
import eu.xaru.mysticrpg.utils.DeadlockDetector;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MysticCore extends JavaPlugin {
    public PlayerDataManager playerDataManager;
    public EconomyManager economyManager;
    public ActionBarManager actionBarManager;
    public CustomScoreboardManager scoreboardManager;
    public PartyManager partyManager;
    public AdminMenuMain adminMenuMain;
    public CustomDamageHandler customDamageHandler;
    public LevelingManager levelingManager;
    public LevelingMenu levelingMenu;
    public StatManager statManager;
    public StatMenu statMenu;
    public FriendsManager friendsManager;
    public FriendsMenu friendsMenu;

    private ModuleManager moduleManager;
    private DeadlockDetector deadlockDetector;
    private DebugLoggerModule logger;
    @Override
    public void onEnable() {
        try {
            moduleManager.loadAllModules();
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

        // Initialize components in the correct order
        this.playerDataManager = new PlayerDataManager(this, new File(getDataFolder(), "playerdata"));
        this.statManager = new StatManager(this, playerDataManager);
        this.levelingManager = new LevelingManager(playerDataManager, statManager);
        this.levelingMenu = new LevelingMenu(this, levelingManager, playerDataManager);
        this.partyManager = new PartyManager(levelingManager);
        this.scoreboardManager = new CustomScoreboardManager(this);
        this.economyManager = new EconomyManager(playerDataManager, scoreboardManager);
        this.actionBarManager = new ActionBarManager(this, playerDataManager);
        this.customDamageHandler = new CustomDamageHandler(this, playerDataManager, actionBarManager);
        this.statMenu = new StatMenu(this, playerDataManager);
        this.friendsManager = new FriendsManager(this, playerDataManager);
        this.friendsMenu = new FriendsMenu(this);

        // Register combined listener
        getServer().getPluginManager().registerEvents(new MainListener(this, adminMenuMain, playerDataManager,
                levelingManager, levelingMenu, customDamageHandler, partyManager, economyManager, statManager, statMenu, friendsMenu), this);

        // Register commands
        if (getCommand("money") != null) {
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

    }

    @Override
    public void onLoad() {
        // Initialize the module manager and logger
        moduleManager = ModuleManager.getInstance();
        logger = moduleManager.getModuleInstance(DebugLoggerModule.class);

        // Initialize DeadlockDetector
        deadlockDetector = new DeadlockDetector(10, TimeUnit.SECONDS, moduleManager, logger);
        deadlockDetector.start();

        if (logger != null) {
            logger.log("Core plugin loading...", 0);
        }
    }

    @Override
    public void onDisable() {

        // Shutdown the plugin, unload modules, and clean up resources
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

        playerDataManager.saveAll();
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public CustomScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public AdminMenuMain getAdminMenuMain() {
        return adminMenuMain;
    }

    public CustomDamageHandler getCustomDamageHandler() {
        return customDamageHandler;
    }

    public LevelingManager getLevelingManager() {
        return levelingManager;
    }

    public LevelingMenu getLevelingMenu() {
        return levelingMenu;
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public StatMenu getStatMenu() {
        return statMenu;
    }

    public FriendsManager getFriendsManager() {
        return friendsManager;
    }

    public FriendsMenu getFriendsMenu() {
        return friendsMenu;
    }
}
