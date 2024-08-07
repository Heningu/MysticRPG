/*package eu.xaru.mysticrpg.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.leveling.LevelingManager;
import eu.xaru.mysticrpg.modules.CustomDamageHandler;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final LevelingManager levelingManager;
    private final CustomDamageHandler customDamageHandler;

    public PlayerListener(Main plugin, PlayerDataManager playerDataManager, LevelingManager levelingManager, CustomDamageHandler customDamageHandler) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.levelingManager = levelingManager;
        this.customDamageHandler = customDamageHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().getPlayerData(player); // This will load or initialize player data
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().save(player);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            int xp = event.getDroppedExp();
            levelingManager.addXp(player, xp);
            player.sendMessage("You have gained " + xp + " XP!");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            double damage = event.getDamage();

            // Handle custom damage and update stats
            customDamageHandler.handleDamage(damaged, damage);

            // Update action bar and optionally provide feedback
            plugin.getActionBarManager().updateActionBar(damaged);
            damaged.sendMessage("You took " + damage + " damage!");
        }
    }
}*/
