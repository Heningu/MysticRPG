// File: eu/xaru/mysticrpg/dungeons/commands/DungeonSetupCommand.java

package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DungeonSetupCommand {

    private final DungeonManager dungeonManager;
    private final DungeonSetupManager setupManager;

    public DungeonSetupCommand(DungeonManager dungeonManager, DungeonSetupManager setupManager) {
        this.dungeonManager = dungeonManager;
        this.setupManager = setupManager;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("dungeon")
                .withSubcommand(new CommandAPICommand("setup")
                        .withArguments(new StringArgument("dungeonId"))
                        .executesPlayer((player, args) -> {
                            String dungeonId = (String) args.get("dungeonId");
                            setupManager.startSetup(player, dungeonId);
                            player.sendMessage("Dungeon setup started for ID: " + dungeonId);
                        })
                )
                .withSubcommand(new CommandAPICommand("save")
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                dungeonManager.getConfigManager().saveDungeonConfig(setupManager.getSession(player).getConfig());
                                setupManager.endSetup(player);
                                player.sendMessage("Dungeon configuration saved.");
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                // Add new subcommands for settings
                .withSubcommand(new CommandAPICommand("setting")
                        .withPermission("dungeon.setup")
                        .withSubcommand(new CommandAPICommand("minplayers")
                                .withArguments(new IntegerArgument("amount", 1))
                                .executesPlayer((player, args) -> {
                                    if (setupManager.isInSetup(player)) {
                                        int amount = (int) args.get("amount");
                                        DungeonSetupSession session = setupManager.getSession(player);
                                        session.getConfig().setMinPlayers(amount);
                                        player.sendMessage("Minimum players set to " + amount);
                                    } else {
                                        player.sendMessage("You are not in a setup session.");
                                    }
                                })
                        )
                        .withSubcommand(new CommandAPICommand("maxplayers")
                                .withArguments(new IntegerArgument("amount", 1))
                                .executesPlayer((player, args) -> {
                                    if (setupManager.isInSetup(player)) {
                                        int amount = (int) args.get("amount");
                                        DungeonSetupSession session = setupManager.getSession(player);
                                        session.getConfig().setMaxPlayers(amount);
                                        player.sendMessage("Maximum players set to " + amount);
                                    } else {
                                        player.sendMessage("You are not in a setup session.");
                                    }
                                })
                        )
                        .withSubcommand(new CommandAPICommand("difficulty")
                                .withArguments(new IntegerArgument("difficulty", 1))
                                .executesPlayer((player, args) -> {
                                    if (setupManager.isInSetup(player)) {
                                        int difficulty = (int) args.get("difficulty");
                                        DungeonSetupSession session = setupManager.getSession(player);
                                        session.getConfig().setDifficultyLevel(difficulty);
                                        player.sendMessage("Difficulty level set to " + difficulty);
                                    } else {
                                        player.sendMessage("You are not in a setup session.");
                                    }
                                })
                        )
                )
                // Add setspawn command
                .withSubcommand(new CommandAPICommand("setspawn")
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                DungeonSetupSession session = setupManager.getSession(player);
                                Location location = player.getLocation();
                                session.getConfig().setSpawnLocation(location);
                                player.sendMessage("Spawn location set to your current location.");
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                .register();
    }
}
