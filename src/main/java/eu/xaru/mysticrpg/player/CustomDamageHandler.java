package eu.xaru.mysticrpg.player;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import eu.xaru.mysticrpg.ui.ActionBarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class CustomDamageHandler implements Listener {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;
    private final ActionBarManager actionBarManager;

    public CustomDamageHandler(MysticCore plugin, PlayerDataManager playerDataManager, ActionBarManager actionBarManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.actionBarManager = actionBarManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player);

        int maxHp = data.getHp();
        int currentHp = data.getCurrentHp();

        // Ensure currentHp is set correctly when the player joins
        if (currentHp > maxHp || currentHp <= 0) {
            data.setCurrentHp(maxHp);
            playerDataManager.save(player);
            Bukkit.getLogger().info("Player " + player.getName() + " joined. Resetting current HP to max: " + maxHp);
        }

        actionBarManager.updateActionBar(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player);

        // Reset current HP to max HP after respawn
        int maxHp = data.getHp();
        data.setCurrentHp(maxHp);
        playerDataManager.save(player);

        // Update action bar to reflect the reset HP
        actionBarManager.updateActionBar(player);

        // Log for debugging
        Bukkit.getLogger().info("Player " + player.getName() + " respawned. Resetting current HP to max: " + maxHp);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            double damageInHearts = event.getFinalDamage();
            handleDamage(player, damageInHearts);
            event.setCancelled(true); // Prevent default damage handling
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = playerDataManager.getPlayerData(player);

        // Set current HP to 0 upon death
        data.setCurrentHp(0);
        playerDataManager.save(player);

        // Log for debugging purposes
        Bukkit.getLogger().info("Player " + player.getName() + " died. Setting current HP to 0.");
    }

    public void handleDamage(Player player, double damageInHearts) {
        PlayerData data = playerDataManager.getPlayerData(player);
        int currentHp = data.getCurrentHp();
        int damageInHp = (int) Math.round(damageInHearts * 2); // 1 heart = 2 HP
        int newHp = currentHp - damageInHp;

        Bukkit.getLogger().info("Handling damage for player " + player.getName() + ". Damage: " + damageInHp + " HP. Current HP before damage: " + currentHp + "/" + data.getHp());

        if (newHp <= 0) {
            player.setHealth(0.0); // Trigger Minecraft's native death process
            data.setCurrentHp(0);
            Bukkit.getLogger().info("Player " + player.getName() + " will be killed. Setting current HP to 0.");
        } else {
            data.setCurrentHp(newHp);
            player.setHealth(20.0); // Keep hearts visually full
            Bukkit.getLogger().info("Player " + player.getName() + " now has " + newHp + "/" + data.getHp() + " HP after taking damage.");
        }

        playerDataManager.save(player);
        actionBarManager.updateActionBar(player);
    }
}
