// File: eu.xaru.mysticrpg.dungeons.DungeonEventHandler.java
package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
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
import org.bukkit.inventory.ItemStack;
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

                                // Debug: show door details
                                Bukkit.getLogger().info("[DungeonEventHandler] Checking door '"
                                        + door.getDoorId() + "' => trigger='"
                                        + door.getTriggerType() + "', requiredKey='"
                                        + door.getRequiredKeyItemId() + "'");

                                String trigger = door.getTriggerType().toLowerCase();
                                boolean leftClicked  = (event.getAction() == Action.LEFT_CLICK_BLOCK);
                                boolean rightClicked = (event.getAction() == Action.RIGHT_CLICK_BLOCK);

                                // 1) If trigger is leftclick or rightclick
                                if ((leftClicked && trigger.equals("leftclick"))
                                        || (rightClicked && trigger.equals("rightclick"))) {

                                    doorMgr.removeDoor(door.getDoorId());
                                    doorOpener.openDoor(door);
                                    player.sendMessage(ChatColor.GREEN
                                            + "Door '" + door.getDoorId() + "' opened!");
                                }
                                // 2) If trigger is doorkey, check if player is holding the correct item
                                else if (trigger.equals("doorkey")) {
                                    ItemStack inHand = player.getInventory().getItemInMainHand();
                                    String requiredKeyId = door.getRequiredKeyItemId();

                                    // Another debug line
                                    Bukkit.getLogger().info("[DungeonEventHandler] Player in-hand item = "
                                            + (inHand.hasItemMeta() ? inHand.getItemMeta().getDisplayName() : inHand.getType())
                                            + "; door requires key='" + requiredKeyId + "'");

                                    if (CustomItemUtils.isCustomItem(inHand)) {
                                        // The ID of the item the player is holding
                                        String playerItemId = CustomItemUtils.fromItemStack(inHand).getId();
                                        if (playerItemId.equals(requiredKeyId)) {
                                            // Holding the correct key => open the door
                                            doorMgr.removeDoor(door.getDoorId());
                                            doorOpener.openDoor(door);
                                            player.sendMessage(ChatColor.GREEN
                                                    + "Door '" + door.getDoorId() + "' opened using key: " + requiredKeyId);
                                        } else {
                                            player.sendMessage(ChatColor.RED
                                                    + "You need the correct key item (" + requiredKeyId + ") to open this door.");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED
                                                + "You need the correct custom key item to open this door.");
                                    }
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
