package eu.xaru.mysticrpg.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.admin.AdminMenuMain;
import eu.xaru.mysticrpg.admin.players.PlayerBanFeature;
import eu.xaru.mysticrpg.admin.players.PlayerStatsFeature;
import eu.xaru.mysticrpg.economy.EconomyManager;
import eu.xaru.mysticrpg.leveling.LevelingManager;
import eu.xaru.mysticrpg.modules.CustomDamageHandler;
import eu.xaru.mysticrpg.party.PartyManager;
import eu.xaru.mysticrpg.stats.StatManager;
import eu.xaru.mysticrpg.stats.StatMenu;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class MainListener implements Listener {
    private final Main plugin;
    private final AdminMenuMain adminMenuMain;
    private final PlayerDataManager playerDataManager;
    private final LevelingManager levelingManager;
    private final CustomDamageHandler customDamageHandler;
    private final PartyManager partyManager;
    private final EconomyManager economyManager;
    private final StatManager statManager;
    private final StatMenu statMenu;

    public MainListener(Main plugin, AdminMenuMain adminMenuMain, PlayerDataManager playerDataManager,
                        LevelingManager levelingManager, CustomDamageHandler customDamageHandler,
                        PartyManager partyManager, EconomyManager economyManager,
                        StatManager statManager, StatMenu statMenu) {
        this.plugin = plugin;
        this.adminMenuMain = adminMenuMain;
        this.playerDataManager = playerDataManager;
        this.levelingManager = levelingManager;
        this.customDamageHandler = customDamageHandler;
        this.partyManager = partyManager;
        this.economyManager = economyManager;
        this.statManager = statManager;
        this.statMenu = statMenu;
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
        partyManager.leaveParty(player); // Handle party leave on player quit
    }

    @EventHandler
    public void onEntityDeathLeveling(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            int xp = event.getDroppedExp();
            levelingManager.addXp(player, xp);
            player.sendMessage("You have gained " + xp + " XP!");
        }
    }

    @EventHandler
    public void onEntityDeathParty(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            int xp = levelingManager.getXpForEntity(event.getEntityType().name());
            if (partyManager != null && partyManager.isInParty(killer)) {
                partyManager.shareXp(killer, xp);
            } else {
                levelingManager.addXp(killer, xp);
            }
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            double damage = event.getDamage();
            customDamageHandler.handleDamage(damaged, damage);
            plugin.getActionBarManager().updateActionBar(damaged);
            damaged.sendMessage("You took " + damage + " damage!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        int xp = levelingManager.getXpForEntity(event.getBlock().getType().name());
        if (partyManager != null && partyManager.isInParty(player)) {
            partyManager.shareXp(player, xp);
        } else {
            levelingManager.addXp(player, xp);
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String itemName = item.getItemMeta().getDisplayName();
        Player player = (Player) event.getWhoClicked();

        if (itemName.equals("[BACK]")) {
            event.setCancelled(true);
            adminMenuMain.openMainMenu(player);
        } else if (adminMenuMain.isPlayerHead(item)) {
            event.setCancelled(true);
            Player target = plugin.getServer().getPlayerExact(item.getItemMeta().getDisplayName());
            adminMenuMain.openPlayerOptionsMenu(player, target);
        } else if (itemName.equals("[BAN]")) {
            event.setCancelled(true);
            Player target = adminMenuMain.getPlayerEditMap().get(player.getUniqueId());
            new PlayerBanFeature(plugin).execute(player, target);
        } else if (itemName.equals("[STATS]")) {
            event.setCancelled(true);
            Player target = adminMenuMain.getPlayerEditMap().get(player.getUniqueId());
            new PlayerStatsFeature(plugin).openPlayerStatsMenu(player, target);
        }

        if (event.getInventory().getHolder() instanceof StatMenu) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            switch (displayName) {
                case "Increase Vitality":
                    statManager.increaseVitality(player);
                    break;
                case "Increase Intelligence":
                    statManager.increaseIntelligence(player);
                    break;
                case "Increase Dexterity":
                    statManager.increaseDexterity(player);
                    break;
                case "Increase Strength":
                    statManager.increaseStrength(player);
                    break;
                default:
                    break;
            }

            // Refresh the inventory to show updated stats
            statMenu.openStatMenu(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof StatMenu) {
            event.setCancelled(true);
        }
    }
}
