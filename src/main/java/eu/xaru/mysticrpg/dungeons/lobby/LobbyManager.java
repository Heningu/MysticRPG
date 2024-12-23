package eu.xaru.mysticrpg.dungeons.lobby;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class LobbyManager {

    private final DungeonManager dungeonManager;
    private final Map<String, DungeonLobby> activeLobbies;

    public LobbyManager(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.activeLobbies = new HashMap<>();
    }

    public DungeonLobby getOrCreateLobby(String dungeonId, Player player) {
        DungeonLobby lobby = new DungeonLobby(dungeonId, dungeonManager, this);
        lobby.addPlayer(player);
        activeLobbies.put(lobby.getLobbyId(), lobby);

        // Log that a new lobby was created
        DebugLogger.getInstance().log(Level.INFO,
                "Lobby " + lobby.getLobbyId() + " created for dungeon " + dungeonId,
                0);

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
            if (lobby.getDungeonId().equals(dungeonId) && !lobby.isFull()) {
                return lobby;
            }
        }
        return null;
    }

    public List<DungeonLobby> getActiveLobbiesForDungeon(String dungeonId) {
        List<DungeonLobby> result = new ArrayList<>();
        for (DungeonLobby lobby : activeLobbies.values()) {
            if (lobby.getDungeonId().equals(dungeonId)) {
                result.add(lobby);
            }
        }
        return result;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }
}
