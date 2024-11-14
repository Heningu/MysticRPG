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

    public DungeonLobby getOrCreateLobby(String dungeonId, Player player) {
        DungeonLobby lobby = findAvailableLobby(dungeonId);
        if (lobby != null) {
            lobby.addPlayer(player);
            logger.log(Level.INFO, "Player " + player.getName() + " added to existing lobby " + lobby.getLobbyId(), 0);
        } else {
            lobby = new DungeonLobby(dungeonId, dungeonManager, this);
            lobby.addPlayer(player);
            activeLobbies.put(lobby.getLobbyId(), lobby);
            logger.log(Level.INFO, "Lobby " + lobby.getLobbyId() + " created for dungeon " + dungeonId, 0);
        }
        return lobby;
    }

    public void removeLobby(String lobbyId) {
        DungeonLobby removedLobby = activeLobbies.remove(lobbyId);
        if (removedLobby != null) {
            System.out.println("Lobby " + lobbyId + " has been removed from active lobbies.");
        }
    }

    public DungeonLobby getLobby(String lobbyId) {
        return activeLobbies.get(lobbyId);
    }

    public DungeonLobby findAvailableLobby(String dungeonId) {
        for (DungeonLobby lobby : activeLobbies.values()) {
            if (lobby.getDungeonId().equals(dungeonId) && lobby.getPlayers().size() < lobby.getMaxPlayers()) {
                return lobby;
            }
        }
        return null;
    }
}
