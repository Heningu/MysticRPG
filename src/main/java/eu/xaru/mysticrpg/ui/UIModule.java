package eu.xaru.mysticrpg.ui;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * UIModule handles the user interface components such as commands and display names.
 */
public class UIModule implements IBaseModule {

    private ActionBarManager actionBarManager;
    private ScoreboardManager scoreboardManager;

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private ChatFormatter chatFormatter;


    /**
     * Constructs a new UIModule instance.
     */
    public UIModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Assuming MysticCore is the main class
        this.eventManager = new EventManager(plugin);
    }

    @Override
    public void initialize() {

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.chatFormatter = new ChatFormatter();
        Bukkit.getPluginManager().registerEvents(this.chatFormatter, plugin);

        if (saveModule != null && levelModule != null) {
            PlayerDataCache playerDataCache = saveModule.getPlayerDataCache();
            this.actionBarManager = new ActionBarManager((MysticCore) plugin, playerDataCache);
            this.scoreboardManager = new ScoreboardManager(); // Initialize ScoreboardManager
            DebugLogger.getInstance().log(Level.INFO, "UIModule initialized successfully.", 0);
        } else {
            DebugLogger.getInstance().severe("[MysticRPG] SaveModule or LevelModule is not initialized. UIModule cannot function without them.");
            return;
        }

        // Register event listeners using EventManager
        registerEventListeners();

        // Register LevelModule's level-up callback to update ScoreboardManager
        if (levelModule != null) {
            levelModule.setLevelUpListener(player -> {
                if (scoreboardManager != null) {
                    scoreboardManager.createOrUpdateTeam(player); // Ensure team is updated first
                    scoreboardManager.updatePlayerScoreboard(player); // Then update scoreboard
                }
            });
        }
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "UIModule started", 0);
        registerCommands();
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "UIModule stopped", 0);
    }

    @Override
    public void unload() {
        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
        }
        DebugLogger.getInstance().log(Level.INFO, "UIModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, LevelModule.class);  // Depend on SaveModule and LevelModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Registers the /name and /refreshname commands using CommandAPI.
     * Since prefixes are managed via the scoreboard teams, /name informs players accordingly.
     * /refreshname allows admins to manually update a player's display name and side scoreboard.
     */
    private void registerCommands() {
        // Parent /name command
        new CommandAPICommand("name")
                .withPermission("mysticrpg.debug")
                .executes((sender, args) -> {
                    sender.sendMessage(ChatColor.RED + "Prefixes are managed automatically through LuckPerms.");
                })
                .register();

        // Optional: /refreshname <player> command for manual updates
        new CommandAPICommand("refreshname")
                .withPermission("mysticrpg.debug")
                .withArguments(new dev.jorel.commandapi.arguments.PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    if (scoreboardManager != null) {
                        scoreboardManager.createOrUpdateTeam(target); // Update team first
                        scoreboardManager.updatePlayerScoreboard(target); // Then update scoreboard
                        sender.sendMessage(ChatColor.GREEN + "Refreshed scoreboard for " + target.getName());
                        target.sendMessage(ChatColor.GREEN + "Your scoreboard has been refreshed by " + sender.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Error: ScoreboardManager is not initialized.");
                    }
                })
                .register();
    }

    /**
     * Registers event listeners for player join and quit events.
     */
    private void registerEventListeners() {
        // Handle PlayerJoinEvent
        eventManager.registerEvent(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();

            // Assign the per-player scoreboard and add the player to all teams
            if (scoreboardManager != null) {
                scoreboardManager.createScoreboardForPlayer(player);
                scoreboardManager.addNewPlayer(player);
            }

            // Update display name with current level and prefix is handled within ScoreboardManager
        });

        // Handle PlayerQuitEvent
        eventManager.registerEvent(PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();

            // Remove the player from all scoreboards and clean up
            if (scoreboardManager != null) {
                scoreboardManager.removePlayer(player);
            }
        });
    }

    /**
     * Retrieves the ScoreboardManager instance.
     *
     * @return The ScoreboardManager.
     */
    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    /**
     * Retrieves the ActionBarManager instance.
     *
     * @return The ActionBarManager.
     */
    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    /**
     * Sets the ActionBarManager instance.
     *
     * @param actionBarManager The ActionBarManager to set.
     */
    public void setActionBarManager(ActionBarManager actionBarManager) {
        this.actionBarManager = actionBarManager;
    }

    /**
     * Retrieves the ChatFormatter instance.
     *
     * @return The ChatFormatter.
     */
    public ChatFormatter getChatFormatter() {
        return chatFormatter;
    }

    /**
     * Sets the ChatFormatter instance.
     *
     * @param chatFormatter The ChatFormatter to set.
     */
    public void setChatFormatter(ChatFormatter chatFormatter) {
        this.chatFormatter = chatFormatter;
    }
}
