package eu.xaru.mysticrpg.dungeons.setup;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.event.block.Action;

public class DungeonSetupListener implements Listener {

    private final DungeonSetupManager setupManager;
    private final JavaPlugin plugin;

    public DungeonSetupListener(DungeonSetupManager setupManager, JavaPlugin plugin) {
        this.setupManager = setupManager;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (setupManager.isInSetup(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Block breaking is disabled during dungeon setup.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (setupManager.isInSetup(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Block placing is disabled during dungeon setup.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!setupManager.isInSetup(player)) {
            return;
        }

        DungeonSetupSession session = setupManager.getSession(player);

        // If setting portal
        if (session.isSettingPortal()) {
            if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock() != null) {
                Location clickedLocation = event.getClickedBlock().getLocation();
                session.setPortalPos1(clickedLocation);
                event.setCancelled(true);
            }
            return;
        }

        // If setting a door
        if (session.isSettingDoor()) {
            if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock() != null) {
                Location loc = event.getClickedBlock().getLocation();
                session.setDoorCorner(loc);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Please click on a block to set the door corner.");
            }
            return;
        }

        // Handle chest setup
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && (block.getType() == Material.CHEST || block.getType() == Material.ENDER_CHEST)) {
                if (player.hasMetadata("chestType")) {
                    String chestType = player.getMetadata("chestType").get(0).asString();
                    session.addChestLocation(chestType, block.getLocation());
                    player.sendMessage("Chest registered as " + chestType + " chest.");
                    player.removeMetadata("chestType", plugin);
                    event.setCancelled(true);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Interacting with blocks is disabled during dungeon setup.");
                }
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Interacting with blocks is disabled during dungeon setup.");
            }
        } else {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Interacting with blocks is disabled during dungeon setup.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (setupManager.isInSetup(player)) {
            setupManager.discardSession(player);
            // player will see this message next login, or you can log it server side.
        }
    }
}
