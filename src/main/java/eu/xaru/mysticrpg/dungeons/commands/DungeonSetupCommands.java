package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupSession;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DungeonSetupCommands {

    private final DungeonSetupManager setupManager;
    private final JavaPlugin plugin;
    private final DoorManager doorManager;

    public DungeonSetupCommands(DungeonSetupManager setupManager) {
        this.setupManager = setupManager;
        this.doorManager = setupManager.getDoorManager();
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("ds")
                .withSubcommand(new CommandAPICommand("setspawn")
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                DungeonSetupSession session = setupManager.getSession(player);
                                Location location = player.getLocation();
                                session.setSpawnLocation(location);
                                player.sendMessage(ChatColor.GREEN + "Dungeon spawn location set.");
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setmobspawn")
                        .withArguments(new StringArgument("mobId"))
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                String mobId = (String) args.get("mobId");
                                DungeonSetupSession session = setupManager.getSession(player);
                                Location location = player.getLocation();
                                session.addMobSpawnPoint(mobId, location);
                                player.sendMessage(ChatColor.GREEN + "Mob spawn point added for mob ID: " + mobId);
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setchest")
                        .withArguments(new StringArgument("type").replaceSuggestions(ArgumentSuggestions.strings("NORMAL", "ELITE")))
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                String chestType = (String) args.get("type");
                                player.sendMessage(ChatColor.GREEN + "Please click on a chest to register it.");
                                player.setMetadata("chestType", new FixedMetadataValue(plugin, chestType));
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setfinishportal")
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                DungeonSetupSession session = setupManager.getSession(player);
                                session.startPortalSetup();
                                player.sendMessage(ChatColor.GREEN + "Finish portal setup started. Please click on the portal location.");
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setdoor")
                        .withArguments(new StringArgument("doorId"))
                        .executesPlayer((player, args) -> {
                            String doorId = (String) args.get("doorId");
                            if (doorManager.getDoor(doorId) != null) {
                                player.sendMessage(ChatColor.RED + "A door with ID '" + doorId + "' already exists.");
                                return;
                            }
                            player.sendMessage(ChatColor.GREEN + "Door setup started for Door ID: " + doorId + ". Please click on the bottom-left and top-right blocks to define the door area.");
                            UUID playerId = player.getUniqueId();
                            setupManager.startDoorSetupSession(playerId, doorId);
                        })
                )

                // NEW SUBCOMMAND: levelrequirement
                .withSubcommand(new CommandAPICommand("levelrequirement")
                        .withArguments(new IntegerArgument("level"))
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                int levelReq = (int) args.get("level");
                                if (levelReq < 1) levelReq = 1;
                                DungeonSetupSession session = setupManager.getSession(player);
                                session.getConfig().setLevelRequirement(levelReq);
                                player.sendMessage(ChatColor.GREEN + "Level requirement set to " + levelReq + ".");
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                            }
                        })
                )

                .register();
    }
}