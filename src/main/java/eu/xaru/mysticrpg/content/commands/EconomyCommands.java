package eu.xaru.mysticrpg.content.commands;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.Permissions;
import eu.xaru.mysticrpg.content.modules.EconomyModule;
import eu.xaru.mysticrpg.hooks.LuckPermsHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EconomyCommands extends BaseCommand implements TabCompleter {

    private final EconomyModule economyModule;
    private final LuckPermsHook luckPermsHook;

    public EconomyCommands(Main plugin) {
        super(plugin);
        this.economyModule = plugin.getManagers().getModuleManager().getEconomyModule();
        this.luckPermsHook = new LuckPermsHook();
    }

    @Override
    public String getCommandName() {
        return "money";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("money")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("balance")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                    return true;
                }

                Player player = (Player) sender;
                if (!luckPermsHook.hasPermission(player, Permissions.MONEY_BALANCE)) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                int balance = economyModule.getBalance(player.getUniqueId());
                player.sendMessage(ChatColor.GOLD + "Your balance: " + balance + " \u20B8");
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                    return true;
                }

                Player senderPlayer = (Player) sender;
                if (!luckPermsHook.hasPermission(senderPlayer, Permissions.MONEY_SEND)) {
                    senderPlayer.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                UUID senderUUID = senderPlayer.getUniqueId();
                int amount;

                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    senderPlayer.sendMessage(ChatColor.RED + "Invalid amount.");
                    return true;
                }

                if (amount <= 0) {
                    senderPlayer.sendMessage(ChatColor.RED + "Amount must be positive.");
                    return true;
                }

                if (!economyModule.hasEnough(senderUUID, amount)) {
                    senderPlayer.sendMessage(ChatColor.RED + "You do not have enough coins.");
                    return true;
                }

                Player receiverPlayer = Bukkit.getPlayer(args[2]);
                if (receiverPlayer == null || !receiverPlayer.isOnline()) {
                    senderPlayer.sendMessage(ChatColor.RED + "Player not found or not online.");
                    return true;
                }

                UUID receiverUUID = receiverPlayer.getUniqueId();
                economyModule.subtractBalance(senderUUID, amount);
                economyModule.addBalance(receiverUUID, amount);

                senderPlayer.sendMessage(ChatColor.GOLD + "You sent " + amount + " \u20B8 to " + receiverPlayer.getName() + ".");
                receiverPlayer.sendMessage(ChatColor.GOLD + "You received " + amount + " \u20B8 from " + senderPlayer.getName() + ".");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Usage: /money <balance|send> [amount] [player]");
            return true;
        }

        if (label.equalsIgnoreCase("amoney")) {
            if (args.length == 3 && args[0].equalsIgnoreCase("setvalue")) {
                if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                String playerName = args[1];
                int value;

                try {
                    value = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid value.");
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                UUID targetUUID = targetPlayer.getUniqueId();
                economyModule.setBalance(targetUUID, value);

                sender.sendMessage(ChatColor.GREEN + "Set " + targetPlayer.getName() + "'s balance to " + value + " \u20B8.");
                return true;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
                if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                String playerName = args[1];
                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                UUID targetUUID = targetPlayer.getUniqueId();
                int balance = economyModule.getBalance(targetUUID);

                sender.sendMessage(ChatColor.GOLD + targetPlayer.getName() + "'s balance: " + balance + " \u20B8.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Usage: /amoney <setvalue|check> [player] [value]");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("money")) {
            if (args.length == 1) {
                return Arrays.asList("balance", "send");
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }

        if (command.getName().equalsIgnoreCase("amoney")) {
            if (args.length == 1) {
                return Arrays.asList("setvalue", "check");
            }

            if (args.length == 2) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }

        return null;
    }
}
