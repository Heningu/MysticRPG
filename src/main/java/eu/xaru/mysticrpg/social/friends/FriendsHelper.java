//package eu.xaru.mysticrpg.social.friends;
//
//import dev.jorel.commandapi.CommandAPICommand;
//import dev.jorel.commandapi.arguments.StringArgument;
//import eu.xaru.mysticrpg.storage.PlayerData;
//import eu.xaru.mysticrpg.storage.SaveModule;
//import eu.xaru.mysticrpg.utils.DebugLoggerModule;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.logging.Level;
//import java.util.stream.Collectors;
//
//public class FriendsHelper {
//
//    private final SaveModule saveModule;
//    private final DebugLoggerModule logger;
//    private final FriendsInventory friendsInventory;
//
//    public FriendsHelper(SaveModule saveModule, DebugLoggerModule logger) {
//        this.saveModule = saveModule;
//        this.logger = logger;
//        this.friendsInventory = new FriendsInventory(saveModule);
//        registerFriendsCommand();
//    }
//
//    public void sendFriendRequest(Player sender, Player receiver) {
//        PlayerData receiverData = saveModule.getPlayerData(receiver);
//
//        if (receiverData.blockedPlayers().contains(sender.getUniqueId())) {
//            sender.sendMessage("You cannot send a friend request to " + receiver.getName() + " as you are blocked.");
//            return;
//        }
//
//        receiverData.friendRequests().add(sender.getUniqueId());
//        saveModule.savePlayerData(receiverData);
//
//        sender.sendMessage("Friend request sent to " + receiver.getName());
//        receiver.sendMessage("You have received a friend request from " + sender.getName());
//        logger.log("Friend request sent from " + sender.getName() + " to " + receiver.getName());
//    }
//
//    public void acceptFriendRequest(Player receiver, Player sender) {
//        PlayerData receiverData = saveModule.getPlayerData(receiver);
//        PlayerData senderData = saveModule.getPlayerData(sender);
//
//        UUID senderUUID = sender.getUniqueId();
//
//        if (receiverData.friendRequests().contains(senderUUID)) {
//            receiverData.friendRequests().remove(senderUUID);
//            receiverData.friends().add(senderUUID);
//            senderData.friends().add(receiver.getUniqueId());
//
//            saveModule.savePlayerData(receiverData);
//            saveModule.savePlayerData(senderData);
//
//            receiver.sendMessage("You are now friends with " + sender.getName());
//            sender.sendMessage("You are now friends with " + receiver.getName());
//            logger.log("Friend request accepted between " + receiver.getName() + " and " + sender.getName());
//        } else {
//            receiver.sendMessage("No friend request from " + sender.getName());
//        }
//    }
//
//    public void declineFriendRequest(Player receiver, Player sender) {
//        PlayerData receiverData = saveModule.getPlayerData(receiver);
//        UUID senderUUID = sender.getUniqueId();
//
//        if (receiverData.friendRequests().contains(senderUUID)) {
//            receiverData.friendRequests().remove(senderUUID);
//            saveModule.savePlayerData(receiverData);
//
//            receiver.sendMessage("Friend request from " + sender.getName() + " declined.");
//            sender.sendMessage("Your friend request to " + receiver.getName() + " was declined.");
//            logger.log("Friend request declined by " + receiver.getName() + " from " + sender.getName());
//        } else {
//            receiver.sendMessage("No friend request from " + sender.getName());
//        }
//    }
//
//    public void blockUser(Player blocker, Player toBlock) {
//        PlayerData blockerData = saveModule.getPlayerData(blocker);
//        blockerData.blockedPlayers().add(toBlock.getUniqueId());
//        saveModule.savePlayerData(blockerData);
//
//        blocker.sendMessage("You have blocked " + toBlock.getName());
//        toBlock.sendMessage("You have been blocked by " + blocker.getName());
//        logger.log(blocker.getName() + " blocked " + toBlock.getName());
//    }
//
//    public void unblockUser(Player blocker, Player toUnblock) {
//        PlayerData blockerData = saveModule.getPlayerData(blocker);
//        UUID toUnblockUUID = toUnblock.getUniqueId();
//
//        if (blockerData.blockedPlayers().contains(toUnblockUUID)) {
//            blockerData.blockedPlayers().remove(toUnblockUUID);
//            saveModule.savePlayerData(blockerData);
//
//            blocker.sendMessage("You have unblocked " + toUnblock.getName());
//            toUnblock.sendMessage("You have been unblocked by " + blocker.getName());
//            logger.log(blocker.getName() + " unblocked " + toUnblock.getName());
//        } else {
//            blocker.sendMessage("Player " + toUnblock.getName() + " is not blocked.");
//        }
//    }
//
//    private void registerFriendsCommand() {
//        new CommandAPICommand("friends")
//                .withAliases("friend")
//                .withPermission("mysticrpg.friends")
//                .withArguments(new StringArgument("subcommand")
//                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                            // Provide a list of subcommands as suggestions
//                            return new Suggestions(info.getContext().getInput(), List.of("requests", "accept", "decline", "add", "block", "unblock"));
//                        }))
//                )
//                .withSubcommands(
//                        new CommandAPICommand("requests")
//                                .executesPlayer((player, args) -> {
//                                    friendsHelper.openFriendRequestsMenu(player);
//                                }),
//                        new CommandAPICommand("accept")
//                                .withArguments(new StringArgument("player")
//                                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                                            Player player = (Player) info.sender();
//                                            // Assuming `getFriendRequestNames` is a method that returns a list of player names who sent friend requests
//                                            return new Suggestions(info.getContext().getInput(), friendsHelper.getFriendRequestNames(player));
//                                        }))
//                                )
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        friendsHelper.acceptFriendRequest(player, target);
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                }),
//                        new CommandAPICommand("decline")
//                                .withArguments(new StringArgument("player")
//                                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                                            Player player = (Player) info.sender();
//                                            // Similarly assuming a method for getting friend requests
//                                            return new Suggestions(info.getContext().getInput(), friendsHelper.getFriendRequestNames(player));
//                                        }))
//                                )
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        friendsHelper.declineFriendRequest(player, target);
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                }),
//                        new CommandAPICommand("add")
//                                .withArguments(new StringArgument("player")
//                                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                                            Player player = (Player) info.sender();
//                                            // Assuming `getOnlinePlayerNames` is a method that returns a list of online player names
//                                            return new Suggestions(info.getContext().getInput(), friendsHelper.getOnlinePlayerNames(player));
//                                        }))
//                                )
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        friendsHelper.sendFriendRequest(player, target);
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                }),
//                        new CommandAPICommand("block")
//                                .withArguments(new StringArgument("player")
//                                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                                            Player player = (Player) info.sender();
//                                            return new Suggestions(info.getContext().getInput(), friendsHelper.getOnlinePlayerNames(player));
//                                        }))
//                                )
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        friendsHelper.blockUser(player, target);
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                }),
//                        new CommandAPICommand("unblock")
//                                .withArguments(new StringArgument("player")
//                                        .replaceSuggestions(info -> CompletableFuture.supplyAsync(() -> {
//                                            Player player = (Player) info.sender();
//                                            return new Suggestions(info.getContext().getInput(), friendsHelper.getBlockedPlayerNames(player));
//                                        }))
//                                )
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        friendsHelper.unblockUser(player, target);
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                })
//                )
//                .executesPlayer((player, args) -> {
//                    // Handle the default command or open friends menu
//                    friendsHelper.openFriendsMenu(player);
//                })
//                .register();
//    }
//
//    private void registerFriendsCommand() {
//        new CommandAPICommand("friends")
//                .withAliases("friend")
//                .withPermission("mysticrpg.friends")
//                .executesPlayer((player, args) -> {
//                    // Open the friends menu using FriendsInventory
//                    friendsInventory.openFriendsMenu(player);
//                })
//                .register();
//    }
//
//    private List<String> getOnlinePlayerNames(Player player) {
//        return Bukkit.getOnlinePlayers().stream()
//                .map(Player::getName)
//                .collect(Collectors.toList());
//    }
//
//    private List<String> getFriendRequestNames(Player player) {
//        return saveModule.getPlayerData(player).friendRequests().stream()
//                .map(uuid -> {
//                    Player friend = Bukkit.getPlayer(uuid);
//                    return friend != null ? friend.getName() : null;
//                })
//                .filter(name -> name != null)
//                .collect(Collectors.toList());
//    }
//
//    private List<String> getBlockedPlayerNames(Player player) {
//        return saveModule.getPlayerData(player).blockedPlayers().stream()
//                .map(uuid -> {
//                    Player blocked = Bukkit.getPlayer(uuid);
//                    return blocked != null ? blocked.getName() : null;
//                })
//                .filter(name -> name != null)
//                .collect(Collectors.toList());
//    }
//
//    public void openFriendsMenu(Player player) {
//        // Placeholder for opening the friends menu logic
//        player.sendMessage("Opening friends menu...");
//    }
//
//    public void openFriendRequestsMenu(Player player) {
//        // Placeholder for opening the friend requests menu logic
//        player.sendMessage("Opening friend requests menu...");
//    }
//}
