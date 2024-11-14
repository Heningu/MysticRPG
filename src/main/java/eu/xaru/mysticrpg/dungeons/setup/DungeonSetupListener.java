// File: eu/xaru/mysticrpg/dungeons/setup/DungeonSetupListener.java

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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

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
        if (setupManager.isInSetup(player)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                if (block != null && (block.getType() == Material.CHEST || block.getType() == Material.ENDER_CHEST)) {
                    if (player.hasMetadata("chestType")) {
                        String chestType = player.getMetadata("chestType").get(0).asString();
                        DungeonSetupSession session = setupManager.getSession(player);
                        session.addChestLocation(chestType, block.getLocation());
                        player.sendMessage("Chest registered as " + chestType + " chest.");
                        player.removeMetadata("chestType", plugin);
                        event.setCancelled(true);
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
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (setupManager.isInSetup(player)) {
            setupManager.discardSession(player);
        }
    }
}
