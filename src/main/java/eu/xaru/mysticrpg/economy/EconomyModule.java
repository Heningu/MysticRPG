package eu.xaru.mysticrpg.economy;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
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

    public EconomyModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            PlayerDataCache playerDataCache = PlayerDataCache.getInstance();
            this.economyHelper = new EconomyHelper(playerDataCache);
            DebugLogger.getInstance().log(Level.INFO, "EconomyModule initialized successfully.", 0);
        } else {
            DebugLogger.getInstance().log(String.valueOf(Level.SEVERE), "SaveModule is not initialized. EconomyModule cannot function without it.");
            return;
        }

        registerCommands();
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "EconomyModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "EconomyModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "EconomyModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.HIGH;
    }

    private void registerCommands() {
        new CommandAPICommand("balance")
                .executesPlayer((player, args) -> {
                    int held = economyHelper.getHeldGold(player);
                    int bank = economyHelper.getBankGold(player);
                    player.sendMessage(Utils.getInstance().$(ChatColor.GOLD + "Balance Information:"));
                    player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Held Gold: " + ChatColor.WHITE + held));
                    player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Bank Gold: " + ChatColor.WHITE + bank));
                })
                .register();

        new CommandAPICommand("money")
                .withSubcommand(new CommandAPICommand("balance")
                        .executesPlayer((player, args) -> {
                            int held = economyHelper.getHeldGold(player);
                            int bank = economyHelper.getBankGold(player);
                            player.sendMessage(Utils.getInstance().$(ChatColor.GOLD + "Balance Information:"));
                            player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Held Gold: " + ChatColor.WHITE + held));
                            player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Bank Gold: " + ChatColor.WHITE + bank));
                        }))
                .withSubcommand(new CommandAPICommand("send")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new DoubleArgument("amount"))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) (double) args.get("amount");

                            if (target != null) {
                                if (amount <= 0) {
                                    player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Amount must be positive."));
                                    return;
                                }
                                int senderHeld = economyHelper.getHeldGold(player);
                                if (senderHeld < amount) {
                                    player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You do not have enough gold to send."));
                                    return;
                                }
                                economyHelper.setHeldGold(player, senderHeld - amount);
                                economyHelper.addHeldGold(target, amount);

                                player.sendMessage(Utils.getInstance().$(ChatColor.GREEN + "You sent " + amount + " gold to " + target.getName()));
                                target.sendMessage(Utils.getInstance().$(ChatColor.GREEN + "You received " + amount + " gold from " + player.getName()));
                                DebugLogger.getInstance().log(Level.INFO, "Player {0} sent {1} gold to player {2}",
                                        new Object[]{player.getName(), amount, target.getName()});
                            } else {
                                DebugLogger.getInstance().log("Target player not found for sending money command.", Level.WARNING);
                                player.sendMessage(Utils.getInstance().$("Target player not found."));
                            }
                        }))
                .register();

        new CommandAPICommand("economy")
                .withPermission("economy.admin")
                .withSubcommand(new CommandAPICommand("setheld")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new DoubleArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) (double) args.get("amount");
                            economyHelper.setHeldGold(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Set held gold of " + target.getName() + " to " + amount));
                            target.sendMessage(Utils.getInstance().$("Your held gold has been set to " + amount + " by an administrator."));
                        }))
                .withSubcommand(new CommandAPICommand("setbank")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new DoubleArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) (double) args.get("amount");
                            economyHelper.setBankGold(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Set bank gold of " + target.getName() + " to " + amount));
                            target.sendMessage(Utils.getInstance().$("Your bank gold has been set to " + amount + " by an administrator."));
                        }))
                .withSubcommand(new CommandAPICommand("giveheld")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new DoubleArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) (double) args.get("amount");
                            economyHelper.addHeldGold(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Gave " + amount + " held gold to " + target.getName()));
                            target.sendMessage(Utils.getInstance().$("You received " + amount + " held gold from an administrator."));
                        }))
                .withSubcommand(new CommandAPICommand("givebank")
                        .withArguments(new PlayerArgument("target"))
                        .withArguments(new DoubleArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) (double) args.get("amount");
                            economyHelper.addBankGold(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Gave " + amount + " bank gold to " + target.getName()));
                            target.sendMessage(Utils.getInstance().$("You received " + amount + " bank gold from an administrator."));
                        }))
                .register();
    }

    public EconomyHelper getEconomyHelper() {
        return economyHelper;
    }
}
