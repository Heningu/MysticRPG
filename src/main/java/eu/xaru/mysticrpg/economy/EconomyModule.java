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
import eu.xaru.mysticrpg.utils.Utils;
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
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
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
            logger.log(String.valueOf(Level.SEVERE), "SaveModule is not initialized. EconomyModule cannot function without it.", new Object[0]);
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

    /**
     * Registers economy-related commands using CommandAPI.
     */
    private void registerEconomyCommand() {
        new CommandAPICommand("money")
                .withSubcommand(new CommandAPICommand("balance")
                        .executesPlayer((player, args) -> {
                            double balance = economyHelper.getBalance(player);
                            player.sendMessage(Utils.getInstance().$("Your balance: $" + economyHelper.formatBalance(balance)));
                            logger.log(Level.INFO, "Displayed balance for player: {0}", Integer.parseInt(player.getName()));
                        }))
                .withSubcommand(new CommandAPICommand("send")
                        .withArguments(new PlayerArgument("target"), new DoubleArgument("amount"))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args.get("target");
                            double amount = (double) args.get("amount");

                            if (target != null) {
                                economyHelper.sendMoney(player, target, amount);
                                logger.log(String.valueOf(Level.INFO), "Player {0} attempted to send ${1} to {2}", new Object[]{player.getName(), amount, target.getName()});
                            } else {
                                logger.log("Target player not found for sending money command.", Level.WARNING);
                                player.sendMessage(Utils.getInstance().$("Target player not found."));
                            }
                        }))
                .register();
    }

    /**
     * Getter for EconomyHelper.
     *
     * @return The EconomyHelper instance.
     */
    public EconomyHelper getEconomyHelper() {
        return economyHelper;
    }
}
