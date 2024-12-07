// File: eu/xaru/mysticrpg/dungeons/setup/DungeonSetupManager.java

package eu.xaru.mysticrpg.dungeons.setup;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonSetupManager {

    private final JavaPlugin plugin;
    
    private final DoorManager doorManager;
    private final DungeonConfigManager configManager;
    private final Map<UUID, DungeonSetupSession> setupSessions;

    public DungeonSetupManager(JavaPlugin plugin,  DungeonConfigManager configManager) {
        this.plugin = plugin;
 
        this.configManager = configManager;
        this.doorManager = new DoorManager(plugin);
        this.setupSessions = new HashMap<>();
    }

    public void startSetup(Player player, String dungeonId) {
        if (setupSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("You are already in a setup session.");
            return;
        }

        DungeonConfig config = configManager.getDungeonConfig(dungeonId);
        if (config == null) {
            // Create a new dungeon config
            config = new DungeonConfig();
            config.setId(dungeonId);
            config.setName("Dungeon " + dungeonId);
            configManager.addDungeonConfig(config);
        }

        DungeonSetupSession session = new DungeonSetupSession(player, dungeonId);
        setupSessions.put(player.getUniqueId(), session);
        player.sendMessage("Entered setup mode for dungeon: " + dungeonId);
    }

    public void endSetup(Player player) {
        setupSessions.remove(player.getUniqueId());
        player.sendMessage("Exited setup mode.");
    }

    public DungeonSetupSession getSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }
    
    

    public boolean isInSetup(Player player) {
        return setupSessions.containsKey(player.getUniqueId());
    }

    public void discardSession(Player player) {

    }

    public DoorManager getDoorManager() {
        return doorManager;
    }

    public void startDoorSetupSession(UUID playerId, String doorId) {
    }
}
