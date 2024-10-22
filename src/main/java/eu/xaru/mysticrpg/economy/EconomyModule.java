package eu.xaru.mysticrpg.economy;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class EconomyModule implements IBaseModule {

    private EconomyHelper economyHelper;
    private final JavaPlugin plugin;
    private DebugLoggerModule logger;

    public EconomyModule() {
        // Initialize the plugin instance if required or leave empty
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Assuming MysticCore is the main class
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            PlayerDataCache playerDataCache = saveModule.getPlayerDataCache();
            this.economyHelper = new EconomyHelper(playerDataCache);
            logger.log(Level.INFO, "EconomyModule initialized successfully.", 0);
        } else {
            logger.error("SaveModule is not initialized. EconomyModule cannot function without it.");
            return;
        }

        registerEconomyCommand();
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "EconomyModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "EconomyModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "EconomyModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);  // Depend on SaveModule for player data cache
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void registerEconomyCommand() {
        new CommandAPICommand("economy")
                .withSubcommand(new CommandAPICommand("balance")
                        .executesPlayer((player, args) -> {
                            double balance = economyHelper.getBalance(player);
                            player.sendMessage("Your balance: $" + economyHelper.formatBalance(balance));
                            logger.log("Displayed balance for player: " + player.getName());
                        }))
                .withSubcommand(new CommandAPICommand("send")
                        .withArguments(new PlayerArgument("target"), new DoubleArgument("amount"))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args.get("target");
                            double amount = (double) args.get("amount");

                            if (target != null) {
                                economyHelper.sendMoney(player, target, amount);
                                logger.log("Player " + player.getName() + " attempted to send $" + amount + " to " + target.getName());
                            } else {
                                logger.warn("Target player not found for sending money command.");
                                player.sendMessage(ChatColor.RED + "Target player not found.");
                            }
                        }))
                .register();
    }
    public EconomyHelper getEconomyHelper() {
        return economyHelper;
    }


}
