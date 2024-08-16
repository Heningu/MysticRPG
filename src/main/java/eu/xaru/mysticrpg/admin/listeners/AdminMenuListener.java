/*package eu.xaru.mysticrpg.admin.listeners;

import eu.xaru.mysticrpg.cores.Main;
import eu.xaru.mysticrpg.admin.AdminMenuMain;
import eu.xaru.mysticrpg.admin.players.PlayerBanFeature;
import eu.xaru.mysticrpg.admin.players.PlayerStatsFeature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AdminMenuListener implements Listener {
    private final Main plugin;
    private final AdminMenuMain adminMenuMain;

    public AdminMenuListener(Main plugin, AdminMenuMain adminMenuMain) {
        this.plugin = plugin;
        this.adminMenuMain = adminMenuMain;
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
    }
}
*/