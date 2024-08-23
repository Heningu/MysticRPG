//package eu.xaru.mysticrpg.listeners;
//
//import eu.xaru.mysticrpg.cores.MysticCore;
//import eu.xaru.mysticrpg.admin.AdminMenuMain;
//import eu.xaru.mysticrpg.economy.EconomyHelper;
//import eu.xaru.mysticrpg.social.friends.FriendsMenu;
//import eu.xaru.mysticrpg.player.leveling.LevelingManager;
//import eu.xaru.mysticrpg.player.leveling.LevelingMenu;
//import eu.xaru.mysticrpg.player.CustomDamageHandler;
//import eu.xaru.mysticrpg.social.party.PartyManager;
//import eu.xaru.mysticrpg.player.stats.StatManager;
//import eu.xaru.mysticrpg.player.stats.StatMenu;
//import eu.xaru.mysticrpg.storage.PlayerDataManager;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//import org.bukkit.event.block.BlockBreakEvent;
//import org.bukkit.event.entity.EntityDamageByEntityEvent;
//import org.bukkit.event.entity.EntityDeathEvent;
//import org.bukkit.event.inventory.*;
//import org.bukkit.event.player.PlayerDropItemEvent;
//import org.bukkit.event.player.PlayerExpChangeEvent;
//import org.bukkit.event.player.PlayerJoinEvent;
//import org.bukkit.event.player.PlayerQuitEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.SkullMeta;
//
//public class MainListener implements Listener {
//    private final MysticCore plugin;
//    private final AdminMenuMain adminMenuMain;
//    private final PlayerDataManager playerDataManager;
//    private final LevelingManager levelingManager;
//    private final LevelingMenu levelingMenu;
//    private final CustomDamageHandler customDamageHandler;
//    private final PartyManager partyManager;
//    private final EconomyHelper economyManager;
//    private final StatManager statManager;
//    private final StatMenu statMenu;
//    private final FriendsMenu friendsMenu;
//
//    public MainListener(MysticCore plugin, AdminMenuMain adminMenuMain, PlayerDataManager playerDataManager,
//                        LevelingManager levelingManager, LevelingMenu levelingMenu, CustomDamageHandler customDamageHandler,
//                        PartyManager partyManager, EconomyHelper economyManager,
//                        StatManager statManager, StatMenu statMenu, FriendsMenu friendsMenu) {
//        this.plugin = plugin;
//        this.adminMenuMain = adminMenuMain;
//        this.playerDataManager = playerDataManager;
//        this.levelingManager = levelingManager;
//        this.levelingMenu = levelingMenu;
//        this.customDamageHandler = customDamageHandler;
//        this.partyManager = partyManager;
//        this.economyManager = economyManager;
//        this.statManager = statManager;
//        this.statMenu = statMenu;
//        this.friendsMenu = friendsMenu;
//    }
//
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//        Inventory clickedInventory = event.getClickedInventory();
//        if (clickedInventory == null) return;
//
//        String inventoryTitle = event.getView().getTitle();
//        Player player = (Player) event.getWhoClicked();
//        ItemStack clickedItem = event.getCurrentItem();
//
//        if (clickedItem == null || !clickedItem.hasItemMeta()) {
//            return;
//        }
//
//        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
//
//        // Handle clicks in the Player Stats menu
//        if ("Player Stats".equals(inventoryTitle)) {
//            plugin.getLogger().info("Player " + player.getName() + " clicked in the Player Stats menu.");
//            if (displayName.startsWith("Increase ")) {
//                plugin.getLogger().info("Passing attribute name to StatManager: " + displayName);
//                statManager.increaseAttribute(player, displayName);
//                statMenu.openStatMenu(player); // Refresh the inventory to show updated stats
//            }
//            event.setCancelled(true); // Prevent item movement
//        }
//
//        // Handle clicks in the Friends menu
//        if ("Friends".equals(inventoryTitle)) {
//            event.setCancelled(true);
//            if ("Friend Requests".equals(displayName)) {
//                friendsMenu.openFriendRequestsMenu(player);
//            } else if ("Blocked Players".equals(displayName)) {
//                friendsMenu.openBlockedPlayersMenu(player);
//            } else if ("Block All Incoming Friend Requests".equals(displayName)) {
//                friendsMenu.toggleBlockingRequests(player);
//                friendsMenu.openFriendsMenu(player);
//            } else if ("Next Page".equals(displayName)) {
//                // Handle navigation only if it's not a skull with "No Next Page"
//                if (!(clickedItem.getType() == Material.WITHER_SKELETON_SKULL && "No Next Page".equals(displayName))) {
//                    friendsMenu.openFriendsMenu(player, 1);
//                }
//            } else if ("Previous Page".equals(displayName)) {
//                // Handle navigation only if it's not a skull with "No Previous Page"
//                if (!(clickedItem.getType() == Material.WITHER_SKELETON_SKULL && "No Previous Page".equals(displayName))) {
//                    friendsMenu.openFriendsMenu(player, 0);
//                }
//            }
//        }
//
//        // Handle clicks in the Friend Requests menu
//        if ("Friend Requests".equals(inventoryTitle)) {
//            event.setCancelled(true);
//            if (displayName.startsWith("Back to Friends Menu")) {
//                friendsMenu.openFriendsMenu(player);
//            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
//                friendsMenu.handleFriendRequestClick(player, clickedItem, event.getClick());
//            }
//        }
//
//        // Handle clicks in the Blocked Players menu
//        if ("Blocked Players".equals(inventoryTitle)) {
//            event.setCancelled(true);
//            if (displayName.startsWith("Back to Friends Menu")) {
//                friendsMenu.openFriendsMenu(player);
//            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
//                friendsMenu.handleBlockedPlayerClick(player, clickedItem);
//            }
//        }
//
//        // Handle clicks in the Leveling Menu
//        if ("Leveling Menu".equals(inventoryTitle)) {
//            event.setCancelled(true);
//            int currentPage = getCurrentPageFromLevelingMenu(clickedInventory);
//            if ("Next Page".equals(displayName)) {
//                levelingMenu.openLevelingMenu(player, currentPage + 1);
//            } else if ("Previous Page".equals(displayName)) {
//                levelingMenu.openLevelingMenu(player, Math.max(1, currentPage - 1));
//            }
//        }
//    }
//
//    private int getCurrentPageFromLevelingMenu(Inventory inventory) {
//        ItemStack headItem = inventory.getItem(0);
//        if (headItem != null && headItem.hasItemMeta() && headItem.getItemMeta() instanceof SkullMeta) {
//            String displayName = headItem.getItemMeta().getDisplayName();
//            if (displayName.contains("Level: ")) {
//                int level = Integer.parseInt(displayName.split("Level: ")[1].split(" ")[0]);
//                return (level - 1) / 26 + 1;
//            }
//        }
//        return 1; // Default to the first page if unable to determine
//    }
//
//    @EventHandler
//    public void onInventoryDrag(InventoryDragEvent event) {
//        String inventoryTitle = event.getView().getTitle();
//        if ("Player Stats".equals(inventoryTitle) || "Friends".equals(inventoryTitle) || "Friend Requests".equals(inventoryTitle) || "Blocked Players".equals(inventoryTitle) || "Leveling Menu".equals(inventoryTitle)) {
//            plugin.getLogger().info("Player is dragging items in a protected menu.");
//            event.setCancelled(true); // Prevent item movement
//        }
//    }
//
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        plugin.getPlayerDataManager().getPlayerData(player); // Load or initialize player data
//        plugin.getScoreboardManager().updateScoreboard(player, plugin.getPlayerDataManager().getPlayerData(player)); // Update scoreboard
//        plugin.getActionBarManager().updateActionBar(player); // Update action bar
//    }
//
//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//        plugin.getPlayerDataManager().save(player);
//        partyManager.leaveParty(player); // Handle party leave on player quit
//    }
//
//    @EventHandler
//    public void onEntityDeath(EntityDeathEvent event) {
//        if (event.getEntity().getKiller() != null) {
//            Player killer = (Player) event.getEntity().getKiller();
//            int xp = levelingManager.getXpForEntity(event.getEntityType().name());
//
//            if (partyManager != null && partyManager.isInParty(killer)) {
//                partyManager.shareXp(killer, xp);
//            } else {
//                levelingManager.addXp(killer, xp);
//            }
//            killer.sendMessage("You have gained " + xp + " XP!");
//            event.setDroppedExp(0); // Prevent double XP gain
//        }
//    }
//
//    @EventHandler
//    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//        if (event.getEntity() instanceof Player) {
//            Player damaged = (Player) event.getEntity();
//            double damage = event.getDamage();
//            customDamageHandler.handleDamage(damaged, damage);
//            plugin.getActionBarManager().updateActionBar(damaged); // Update action bar after taking damage
//            damaged.sendMessage("You took " + damage + " damage!");
//        }
//    }
//
//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent event) {
//        Player player = event.getPlayer();
//        int xp = levelingManager.getXpForEntity(event.getBlock().getType().name());
//        if (partyManager != null && partyManager.isInParty(player)) {
//            partyManager.shareXp(player, xp);
//        } else {
//            levelingManager.addXp(player, xp);
//        }
//    }
//
//    @EventHandler
//    public void onPlayerExpChange(PlayerExpChangeEvent event) {
//        event.setAmount(0);
//    }
//
//    @EventHandler
//    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
//        if ((event.getDestination() != null && event.getDestination().getHolder() instanceof StatMenu) ||
//                (event.getSource() != null && event.getSource().getHolder() instanceof StatMenu)) {
//            event.setCancelled(true); // Prevent item movement
//        }
//    }
//
//    @EventHandler
//    public void onPlayerDropItem(PlayerDropItemEvent event) {
//        Player player = event.getPlayer();
//        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory().getHolder() instanceof StatMenu) {
//            event.setCancelled(true); // Prevent item dropping
//        }
//    }
//}
