package eu.xaru.mysticrpg;

import eu.xaru.mysticrpg.admin.AdminMenuMain;
import eu.xaru.mysticrpg.admin.commands.AdminCommand;
import eu.xaru.mysticrpg.admin.commands.AdminCommandTabCompleter;
import eu.xaru.mysticrpg.economy.EconomyCommand;
import eu.xaru.mysticrpg.economy.EconomyManager;
import eu.xaru.mysticrpg.leveling.LevelingCommand;
import eu.xaru.mysticrpg.leveling.LevelingManager;
import eu.xaru.mysticrpg.leveling.PlayerXPCommandTabCompleter;
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
import eu.xaru.mysticrpg.modules.CustomDamageHandler;
import eu.xaru.mysticrpg.listeners.MainListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public PlayerDataManager playerDataManager;
    public EconomyManager economyManager;
    public ActionBarManager actionBarManager;
    public CustomScoreboardManager scoreboardManager;
    public PartyManager partyManager;
    public AdminMenuMain adminMenuMain;
    public CustomDamageHandler customDamageHandler;
    public LevelingManager levelingManager;
    public StatManager statManager;
    public StatMenu statMenu;

    @Override
    public void onEnable() {
        this.playerDataManager = new PlayerDataManager(this, getDataFolder());
        this.economyManager = new EconomyManager(playerDataManager);
        this.actionBarManager = new ActionBarManager(this, playerDataManager);
        this.scoreboardManager = new CustomScoreboardManager(this);
        this.levelingManager = new LevelingManager(playerDataManager);
        this.partyManager = new PartyManager(levelingManager);
        this.adminMenuMain = new AdminMenuMain(this);
        this.customDamageHandler = new CustomDamageHandler(this, playerDataManager, actionBarManager);
        this.statManager = new StatManager(this, playerDataManager);
        this.statMenu = new StatMenu(this, playerDataManager);

        // Register combined listener
        getServer().getPluginManager().registerEvents(new MainListener(this, adminMenuMain, playerDataManager,
                levelingManager, customDamageHandler, partyManager, economyManager, statManager, statMenu), this);

        // Register commands
        if (getCommand("money") != null) {
            getCommand("money").setExecutor(new EconomyCommand(economyManager));
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
            getCommand("playerxp").setExecutor(new LevelingCommand(levelingManager));
            getCommand("playerxp").setTabCompleter(new PlayerXPCommandTabCompleter());
        }
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(new StatsCommand(this, statMenu));
            getCommand("stats").setTabCompleter(new StatsCommandTabCompleter());
        }
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

    public StatManager getStatManager() {
        return statManager;
    }

    public StatMenu getStatMenu() {
        return statMenu;
    }
}
