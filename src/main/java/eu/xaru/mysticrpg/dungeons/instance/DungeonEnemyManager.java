package eu.xaru.mysticrpg.dungeons.instance;

import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.CustomMobModule;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig.MobSpawnPoint;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DungeonEnemyManager {

    private final JavaPlugin plugin;
    private final DungeonInstance instance;
    private final DungeonConfig config;
    
    private final MobManager mobManager;
    private final List<CustomMobInstance> spawnedMobs;

    public DungeonEnemyManager(JavaPlugin plugin, DungeonInstance instance, DungeonConfig config) {
        this.plugin = plugin;
        this.instance = instance;
        this.config = config;
 
        this.spawnedMobs = new ArrayList<>();

        // Get your existing MobManager instance from CustomMobModule
        CustomMobModule customMobModule = ModuleManager.getInstance().getModuleInstance(CustomMobModule.class);
        if (customMobModule == null) {
            throw new IllegalStateException("CustomMobModule is not loaded. DungeonEnemyManager requires CustomMobModule.");
        }
        this.mobManager = customMobModule.getMobManager();
    }

    public void spawnMobs() {
        for (MobSpawnPoint spawnPoint : config.getMobSpawnPoints()) {
            Location location = spawnPoint.getLocation().clone();
            location.setWorld(instance.getInstanceWorld());

            // Get the custom mob configuration
            CustomMob customMob = mobManager.getMobConfigurations().get(spawnPoint.getMobId());
            if (customMob == null) {
                DebugLogger.getInstance().log(Level.WARNING, "Custom mob with ID '" + spawnPoint.getMobId() + "' not found.", 0);
                continue;
            }

            // Spawn the custom mob at the location
            CustomMobInstance mobInstance = mobManager.spawnMobAtLocation(customMob, location);

            // Keep track of spawned mobs for checking
            if (mobInstance != null) {
                spawnedMobs.add(mobInstance);
            }
        }
    }

    public boolean areAllMonstersDefeated() {
        for (CustomMobInstance mobInstance : spawnedMobs) {
            if (!mobInstance.getEntity().isDead()) {
                return false;
            }
        }
        return true;
    }
}
