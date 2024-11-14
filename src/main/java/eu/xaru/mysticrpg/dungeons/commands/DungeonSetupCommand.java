// File: eu/xaru/mysticrpg/dungeons/commands/DungeonSetupCommand.java

package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.Arrays;

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
                                DungeonSetupSession session = setupManager.getSession(player);
                                DungeonConfig config = session.getConfig();
                                // Set the world name
                                String worldName = player.getWorld().getName();
                                if (worldName == null) {
                                    player.sendMessage("Error: Your current world has no name.");
                                    return;
                                }
                                config.setWorldName(worldName);
                                dungeonManager.getConfigManager().saveDungeonConfig(config);
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
                                .withArguments(new dev.jorel.commandapi.arguments.IntegerArgument("amount", 1))
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
                                .withArguments(new dev.jorel.commandapi.arguments.IntegerArgument("amount", 1))
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
                                .withArguments(new StringArgument("difficulty")
                                        .replaceSuggestions(ArgumentSuggestions.strings("Easy", "Normal", "Hard", "Deadly")))
                                .executesPlayer((player, args) -> {
                                    if (setupManager.isInSetup(player)) {
                                        String difficulty = (String) args.get("difficulty");
                                        if (!Arrays.asList("Easy", "Normal", "Hard", "Deadly").contains(difficulty)) {
                                            player.sendMessage("Invalid difficulty level. Allowed values are: Easy, Normal, Hard, Deadly.");
                                            return;
                                        }
                                        DungeonSetupSession session = setupManager.getSession(player);
                                        session.getConfig().setDifficulty(difficulty);
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
                // Add name command
                .withSubcommand(new CommandAPICommand("name")
                        .withArguments(new StringArgument("dungeonName"))
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                String dungeonName = (String) args.get("dungeonName");
                                DungeonSetupSession session = setupManager.getSession(player);
                                session.getConfig().setName(dungeonName);
                                player.sendMessage("Dungeon name set to " + dungeonName);
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                // Add worldvisit command
                .withSubcommand(new CommandAPICommand("worldvisit")
                        .withArguments(new StringArgument("worldName"))
                        .executesPlayer((player, args) -> {
                            String worldName = (String) args.get("worldName");
                            World world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                player.sendMessage("World '" + worldName + "' is not loaded. Attempting to load...");
                                WorldCreator worldCreator = new WorldCreator(worldName);
                                world = worldCreator.createWorld();
                                if (world == null) {
                                    player.sendMessage("World '" + worldName + "' could not be loaded.");
                                    return;
                                }
                            }
                            player.teleport(world.getSpawnLocation());
                            player.sendMessage("Teleported to world: " + worldName);
                        })
                )
                .register();
    }
}
