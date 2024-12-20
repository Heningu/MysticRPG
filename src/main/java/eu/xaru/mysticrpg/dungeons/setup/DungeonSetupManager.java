package eu.xaru.mysticrpg.dungeons.setup;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfigManager;
import eu.xaru.mysticrpg.dungeons.doors.DoorManager;
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

    public DungeonSetupManager(JavaPlugin plugin, DungeonConfigManager configManager) {
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

        // If needed, load existing config or create a new one
        // For simplicity, just create a new session with a new config if doesn't exist
        DungeonSetupSession session = new DungeonSetupSession(player, dungeonId, doorManager);
        setupSessions.put(player.getUniqueId(), session);
        player.sendMessage("Entered setup mode for dungeon: " + dungeonId);
    }

    public void endSetup(Player player) {
        UUID uuid = player.getUniqueId();
        if (!setupSessions.containsKey(uuid)) {
            player.sendMessage("You are not in a setup session.");
            return;
        }

        DungeonSetupSession session = setupSessions.get(uuid);
        configManager.saveDungeonConfig(session.getConfig());

        setupSessions.remove(uuid);
        player.sendMessage("Exited setup mode and config saved.");
    }

    public DungeonSetupSession getSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }

    public boolean isInSetup(Player player) {
        return setupSessions.containsKey(player.getUniqueId());
    }

    public void discardSession(Player player) {
        setupSessions.remove(player.getUniqueId());
        player.sendMessage("Setup session discarded.");
    }

    public DoorManager getDoorManager() {
        return doorManager;
    }

    public void startDoorSetupSession(UUID playerId, String doorId) {
        DungeonSetupSession session = setupSessions.get(playerId);
        if (session == null) {
            return;
        }
        session.startDoorSetup(doorId);
    }
}
