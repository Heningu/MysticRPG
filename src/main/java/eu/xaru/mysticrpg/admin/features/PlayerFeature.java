package eu.xaru.mysticrpg.admin.features;

import eu.xaru.mysticrpg.Main;
import org.bukkit.entity.Player;

public abstract class PlayerFeature {
    protected final Main plugin;

    public PlayerFeature(Main plugin) {
        this.plugin = plugin;
    }

    public abstract void execute(Player player, Player target);

    // Add additional common methods if necessary
}
