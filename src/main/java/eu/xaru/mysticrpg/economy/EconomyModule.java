package eu.xaru.mysticrpg.economy;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EconomyModule implements IBaseModule {

    private EconomyHelper economyHelper;
    private final JavaPlugin plugin;

    public EconomyModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            PlayerDataCache playerDataCache = saveModule.getPlayerDataCache();
            this.economyHelper = new EconomyHelper(playerDataCache);
        } else {
            Bukkit.getLogger().severe("SaveModule is not initialized. EconomyModule cannot function without it.");
            return;
        }

        registerCommands();
        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule initialized");
    }

    @Override
    public void start() {
        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule started");
    }

    @Override
    public void stop() {
        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule stopped");
    }

    @Override
    public void unload() {
        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule unloaded");
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);  // Depend on SaveModule for player data cache
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void registerCommands() {
        new CommandAPICommand("economy")
                .withSubcommand(new CommandAPICommand("balance")
                        .executesPlayer((player, args) -> {
                            double balance = economyHelper.getBalance(player);
                            player.sendMessage("Your balance: $" + economyHelper.formatBalance(balance));
                        }))
                .withSubcommand(new CommandAPICommand("send")
                        .withArguments(new PlayerArgument("target"), new DoubleArgument("amount"))
                        .executesPlayer((player, args) -> {
                            // Use the `get` method to retrieve the arguments
                            Player target = (Player) args.get(0);
                            double amount = (double) args.get(1);

                            economyHelper.sendMoney(player, target, amount);
                            player.sendMessage("You sent $" + economyHelper.formatBalance(amount) + " to " + target.getName());
                            target.sendMessage("You received $" + economyHelper.formatBalance(amount) + " from " + player.getName());
                        }))
                .register();
    }

}
