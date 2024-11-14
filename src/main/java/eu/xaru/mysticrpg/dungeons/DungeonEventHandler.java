// File: eu/xaru/mysticrpg/dungeons/DungeonEventHandler.java

package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonEventHandler implements Listener {

    private final JavaPlugin plugin;
    private final DungeonManager dungeonManager;

    public DungeonEventHandler(JavaPlugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            instance.removePlayer(player);
            // Additional handling
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        DungeonInstance instance = dungeonManager.getInstanceByWorld(event.getEntity().getWorld());
        if (instance != null && instance.areAllMonstersDefeated()) {
            // Dungeon completed
            instance.endDungeon();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            if (event.getClickedBlock() != null) {
                Material blockType = event.getClickedBlock().getType();
                if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true); // Cancel if there's no block
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            event.setCancelled(true); // Prevent block breaking
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            event.setCancelled(true); // Prevent block placing
        }
    }
}
