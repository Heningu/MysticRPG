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
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.logging.Level;

/**
 * UIModule handles the user interface components such as commands and display names.
 */
public class UIModule implements IBaseModule {

    private ActionBarManager actionBarManager;
    private ScoreboardManager scoreboardManager;
    private DebugLoggerModule logger;
    private final JavaPlugin plugin;
    private TitleManager titleManager;
    private final EventManager eventManager;

    /**
     * Constructs a new UIModule instance.
     */
    public UIModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Assuming MysticCore is the main class
        this.eventManager = new EventManager(plugin);
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);

        if (saveModule != null && levelModule != null) {
            PlayerDataCache playerDataCache = saveModule.getPlayerDataCache();
            this.actionBarManager = new ActionBarManager((MysticCore) plugin, playerDataCache);
            this.scoreboardManager = new ScoreboardManager(); // Initialize ScoreboardManager
            this.titleManager = new TitleManager(plugin, scoreboardManager); // Pass the ScoreboardManager instance
            logger.log(Level.INFO, "UIModule initialized successfully.", 0);
        } else {
            if (logger != null) {
                logger.error("SaveModule or LevelModule is not initialized. UIModule cannot function without them.");
            } else {
                Bukkit.getLogger().severe("[MysticRPG] SaveModule or LevelModule is not initialized. UIModule cannot function without them.");
            }
            return;
        }

        // Register event listeners using EventManager
        registerEventListeners();

        // Register LevelModule's level-up callback to update TitleManager
        if (levelModule != null) {
            levelModule.setLevelUpListener(player -> {
                if (titleManager != null) {
                    titleManager.onPlayerLevelUp(player);
                }
            });
        }
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "UIModule started", 0);
        registerCommands();
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "UIModule stopped", 0);
    }

    @Override
    public void unload() {
        if (titleManager != null) {
            titleManager.cleanup();
        }
        logger.log(Level.INFO, "UIModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, DebugLoggerModule.class, LevelModule.class);  // Depend on SaveModule, DebugLoggerModule, and LevelModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers the /name and /refreshname commands using CommandAPI.
     * Since prefixes are managed via LuckPerms, /name informs players accordingly.
     * /refreshname allows admins to manually update a player's display name.
     */
    private void registerCommands() {
        // Parent /name command
        new CommandAPICommand("name")
                .withPermission("mysticrpg.name")
                .executes((sender, args) -> {
                    sender.sendMessage(ChatColor.RED + "Prefixes are managed through LuckPerms.");
                })
                .register();

        // Optional: /refreshname <player> command for manual updates
        new CommandAPICommand("refreshname")
                .withPermission("mysticrpg.admin")
                .withArguments(new dev.jorel.commandapi.arguments.PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    if (titleManager != null) {
                        titleManager.updatePlayerDisplayName(target);
                        sender.sendMessage(ChatColor.GREEN + "Refreshed display name for " + target.getName());
                        target.sendMessage(ChatColor.GREEN + "Your display name has been refreshed by " + sender.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Error: TitleManager is not initialized.");
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
            // Assign the player's specific scoreboard
            if (scoreboardManager != null) {
                scoreboardManager.createScoreboardForPlayer(player);
                scoreboardManager.updatePlayerScoreboard(player);
                Scoreboard playerScoreboard = scoreboardManager.getPlayerScoreboard(player);
                if (playerScoreboard != null) {
                    player.setScoreboard(playerScoreboard);
                } else {
                    Bukkit.getLogger().warning("[MysticRPG] Scoreboard for player " + player.getName() + " is null.");
                }
            }

            // Update display name with current level and prefix
            if (titleManager != null) {
                // Ensure that PlayerDataCache has loaded the player's level
                Bukkit.getScheduler().runTaskLater(plugin, () -> titleManager.updatePlayerDisplayName(player), 1L); // 1 tick delay
            }
        });

        // Handle PlayerQuitEvent
        eventManager.registerEvent(PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            // No need to remove prefix as it's managed by LuckPerms
            // Removed calls to removePrefix and removeTitle
        });
    }

    /**
     * Retrieves the TitleManager instance.
     *
     * @return The TitleManager.
     */
    public TitleManager getTitleManager() {
        return this.titleManager;
    }

    /**
     * Retrieves the ScoreboardManager instance.
     *
     * @return The ScoreboardManager.
     */
    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }
}
