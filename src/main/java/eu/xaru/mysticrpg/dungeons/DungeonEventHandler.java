package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.doors.Door;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import eu.xaru.mysticrpg.dungeons.doors.DoorOpener;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.dungeons.portals.PortalManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonEventHandler implements Listener {

    private final JavaPlugin plugin;
    private final DungeonManager dungeonManager;
    private final DoorOpener doorOpener;

    public DungeonEventHandler(JavaPlugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.doorOpener = new DoorOpener(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance != null) {
            instance.removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (instance == null) return; // Not in a dungeon

        if (event.getClickedBlock() != null) {
            Material blockType = event.getClickedBlock().getType();
            // Only allow normal usage of CHEST/TRAPPED_CHEST
            if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) {

                // Possibly a STONE door
                if (blockType == Material.STONE) {
                    Location blockLoc = event.getClickedBlock().getLocation();
                    DoorManager doorMgr = dungeonManager.getSetupManager().getDoorManager();
                    if (doorMgr != null) {
                        for (Door door : doorMgr.getAllDoors()) {
                            if (door.isWithinDoor(blockLoc)) {
                                String trigger = door.getTriggerType().toLowerCase();
                                boolean leftClicked  = (event.getAction() == Action.LEFT_CLICK_BLOCK);
                                boolean rightClicked = (event.getAction() == Action.RIGHT_CLICK_BLOCK);

                                // If the click matches the door's trigger
                                if ((leftClicked && trigger.equals("leftclick"))
                                        || (rightClicked && trigger.equals("rightclick"))) {

                                    // Remove from memory so it won't re-place as stone
                                    doorMgr.removeDoor(door.getDoorId());

                                    // Remove stone blocks
                                    doorOpener.openDoor(door);

                                    player.sendMessage(ChatColor.GREEN
                                            + "Door '" + door.getDoorId() + "' opened!");
                                }
                            }
                        }
                    }
                }
                // Cancel for any block that isn't CHEST/TRAPPED_CHEST
                event.setCancelled(true);
            }
        } else {
            // No block => cancel
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        DungeonInstance inst = dungeonManager.getInstanceByPlayer(p.getUniqueId());
        if (inst != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        DungeonInstance inst = dungeonManager.getInstanceByPlayer(p.getUniqueId());
        if (inst != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DungeonInstance inst = dungeonManager.getInstanceByPlayer(player.getUniqueId());
        if (inst == null) return;

        Location to = event.getTo();
        if (to == null) return;

        DungeonConfig config = inst.getConfig();
        Location portalPos = config.getPortalPos1();
        if (portalPos == null) return;

        if (!to.getWorld().equals(portalPos.getWorld())) {
            plugin.getLogger().warning("Player " + player.getName()
                    + " is not in the instance world?");
            return;
        }

        double dist = to.distance(portalPos);
        double portalRadius = 2.0;
        if (dist <= portalRadius) {
            PortalManager pm = inst.getPortalManager();
            if (pm != null) {
                pm.handlePlayerEntry(player);
            }
        }
    }
}
