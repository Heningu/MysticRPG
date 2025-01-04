package eu.xaru.mysticrpg.player.titles;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class PlayerTitleModule implements IBaseModule {

    private PlayerTitleManager titleManager;

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        StatsModule statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);

        if (saveModule == null || statsModule == null) {
            DebugLogger.getInstance().severe("SaveModule or StatsModule not found. PlayerTitleModule cannot function.");
            return;
        }

        PlayerDataCache dataCache = PlayerDataCache.getInstance();
        PlayerStatsManager statsManager = statsModule.getStatsManager();

        this.titleManager = new PlayerTitleManager(dataCache, statsManager);

        registerCommands();

        // Ensure default title on first join
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();
                titleManager.ensureDefaultTitle(player);
            }
        }, JavaPlugin.getProvidingPlugin(getClass()));

       //  DebugLogger.getInstance().log(Level.INFO, "PlayerTitleModule initialized successfully.", 0);
    }

    private void registerCommands() {
        // /titles list and /titles equip already exist
        // Add /titles give <player> <titleId>
        CommandAPICommand titlesBase = new CommandAPICommand("titles")
                .withPermission("mysticrpg.titles.use")
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            titleManager.listPlayerTitles(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("equip")
                        .withArguments(new StringArgument("titleName"))
                        .executesPlayer((player, args) -> {
                            String titleName = (String) args.get(0);
                            if (titleManager.equipTitle(player, titleName)) {
                                player.sendMessage(ChatColor.GREEN + "You equipped the title: " + titleName);
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("give")
                        .withPermission("mysticrpg.titles.admin")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new StringArgument("titleId"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            String titleId = (String) args.get("titleId");
                            if (titleManager.unlockTitle(target, titleId)) {
                                sender.sendMessage(ChatColor.GREEN + "Unlocked title '" + titleId + "' for " + target.getName());
                            } else {
                                sender.sendMessage(ChatColor.RED + "Failed to unlock title '" + titleId + "' for " + target.getName());
                            }
                        })
                )
                .executesPlayer((player, args) -> {
                    player.sendMessage(ChatColor.YELLOW + "/titles list - to list your titles");
                    player.sendMessage(ChatColor.YELLOW + "/titles equip <title> - to equip a title");
                    player.sendMessage(ChatColor.YELLOW + "/titles give <player> <titleId> - to give a title to a player (admin)");
                });

        titlesBase.register();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, StatsModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public PlayerTitleManager getTitleManager() {
        return titleManager;
    }
}
