//package eu.xaru.mysticrpg.social.party;
//
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.bukkit.event.inventory.InventoryCloseEvent;
//import org.bukkit.event.inventory.InventoryDragEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.SkullMeta;
//import org.bukkit.plugin.Plugin;
//import org.bukkit.scheduler.BukkitRunnable;
//
//import java.util.List;
//
//public class PartyMenu {
//    private final Player player;
//    private final PartyManager partyManager;
//    private final Inventory inventory;
//    private final Plugin plugin;
//
//    public PartyMenu(Player player, PartyManager partyManager, Plugin plugin) {
//        this.player = player;
//        this.partyManager = partyManager;
//        this.inventory = Bukkit.createInventory(null, 54, "Party Menu");
//        this.plugin = plugin;
//
//        initializeItems();
//    }
//
//    private void initializeItems() {
//        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
//        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
//        if (meta != null) {
//            meta.setOwningPlayer(player);
//            meta.setDisplayName(player.getName());
//            Party party = partyManager.getParty(player);
//            meta.setLore(List.of("In a party: " + (party != null ? "YES" : "NO")));
//            playerHead.setItemMeta(meta);
//        }
//        inventory.setItem(0, playerHead);
//
//        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
//        for (int i = 1; i < inventory.getSize(); i++) {
//            inventory.setItem(i, fillerItem);
//        }
//    }
//
//    public void open() {
//        player.openInventory(inventory);
//        startUpdating();
//    }
//
//    // Disable moving items
//    public void onInventoryClick(InventoryClickEvent event) {
//        if (event.getInventory().equals(inventory)) {
//            event.setCancelled(true);
//        }
//    }
//
//    public void onInventoryDrag(InventoryDragEvent event) {
//        if (event.getInventory().equals(inventory)) {
//            event.setCancelled(true);
//        }
//    }
//
//    public void onInventoryClose(InventoryCloseEvent event) {
//        // Optional: handle inventory close if needed
//    }
//
//    // Schedule periodic updates to refresh the GUI
//    private void startUpdating() {
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inventory)) {
//                    initializeItems();
//                } else {
//                    cancel();
//                }
//            }
//        }.runTaskTimer(plugin, 0L, 20L); // Update every second (20 ticks)
//    }
//}
