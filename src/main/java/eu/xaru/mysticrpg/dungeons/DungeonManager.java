// File: eu.xaru.mysticrpg.dungeons.DungeonManager.java
package eu.xaru.mysticrpg.dungeons;

import eu.xaru.mysticrpg.dungeons.commands.DungeonCommand;
import eu.xaru.mysticrpg.dungeons.commands.DungeonSetupCommand;
import eu.xaru.mysticrpg.dungeons.commands.DungeonSetupCommands;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.gui.DungeonLobbyGUI;
import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;
import eu.xaru.mysticrpg.dungeons.lobby.LobbyManager;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupListener;
import eu.xaru.mysticrpg.dungeons.setup.DungeonSetupManager;
import eu.xaru.mysticrpg.ui.UIModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DungeonManager {

    private final JavaPlugin plugin;
    private final Map<UUID, DungeonInstance> activeInstances;
    private final LobbyManager lobbyManager;
    private final DungeonConfigManager configManager;
    private final DungeonSetupManager setupManager;
    private final DungeonModule dungeonModule;
    private final DungeonLobbyGUI lobbyGUI;

    public DungeonManager(JavaPlugin plugin, DungeonModule dungeonModule) {
        this.plugin = plugin;
        this.dungeonModule = dungeonModule;
        this.activeInstances = new ConcurrentHashMap<>();
        this.configManager = new DungeonConfigManager(plugin);
        this.lobbyManager = new LobbyManager(this);
        this.setupManager = new DungeonSetupManager(plugin, configManager);
        this.lobbyGUI = new DungeonLobbyGUI(lobbyManager);

        new DungeonCommand(this);
        new DungeonSetupCommand(this, setupManager);
        new DungeonSetupCommands(setupManager);

        new DungeonSetupListener(setupManager, plugin);
        new DungeonEventHandler(plugin, this);

        // We no longer register DungeonLobbyGUI as a Listener in code
        // because it uses InvUI, which doesn't require manual event registration.
    }

    public void start() {
        configManager.loadConfigs();
        DebugLogger.getInstance().log(Level.INFO, "DungeonManager started.", 0);
    }

    public void stop() {
        // Stop all active instances
        for (DungeonInstance instance : activeInstances.values()) {
            instance.stop();
        }
        activeInstances.clear();
        DebugLogger.getInstance().log(Level.INFO, "DungeonManager stopped.", 0);
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

    public DungeonLobbyGUI getLobbyGUI() {
        return lobbyGUI;
    }

    public void createInstance(String dungeonId, List<UUID> playerUUIDs) {
        DungeonInstance instance = new DungeonInstance(plugin, dungeonId, playerUUIDs, configManager, this);
        activeInstances.put(instance.getInstanceId(), instance);
        instance.start();

        // Notify scoreboard that players have entered a dungeon
        UIModule uiModule = eu.xaru.mysticrpg.managers.ModuleManager.getInstance().getModuleInstance(UIModule.class);
        if (uiModule != null) {
            for (UUID uuid : playerUUIDs) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    // The scoreboard manager now handles dungeon display internally
                    uiModule.getScoreboardManager().updatePlayerScoreboard(p);
                }
            }
        }
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
            // Before removing the instance, revert their scoreboards to normal
            UIModule uiModule = eu.xaru.mysticrpg.managers.ModuleManager.getInstance().getModuleInstance(UIModule.class);
            if (uiModule != null) {
                for (Player p : instance.getPlayersInInstance()) {
                    uiModule.getScoreboardManager().updatePlayerScoreboard(p);
                }
            }

            removeInstance(instance.getInstanceId());
            DebugLogger.getInstance().log(Level.INFO, "Dungeon instance " + instance.getInstanceId() + " removed due to no players.", 0);
        }
    }

    public DungeonInstance getInstanceByWorld(World world) {
        for (DungeonInstance instance : activeInstances.values()) {
            if (instance.getInstanceWorld() != null && instance.getInstanceWorld().equals(world)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Called when a single player leaves the instance (but instance might remain).
     */
    public void playerLeaveInstance(DungeonInstance instance, Player player) {
        // Player leaving dungeon: revert scoreboard to the normal one
        UIModule uiModule = eu.xaru.mysticrpg.managers.ModuleManager.getInstance().getModuleInstance(UIModule.class);
        if (uiModule != null) {
            uiModule.getScoreboardManager().updatePlayerScoreboard(player);
        }
    }

    /**
     * Called when the dungeon ends (completed).
     * All players should be reverted back to the main scoreboard.
     */
    public void endDungeon(DungeonInstance instance) {
        UIModule uiModule = eu.xaru.mysticrpg.managers.ModuleManager.getInstance().getModuleInstance(UIModule.class);
        if (uiModule != null) {
            for (Player p : instance.getPlayersInInstance()) {
                uiModule.getScoreboardManager().updatePlayerScoreboard(p);
            }
        }
        instance.endDungeon();
    }
}
