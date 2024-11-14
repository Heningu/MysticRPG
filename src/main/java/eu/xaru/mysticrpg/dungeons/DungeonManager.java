// File: eu/xaru/mysticrpg/dungeons/DungeonManager.java

package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.dungeons.commands.DungeonCommand;
import eu.xaru.mysticrpg.dungeons.commands.DungeonSetupCommand;
import eu.xaru.mysticrpg.dungeons.commands.DungeonSetupCommands;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupListener;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DungeonManager {

    private final JavaPlugin plugin;
    private final DebugLoggerModule logger;
    private final Map<UUID, DungeonInstance> activeInstances;
    private final LobbyManager lobbyManager;
    private final DungeonConfigManager configManager;
    private final DungeonSetupManager setupManager;
    private final DungeonModule dungeonModule;

    public DungeonManager(JavaPlugin plugin, DebugLoggerModule logger, DungeonModule dungeonModule) {
        this.plugin = plugin;
        this.logger = logger;
        this.dungeonModule = dungeonModule;
        this.activeInstances = new ConcurrentHashMap<>();
        this.configManager = new DungeonConfigManager(plugin, logger);
        this.lobbyManager = new LobbyManager(this, logger);
        this.setupManager = new DungeonSetupManager(plugin, logger, configManager);

        // Register commands
        new DungeonCommand(this);
        new DungeonSetupCommand(this, setupManager);
        new DungeonSetupCommands(setupManager);

        // Register event listeners
        new DungeonSetupListener(setupManager, plugin);
        new DungeonEventHandler(plugin, this);
    }

    public void start() {
        configManager.loadConfigs();
        logger.log(Level.INFO, "DungeonManager started.", 0);
    }

    public void stop() {
        activeInstances.values().forEach(DungeonInstance::stop);
        activeInstances.clear();
        logger.log(Level.INFO, "DungeonManager stopped.", 0);
    }

    public DungeonConfigManager getConfigManager() {
        return configManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public DungeonSetupManager getSetupManager() {
        return setupManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public DungeonModule getDungeonModule() {
        return dungeonModule;
    }

    public void createInstance(String dungeonId, List<UUID> playerUUIDs) {
        DungeonInstance instance = new DungeonInstance(plugin, logger, dungeonId, playerUUIDs, configManager, this);
        activeInstances.put(instance.getInstanceId(), instance);
        instance.start();
    }

    public void removeInstance(UUID instanceId) {
        DungeonInstance instance = activeInstances.remove(instanceId);
        if (instance != null) {
            instance.stop();
        }
    }

    public DungeonInstance getInstanceByPlayer(UUID playerUUID) {
        for (DungeonInstance instance : activeInstances.values()) {
            if (instance.containsPlayer(playerUUID)) {
                return instance;
            }
        }
        return null;
    }

    public void checkAndRemoveInstance(DungeonInstance instance) {
        if (instance.getPlayersInInstance().isEmpty()) {
            removeInstance(instance.getInstanceId());
            logger.log(Level.INFO, "Dungeon instance " + instance.getInstanceId() + " removed due to no players.", 0);
        }
    }

    public DungeonInstance getInstanceByWorld(org.bukkit.World world) {
        for (DungeonInstance instance : activeInstances.values()) {
            if (instance.getInstanceWorld().equals(world)) {
                return instance;
            }
        }
        return null;
    }
}
