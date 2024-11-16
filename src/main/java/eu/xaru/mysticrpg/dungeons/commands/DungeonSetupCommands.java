// File: eu/xaru/mysticrpg/dungeons/commands/DungeonSetupCommands.java

package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonSetupCommands {

    private final DungeonSetupManager setupManager;
    private final JavaPlugin plugin;

    public DungeonSetupCommands(DungeonSetupManager setupManager) {
        this.setupManager = setupManager;
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
                            } else {
                                player.sendMessage("You are not in a setup session.");
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
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setchest")
                        .withArguments(new StringArgument("type").replaceSuggestions(ArgumentSuggestions.strings("NORMAL", "ELITE")))
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                String chestType = (String) args.get("type");
                                player.sendMessage("Please click on a chest to register it.");
                                // Store the type temporarily and wait for the player to click on a chest
                                // We'll handle the click event in an event listener
                                player.setMetadata("chestType", new org.bukkit.metadata.FixedMetadataValue(plugin, chestType));
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setfinishportal")
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                DungeonSetupSession session = setupManager.getSession(player);
                                session.startPortalSetup();
                            } else {
                                player.sendMessage("You are not in a setup session.");
                            }
                        })
                )
                .register();
    }
}
