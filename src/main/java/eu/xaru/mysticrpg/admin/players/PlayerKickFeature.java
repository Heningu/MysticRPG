package eu.xaru.mysticrpg.admin.players;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.admin.features.PlayerFeature;
import org.bukkit.entity.Player;

public class PlayerKickFeature extends PlayerFeature {

    public PlayerKickFeature(Main plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, Player target) {
        // Logic to kick the player
        target.kickPlayer("You have been kicked by an admin.");
        player.sendMessage(target.getName() + " has been kicked.");
    }
}
