package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig.ChestLocation;
import eu.xaru.mysticrpg.dungeons.loot.LootTable;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.logging.Level;

public class ChestManager {

    private final JavaPlugin plugin;
    private final DungeonInstance instance;
    private final DungeonConfig config;

    private final Random random;
    private final LootTableManager lootTableManager;

    public ChestManager(JavaPlugin plugin, DungeonInstance instance, DungeonConfig config) {
        this.plugin = plugin;
        this.instance = instance;
        this.config = config;

        this.random = new Random();
        this.lootTableManager = instance.getDungeonManager().getConfigManager().getLootTableManager();
    }

    public void placeChests() {
        for (ChestLocation chestLocation : config.getChestLocations()) {
            Location location = chestLocation.getLocation().clone();
            location.setWorld(instance.getInstanceWorld());

            // If chest type not set, default to CHEST
            if (chestLocation.getType() == null
                    || chestLocation.getType() == Material.AIR) {
                chestLocation.setType(Material.CHEST);
            }
            location.getBlock().setType(chestLocation.getType());

            // Get the loot table for this chest
            String lootTableId = chestLocation.getLootTableId();
            LootTable lootTable = lootTableManager.getLootTable(lootTableId);

            if (lootTable == null) {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Loot table with ID '" + lootTableId
                                + "' not found for chest at " + location,
                        0);
                continue; // Skip if no loot table
            }

            // Fill the chest with loot after 1 tick delay to ensure block state is correct
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (location.getBlock().getState() instanceof Chest chest) {
                    Inventory inventory = chest.getBlockInventory();
                    inventory.clear();

                    lootTable.generateLoot().forEach(itemStack -> {
                        int slot = random.nextInt(inventory.getSize());
                        inventory.setItem(slot, itemStack);
                    });
                }
            }, 1L);

            DebugLogger.getInstance().log(Level.INFO,
                    "Placed chest at " + location
                            + " with loot table '" + lootTableId + "'.",
                    0);
        }
    }
}
