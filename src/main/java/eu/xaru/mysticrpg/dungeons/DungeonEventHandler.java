// File: eu/xaru/mysticrpg/dungeons/DungeonEventHandler.java

package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.dungeons.portals.PortalManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            Location to = event.getTo();
            if (to == null) return;

            DungeonConfig config = instance.getConfig();
            Location portalLocation = config.getPortalPos1();
            if (portalLocation == null) {
                return;
            }

            // Check if both locations are in the same world
            if (!to.getWorld().equals(portalLocation.getWorld())) {
                // Optionally log this incident for debugging
                plugin.getLogger().warning("Player " + player.getName() + " is not in the instance world.");
                return;
            }

            double distance = to.distance(portalLocation);
            double portalRadius = 2.0; // Must match the radius in PortalManager

            if (distance <= portalRadius) {
                // Get the PortalManager instance associated with this dungeon
                PortalManager portalManager = instance.getPortalManager();
                if (portalManager != null) {
                    portalManager.handlePlayerEntry(player);
                }
            }
        }
    }
}