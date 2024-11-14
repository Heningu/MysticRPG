// File: eu/xaru/mysticrpg/dungeons/lobby/LobbyManager.java

package eu.xaru.mysticrpg.dungeons.lobby;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LobbyManager {

    private final DungeonManager dungeonManager;
    private final DebugLoggerModule logger;
    private final Map<String, DungeonLobby> activeLobbies;

    public LobbyManager(DungeonManager dungeonManager, DebugLoggerModule logger) {
        this.dungeonManager = dungeonManager;
        this.logger = logger;
        this.activeLobbies = new HashMap<>();
    }

    public DungeonLobby createLobby(String dungeonId, Player player) {
        DungeonLobby lobby = new DungeonLobby(dungeonId, dungeonManager, this);
        lobby.addPlayer(player);
        activeLobbies.put(lobby.getLobbyId(), lobby);
        logger.log(Level.INFO, "Lobby " + lobby.getLobbyId() + " created for dungeon " + dungeonId, 0);
        return lobby;
    }

    public void removeLobby(String lobbyId) {
        activeLobbies.remove(lobbyId);
    }

    public DungeonLobby getLobby(String lobbyId) {
        return activeLobbies.get(lobbyId);
    }
}
