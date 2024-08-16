package eu.xaru.mysticrpg.friends;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FriendsCommand implements CommandExecutor {
    private final MysticCore plugin;
    private final FriendsManager friendsManager;
    private final FriendsMenu friendsMenu;

    public FriendsCommand(MysticCore plugin, FriendsManager friendsManager, FriendsMenu friendsMenu) {
        this.plugin = plugin;
        this.friendsManager = friendsManager;
        this.friendsMenu = friendsMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return false;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            friendsMenu.openFriendsMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "requests":
                friendsMenu.openFriendRequestsMenu(player);
                return true;

            case "accept":
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("all")) {
                        friendsManager.getFriendRequests(player).forEach(uuid -> {
                            Player senderPlayer = Bukkit.getPlayer(uuid);
                            if (senderPlayer != null) {
                                friendsManager.acceptFriendRequest(player, senderPlayer);
                            }
                        });
                        player.sendMessage("Accepted all friend requests.");
                    } else {
                        Player senderPlayer = Bukkit.getPlayer(args[1]);
                        if (senderPlayer != null) {
                            friendsManager.acceptFriendRequest(player, senderPlayer);
                        } else {
                            player.sendMessage("Player not found.");
                        }
                    }
                    return true;
                }
                break;

            case "decline":
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("all")) {
                        friendsManager.getFriendRequests(player).forEach(uuid -> {
                            Player senderPlayer = Bukkit.getPlayer(uuid);
                            if (senderPlayer != null) {
                                friendsManager.declineFriendRequest(player, senderPlayer);
                            }
                        });
                        player.sendMessage("Declined all friend requests.");
                    } else {
                        Player senderPlayer = Bukkit.getPlayer(args[1]);
                        if (senderPlayer != null) {
                            friendsManager.declineFriendRequest(player, senderPlayer);
                        } else {
                            player.sendMessage("Player not found.");
                        }
                    }
                    return true;
                }
                break;

            case "add":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        friendsManager.sendFriendRequest(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                    return true;
                }
                break;

            case "block":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        friendsManager.blockUser(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                    return true;
                }
                break;

            case "unblock":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        friendsManager.unblockUser(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                    return true;
                }
                break;

            default:
                player.sendMessage("Unknown subcommand.");
                return false;
        }

        return false;
    }
}