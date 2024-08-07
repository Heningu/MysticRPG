package eu.xaru.mysticrpg.modules;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import eu.xaru.mysticrpg.ui.ActionBarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CustomDamageHandler implements Listener {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final ActionBarManager actionBarManager;

    public CustomDamageHandler(Main plugin, PlayerDataManager playerDataManager, ActionBarManager actionBarManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.actionBarManager = actionBarManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            double damage = event.getDamage();
            handleDamage(damaged, damage);
        }
    }

    public void handleDamage(Player damaged, double damage) {
        PlayerData data = playerDataManager.getPlayerData(damaged);
        int newHp = data.getHp() - (int) damage;
        int maxHp = data.getVitality() * 2; // Assuming Vitality * 2 represents max HP

        if (newHp <= 0) {
            damaged.setHealth(0);
            data.setHp(maxHp);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                damaged.spigot().respawn();
                actionBarManager.updateActionBarOnDamage(damaged);
            }, 1L);
        } else {
            data.setHp(newHp);
            playerDataManager.save(damaged);
            actionBarManager.updateActionBarOnDamage(damaged);
        }

        // Optionally provide feedback
        damaged.sendMessage("You took " + damage + " damage!");
        Bukkit.getLogger().info(damaged.getName() + " took " + damage + " damage!");
    }
}
