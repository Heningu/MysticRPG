package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupSession;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

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
        LootTableManager lootTableManager = new LootTableManager(plugin);
        new CommandAPICommand("ds")
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
                        // Use the lootTableManager's auto-complete suggestions
                        .withArguments(new StringArgument("lootTableId")
                                .replaceSuggestions(lootTableManager.getLootTableIdSuggestions())
                        )
                        .executesPlayer((player, args) -> {
                            if (setupManager.isInSetup(player)) {
                                String lootTableId = (String) args.get("lootTableId");
                                player.sendMessage(ChatColor.GREEN
                                        + "Please click on a chest to register it with lootTableId: "
                                        + lootTableId);
                                // Store this ID in metadata "lootTableId"
                                player.setMetadata("lootTableId", new FixedMetadataValue(plugin, lootTableId));
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
                            if (!setupManager.isInSetup(player)) {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                                return;
                            }
                            player.sendMessage(ChatColor.GREEN + "Door setup started for Door ID: " + doorId + ". Click two corners (bottom-left then top-right).");
                            setupManager.startDoorSetupSession(player.getUniqueId(), doorId);
                        })
                )
                .withSubcommand(new CommandAPICommand("removedoor")
                        .withArguments(new StringArgument("doorId"))
                        .executesPlayer((player, args) -> {
                            String doorId = (String) args.get("doorId");
                            if (!doorManager.removeDoor(doorId)) {
                                player.sendMessage(ChatColor.RED + "No door found with ID '" + doorId + "'.");
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Door '" + doorId + "' removed.");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("setdoortrigger")
                        .withArguments(new StringArgument("doorId"))
                        .withArguments(new StringArgument("triggerType")
                                .replaceSuggestions(ArgumentSuggestions.strings("leftclick","rightclick","none")))
                        .executesPlayer((player, args) -> {
                            String doorId = (String) args.get("doorId");
                            String triggerType = (String) args.get("triggerType");

                            if (!setupManager.isInSetup(player)) {
                                player.sendMessage(ChatColor.RED + "You are not in a setup session.");
                                return;
                            }
                            DungeonSetupSession session = setupManager.getSession(player);

                            if (doorManager.getDoor(doorId) == null) {
                                player.sendMessage(ChatColor.RED + "No door found with ID '" + doorId + "'.");
                            } else {
                                // This sets door's trigger in memory and updates config
                                doorManager.setDoorTriggerAndSave(
                                        doorId,
                                        triggerType,
                                        session.getConfig(),             // from the session
                                        setupManager.getConfigManager()  // config manager
                                );
                                player.sendMessage(ChatColor.GREEN + "Door '" + doorId
                                        + "' trigger set to " + triggerType + " and saved.");
                            }
                        })
                )
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
