package eu.xaru.mysticrpg.dungeons.loot;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LootTableManager {

    private final JavaPlugin plugin;
    private final Map<String, LootTable> lootTables;

    public LootTableManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lootTables = new HashMap<>();
        loadLootTables();
    }

    private void loadLootTables() {
        File lootTableDir = new File(plugin.getDataFolder(), "dungeons/loottables");
        if (!lootTableDir.exists()) {
            lootTableDir.mkdirs();
        }

        File[] files = lootTableDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                LootTable lootTable = LootTable.loadFromFile(file);
                if (lootTable != null) {
                    lootTables.put(lootTable.getId(), lootTable);
                    DebugLogger.getInstance().log(Level.INFO,
                            "Loaded loot table: " + lootTable.getId(), 0);
                }
            }
        }
    }

    public LootTable getLootTable(String id) {
        return lootTables.get(id);
    }

    public void saveLootTable(LootTable lootTable) {
        File lootTableDir = new File(plugin.getDataFolder(), "dungeons/loottables");
        if (!lootTableDir.exists()) {
            lootTableDir.mkdirs();
        }

        File file = new File(lootTableDir, lootTable.getId() + ".yml");
        lootTable.saveToFile(file);
        lootTables.put(lootTable.getId(), lootTable);

        DebugLogger.getInstance().log(Level.INFO,
                "Saved loot table: " + lootTable.getId(), 0);
    }

    public Map<String, LootTable> getAllLootTables() {
        return lootTables;
    }
}
