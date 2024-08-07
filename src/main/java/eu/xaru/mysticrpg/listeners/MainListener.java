package eu.xaru.mysticrpg.listeners;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.admin.AdminMenuMain;
import eu.xaru.mysticrpg.economy.EconomyManager;
import eu.xaru.mysticrpg.leveling.LevelingManager;
import eu.xaru.mysticrpg.modules.CustomDamageHandler;
import eu.xaru.mysticrpg.party.PartyManager;
import eu.xaru.mysticrpg.stats.StatManager;
import eu.xaru.mysticrpg.stats.StatMenu;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

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
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        String inventoryTitle = event.getView().getTitle();
        if (!"Player Stats".equals(inventoryTitle)) return;

        Player player = (Player) event.getWhoClicked();
        plugin.getLogger().info("Player " + player.getName() + " clicked in the Player Stats menu.");

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            plugin.getLogger().info("Clicked item is null or does not have metadata.");
            return;
        }

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        plugin.getLogger().info("Clicked item display name: " + displayName);

        if (displayName.startsWith("Increase ")) {
            plugin.getLogger().info("Passing attribute name to StatManager: " + displayName);
            statManager.increaseAttribute(player, displayName);
            statMenu.openStatMenu(player); // Refresh the inventory to show updated stats
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String inventoryTitle = event.getView().getTitle();
        if ("Player Stats".equals(inventoryTitle)) {
            plugin.getLogger().info("Player is dragging items in the Player Stats menu.");
            event.setCancelled(true); // Prevent item movement
        }
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
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof StatMenu || event.getSource().getHolder() instanceof StatMenu) {
            event.setCancelled(true); // Prevent item movement
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof StatMenu) {
            event.setCancelled(true); // Prevent item dropping
        }
    }
}
