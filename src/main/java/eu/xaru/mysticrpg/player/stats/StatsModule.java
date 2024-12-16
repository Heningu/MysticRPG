package eu.xaru.mysticrpg.player.stats;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;

/**
 * The main module to initialize and manage the stats system.
 */
public class StatsModule implements IBaseModule, Listener {

    private PlayerStatsManager statsManager;

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            DebugLogger.getInstance().severe("SaveModule not available. StatsModule cannot function.");
            return;
        }

        PlayerDataCache dataCache = PlayerDataCache.getInstance();
        statsManager = new PlayerStatsManager(dataCache);

        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getProvidingPlugin(getClass()));

        registerCommands();
        DebugLogger.getInstance().log(Level.INFO, "StatsModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Automatically load stats when player joins.
        Player player = event.getPlayer();
        statsManager.loadStats(player);
    }

    private void registerCommands() {
        // Command to show player's stats
        new CommandAPICommand("showstats")
                .withPermission("mysticrpg.stats.view")
                .executesPlayer((player, args) -> {
                    PlayerStats stats = statsManager.loadStats(player);
                    player.sendMessage(ChatColor.GREEN + "=== Your Stats ===");
                    for (Map.Entry<StatType, Double> entry : stats.getAllEffectiveStats().entrySet()) {
                        player.sendMessage(ChatColor.YELLOW + entry.getKey().name() + ": " + ChatColor.WHITE + entry.getValue());
                    }
                })
                .register();

        // Command to increase a stat (for testing)
        new CommandAPICommand("addstat")
                .withPermission("mysticrpg.stats.modify")
                .withArguments(new StringArgument("stat").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    String[] names = new String[StatType.values().length];
                    for (int i = 0; i < StatType.values().length; i++) {
                        names[i] = StatType.values()[i].name();
                    }
                    return names;
                })))
                .withArguments(new IntegerArgument("amount", 1, 100))
                .executesPlayer((player, args) -> {
                    String statName = (String) args.get(0);
                    int amount = (int) args.get(1);
                    try {
                        StatType statType = StatType.valueOf(statName.toUpperCase());
                        statsManager.increaseBaseStat(player, statType, amount);
                        player.sendMessage(ChatColor.GREEN + "Increased " + statType.name() + " by " + amount);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid stat name!");
                    }
                })
                .register();
    }

    public PlayerStatsManager getStatsManager() {
        return statsManager;
    }
}
