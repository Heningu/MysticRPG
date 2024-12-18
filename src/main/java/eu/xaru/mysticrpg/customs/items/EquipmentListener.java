package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.sets.ItemSet;
import eu.xaru.mysticrpg.customs.items.sets.SetManager;
import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.logging.Level;

public class EquipmentListener implements Listener {

    private final PlayerStatsManager statsManager;

    public EquipmentListener(PlayerStatsManager statsManager) {
        this.statsManager = statsManager;
        Bukkit.getPluginManager().registerEvents(this, MysticCore.getInstance());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Bukkit.getScheduler().runTaskLater(MysticCore.getInstance(), () -> recalculatePlayerStats((Player) event.getWhoClicked()), 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            recalculatePlayerStats((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(MysticCore.getInstance(), () -> recalculatePlayerStats(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.getPlayer() != null) {
            Bukkit.getScheduler().runTaskLater(MysticCore.getInstance(), () -> recalculatePlayerStats(event.getPlayer()), 1L);
        }
    }

    private void recalculatePlayerStats(Player player) {
        DebugLogger.getInstance().log(Level.INFO, "Recalculating stats for " + player.getName());
        PlayerStats stats = statsManager.loadStats(player);
        stats.clearTempStats();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        DebugLogger.getInstance().log(Level.INFO, "MainHand: " + (mainHand != null ? mainHand.getType() : "null"));
        DebugLogger.getInstance().log(Level.INFO, "OffHand: " + (offHand != null ? offHand.getType() : "null"));
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            DebugLogger.getInstance().log(Level.INFO, "Armor Slot " + i + ": " + (piece != null ? piece.getType() : "null"));
        }

        int setCount = 0;
        String setId = null;

        applyItemAttributes(mainHand, stats);
        applyItemAttributes(offHand, stats);

        for (ItemStack piece : armor) {
            applyItemAttributes(piece, stats);
            if (piece != null && CustomItemUtils.isCustomItem(piece)) {
                NamespacedKey setKey = new NamespacedKey(MysticCore.getInstance(), "custom_item_set");
                if (piece.getItemMeta() != null && piece.getItemMeta().getPersistentDataContainer().has(setKey, PersistentDataType.STRING)) {
                    String sId = piece.getItemMeta().getPersistentDataContainer().get(setKey, PersistentDataType.STRING);
                    if (sId != null) {
                        if (setId == null) {
                            setId = sId;
                            setCount = 1;
                        } else if (setId.equals(sId)) {
                            setCount++;
                        }
                    }
                }
            }
        }

        if (setId != null) {
            DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " has set " + setId + " with count " + setCount);
            ItemSet itemSet = SetManager.getInstance().getSet(setId);
            if (itemSet != null) {
                int maxThreshold = 0;
                for (Integer threshold : itemSet.getPieceBonuses().keySet()) {
                    if (setCount >= threshold && threshold > maxThreshold) {
                        maxThreshold = threshold;
                    }
                }
                if (maxThreshold > 0) {
                    Map<StatType, Double> bonuses = itemSet.getPieceBonuses().get(maxThreshold);
                    if (bonuses != null) {
                        for (Map.Entry<StatType, Double> bonus : bonuses.entrySet()) {
                            double base = stats.getBaseStat(bonus.getKey());
                            double addition = base * bonus.getValue();
                            DebugLogger.getInstance().log(Level.INFO, "Applying set bonus: " + bonus.getKey() + " + " + addition);
                            stats.addTempStat(bonus.getKey(), addition);
                        }
                    }
                }
            }
        }

        statsManager.saveStats(player, stats);
        DebugLogger.getInstance().log(Level.INFO, "Recalculate done for " + player.getName());
    }

    private void applyItemAttributes(ItemStack item, PlayerStats stats) {
        if (item == null || item.getType() == Material.AIR) return;
        if (!CustomItemUtils.isCustomItem(item)) {
            DebugLogger.getInstance().log(Level.INFO, "applyItemAttributes: " + item.getType() + " is not a custom item.");
            return;
        }

        Map<StatType, Double> itemStats = CustomItemUtils.getItemStats(item);
        for (Map.Entry<StatType, Double> entry : itemStats.entrySet()) {
            DebugLogger.getInstance().log(Level.INFO, "applyItemAttributes: Adding " + entry.getKey() + " = " + entry.getValue());
            stats.addTempStat(entry.getKey(), entry.getValue());
        }
    }
}
