package eu.xaru.mysticrpg.ui;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.economy.BankSubGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
    private final EconomyModule economyModule;
    private final EconomyHelper economyHelper;
    private ScoreboardManager scoreboardManager;

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private ChatFormatter chatFormatter;

    public UIModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);
        this.economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        this.economyHelper = economyModule.getEconomyHelper();
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        LevelModule levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.chatFormatter = new ChatFormatter();
        Bukkit.getPluginManager().registerEvents(this.chatFormatter, plugin);

        if (saveModule != null && levelModule != null) {
            PlayerDataCache playerDataCache = PlayerDataCache.getInstance();
            this.actionBarManager = new ActionBarManager((MysticCore) plugin, playerDataCache);
            this.scoreboardManager = new ScoreboardManager();
            DebugLogger.getInstance().log(Level.INFO, "UIModule initialized successfully.", 0);
        } else {
            DebugLogger.getInstance().severe("[MysticRPG] SaveModule or LevelModule is not initialized. UIModule cannot function without them.");
            return;
        }

        registerEventListeners();

        if (levelModule != null) {
            levelModule.setLevelUpListener(player -> {
                if (scoreboardManager != null) {
                    scoreboardManager.createOrUpdateTeam(player);
                    scoreboardManager.updatePlayerScoreboard(player);
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
        return List.of(SaveModule.class, LevelModule.class, QuestModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void registerCommands() {
        new CommandAPICommand("name")
                .withPermission("mysticrpg.debug")
                .executes((sender, args) -> {
                    sender.sendMessage(ChatColor.RED + "Prefixes are managed automatically through LuckPerms.");
                })
                .register();

        new CommandAPICommand("refreshname")
                .withPermission("mysticrpg.debug")
                .withArguments(new dev.jorel.commandapi.arguments.PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    if (scoreboardManager != null) {
                        scoreboardManager.createOrUpdateTeam(target);
                        scoreboardManager.updatePlayerScoreboard(target);
                        sender.sendMessage(ChatColor.GREEN + "Refreshed scoreboard for " + target.getName());
                        target.sendMessage(ChatColor.GREEN + "Your scoreboard has been refreshed by " + sender.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Error: ScoreboardManager is not initialized.");
                    }
                })
                .register();
    }

    private void registerEventListeners() {
        eventManager.registerEvent(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();
            if (scoreboardManager != null) {
                scoreboardManager.createScoreboardForPlayer(player);
                scoreboardManager.addNewPlayer(player);
            }
        });

        eventManager.registerEvent(PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            if (scoreboardManager != null) {
                scoreboardManager.removePlayer(player);
            }
        });

        eventManager.registerEvent(AsyncPlayerChatEvent.class, event -> {
            Player player = event.getPlayer();
            // Check if this player is waiting for custom amount input in BankSubGUI
            if (BankSubGUI.isAwaitingInput(player.getUniqueId())) {
                event.setCancelled(true); // prevent message from showing in public chat
                String message = event.getMessage();
                // Handle input
                BankSubGUI.handleChatInput(player, message, economyHelper);
            }
        });
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public void setActionBarManager(ActionBarManager actionBarManager) {
        this.actionBarManager = actionBarManager;
    }

    public ChatFormatter getChatFormatter() {
        return chatFormatter;
    }

    public void setChatFormatter(ChatFormatter chatFormatter) {
        this.chatFormatter = chatFormatter;
    }
}
