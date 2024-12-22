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
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;

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

        // Must be in setup mode
        if (!setupManager.isInSetup(player)) {
            return;
        }

        // If it's the off-hand, ignore (Spigot double-fires for off-hand)
        if (event.getHand() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // We only handle RIGHT_CLICK_BLOCK
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block clicked = event.getClickedBlock();
            Location loc = clicked.getLocation();
            DungeonSetupSession session = setupManager.getSession(player);

            // If setting a portal
            if (session.isSettingPortal()) {
                session.setPortalPos1(loc);
                event.setCancelled(true);
                return;
            }

            // If setting a door
            if (session.isSettingDoor()) {
                session.setDoorCorner(loc);
                event.setCancelled(true);
                return;
            }

            // If setting a chest
            if (player.hasMetadata("chestType")) {
                Material clickedType = clicked.getType();
                if (clickedType == Material.CHEST || clickedType == Material.ENDER_CHEST) {
                    String ctype = player.getMetadata("chestType").get(0).asString();
                    session.addChestLocation(ctype, loc);
                    player.sendMessage("Chest of type " + ctype + " was added.");
                    player.removeMetadata("chestType", plugin);
                } else {
                    player.sendMessage(ChatColor.RED + "That is not a valid chest block.");
                }
                event.setCancelled(true);
                return;
            }
        }

        // Cancel any other interactions while in setup
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (setupManager.isInSetup(player)) {
            // discard or end session automatically
            setupManager.discardSession(player);
        }
    }
}
