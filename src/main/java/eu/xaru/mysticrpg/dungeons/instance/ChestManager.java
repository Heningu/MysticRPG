// File: eu/xaru/mysticrpg/dungeons/instance/ChestManager.java

package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig.ChestLocation;
import eu.xaru.mysticrpg.dungeons.loot.LootTable;
import eu.xaru.mysticrpg.dungeons.loot.LootTableManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.logging.Level;

public class ChestManager {

    private final JavaPlugin plugin;
    private final DungeonInstance instance;
    private final DungeonConfig config;
    private final DebugLoggerModule logger;
    private final Random random;
    private final LootTableManager lootTableManager;

    public ChestManager(JavaPlugin plugin, DungeonInstance instance, DungeonConfig config, DebugLoggerModule logger) {
        this.plugin = plugin;
        this.instance = instance;
        this.config = config;
        this.logger = logger;
        this.random = new Random();
        this.lootTableManager = instance.getDungeonManager().getConfigManager().getLootTableManager();
    }

    public void placeChests() {
        for (ChestLocation chestLocation : config.getChestLocations()) {
            Location location = chestLocation.getLocation().clone();
            location.setWorld(instance.getInstanceWorld());

            // Set the block to chest
            location.getBlock().setType(chestLocation.getType());

            // Get the loot table for this chest
            String lootTableId = chestLocation.getLootTableId();
            LootTable lootTable = lootTableManager.getLootTable(lootTableId);

            if (lootTable == null) {
                logger.log(Level.WARNING, "Loot table with ID '" + lootTableId + "' not found for chest at " + location, 0);
                continue; // Skip this chest if loot table is not found
            }

            // Fill the chest with loot
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (location.getBlock().getState() instanceof Chest chest) {
                    Inventory inventory = chest.getBlockInventory();
                    inventory.clear();
                    lootTable.generateLoot().forEach(itemStack -> {
                        int slot = random.nextInt(inventory.getSize());
                        inventory.setItem(slot, itemStack);
                    });
                }
            });

            logger.log(Level.INFO, "Placed chest at " + location + " with loot table '" + lootTableId + "'.", 0);
        }
    }
}
