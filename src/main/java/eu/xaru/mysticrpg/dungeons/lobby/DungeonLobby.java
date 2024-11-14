// File: eu/xaru/mysticrpg/dungeons/lobby/DungeonLobby.java

package eu.xaru.mysticrpg.dungeons.lobby;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DungeonLobby {

    private final String lobbyId;
    private final String dungeonId;
    private final DungeonManager dungeonManager;
    private final DungeonConfig config;
    private final LobbyManager lobbyManager;
    private final List<Player> players;
    private final Set<UUID> readyPlayers;

    public DungeonLobby(String dungeonId, DungeonManager dungeonManager, LobbyManager lobbyManager) {
        this.lobbyId = UUID.randomUUID().toString();
        this.dungeonId = dungeonId;
        this.dungeonManager = dungeonManager;
        this.config = dungeonManager.getConfigManager().getDungeonConfig(dungeonId);
        this.lobbyManager = lobbyManager;
        this.players = new CopyOnWriteArrayList<>();
        this.readyPlayers = new HashSet<>();
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayer(Player player) {
        players.add(player);
        // Notify players
    }

    public void removePlayer(Player player) {
        players.remove(player);
        readyPlayers.remove(player.getUniqueId());
        // Notify players
    }

    public void playerReady(Player player) {
        readyPlayers.add(player.getUniqueId());
        checkAllReady();
    }

    private void checkAllReady() {
        if (readyPlayers.size() >= config.getMinPlayers() && readyPlayers.size() == players.size()) {
            startDungeon();
        }
    }

    private List<UUID> getPlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : players) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    private void startDungeon() {
        dungeonManager.createInstance(dungeonId, getPlayerUUIDs());
        lobbyManager.removeLobby(lobbyId);
    }
}
