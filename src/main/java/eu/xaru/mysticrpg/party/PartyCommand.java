package eu.xaru.mysticrpg.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PartyCommand implements CommandExecutor {
    private final PartyManager partyManager;
    private final Plugin plugin;

    public PartyCommand(PartyManager partyManager, Plugin plugin) {
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                // Open party menu
                new PartyMenu(player, partyManager, plugin).open();
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                partyManager.createParty(player);
                player.sendMessage("Party created.");
                return true;
            }

            if (args[0].equalsIgnoreCase("invite")) {
                if (args.length < 2) {
                    player.sendMessage("Please specify a player to invite.");
                    return false;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    partyManager.invitePlayer(player, target);
                } else {
                    player.sendMessage("Player not found.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                Party party = partyManager.getParty(player);
                if (party != null && party.getName().equalsIgnoreCase(args[1])) {
                    party.addMember(player);
                    player.sendMessage("You have joined the party.");
                } else {
                    player.sendMessage("Party not found or incorrect name.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("decline")) {
                player.sendMessage("You have declined the party invitation.");
                return true;
            }

            if (args[0].equalsIgnoreCase("leave")) {
                partyManager.leaveParty(player);
                player.sendMessage("You have left the party.");
                return true;
            }
        }
        return false;
    }
}
