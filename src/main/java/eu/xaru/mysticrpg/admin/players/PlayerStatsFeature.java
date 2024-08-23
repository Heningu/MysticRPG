//package eu.xaru.mysticrpg.admin.players;
//
//import eu.xaru.mysticrpg.cores.MysticCore;
//import eu.xaru.mysticrpg.admin.features.PlayerFeature;
//import eu.xaru.mysticrpg.storage.old_playerdata;
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.event.inventory.PrepareAnvilEvent;
//import org.bukkit.inventory.AnvilInventory;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//
//import java.util.List;
//
//public class PlayerStatsFeature extends PlayerFeature {
//
//    public PlayerStatsFeature(MysticCore plugin) {
//        super(plugin);
//    }
//
//    @Override
//    public void execute(Player player, Player target) {
//        openPlayerStatsMenu(player, target);
//    }
//
//    public void onPrepareAnvil(PrepareAnvilEvent event) {
//        AnvilInventory inv = event.getInventory();
//        if (inv.getRenameText().isEmpty()) {
//            return;
//        }
//
//        ItemStack item = inv.getItem(0);
//        if (item == null || item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) {
//            return;
//        }
//
//        Player player = (Player) inv.getViewers().get(0);
//        String statName = item.getItemMeta().getDisplayName();
//
//        try {
//            int newValue = Integer.parseInt(inv.getRenameText());
//            Player target = plugin.getAdminMenuMain().getPlayerEditMap().get(player.getUniqueId());
//            old_playerdata data = plugin.getPlayerDataManager().getPlayerData(target);
//            data.setAttribute(statName, newValue);
//            plugin.getPlayerDataManager().save(target);
//            player.sendMessage(statName + " has been updated to " + newValue);
//        } catch (NumberFormatException e) {
//            player.sendMessage("Invalid number format.");
//        }
//
//        Bukkit.getScheduler().runTask(plugin, player::closeInventory);
//        Bukkit.getScheduler().runTask(plugin, () -> openPlayerStatsMenu(player, plugin.getAdminMenuMain().getPlayerEditMap().get(player.getUniqueId())));
//    }
//
//    public void openPlayerStatsMenu(Player player, Player target) {
//        Inventory inventory = Bukkit.createInventory(null, 54, "Player Stats: " + target.getName());
//
//        addItemToInventory(inventory, Material.GOLDEN_APPLE, "HP", target, 0);
//        addItemToInventory(inventory, Material.POTION, "MANA", target, 1);
//        addItemToInventory(inventory, Material.GOLD_INGOT, "LUCK", target, 2);
//        addItemToInventory(inventory, Material.DIAMOND_SWORD, "ATTACK_DAMAGE", target, 3);
//        addItemToInventory(inventory, Material.SHIELD, "TOUGHNESS", target, 4);
//        addItemToInventory(inventory, Material.IRON_SWORD, "ATTACK_DAMAGE_DEX", target, 5);
//        addItemToInventory(inventory, Material.BOOK, "ATTACK_DAMAGE_MANA", target, 6);
//
//        // Fill the rest with white glass panes
//        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
//        ItemMeta fillerMeta = fillerItem.getItemMeta();
//        if (fillerMeta != null) {
//            fillerMeta.setDisplayName(" ");
//            fillerItem.setItemMeta(fillerMeta);
//        }
//
//        for (int i = 7; i < inventory.getSize(); i++) {
//            if (inventory.getItem(i) == null) {
//                inventory.setItem(i, fillerItem);
//            }
//        }
//
//        player.openInventory(inventory);
//        plugin.getAdminMenuMain().getPlayerEditMap().put(player.getUniqueId(), target);
//    }
//
//    private void addItemToInventory(Inventory inventory, Material material, String statName, Player target, int slot) {
//        ItemStack item = new ItemStack(material);
//        ItemMeta meta = item.getItemMeta();
//        if (meta != null) {
//            meta.setDisplayName(statName);
//            old_playerdata data = plugin.getPlayerDataManager().getPlayerData(target);
//            int statValue = data.getAttribute(statName);
//            meta.setLore(List.of("Current Value: " + statValue));
//            item.setItemMeta(meta);
//        }
//        inventory.setItem(slot, item);
//    }
//}
