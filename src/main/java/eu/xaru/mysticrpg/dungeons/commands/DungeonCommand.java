// File: eu/xaru/mysticrpg/dungeons/commands/DungeonCommand.java

package eu.xaru.mysticrpg.dungeons.commands;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.gui.DungeonSelectionGUI;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import org.bukkit.entity.Player;

public class DungeonCommand {

    private final DungeonManager dungeonManager;

    public DungeonCommand(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("dungeons")
                .executesPlayer((player, args) -> {
                    DungeonSelectionGUI gui = new DungeonSelectionGUI(dungeonManager);
                    gui.open(player);
                })
                .register();

        new CommandAPICommand("dungeon")
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
                            if (instance != null) {
                                instance.removePlayer(player);
                                player.sendMessage("You have left the dungeon.");
                            } else {
                                player.sendMessage("You are not in a dungeon.");
                            }
                        })
                )
                .register();
    }
}
