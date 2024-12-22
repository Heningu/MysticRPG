package eu.xaru.mysticrpg.dungeons.lobby;

import eu.xaru.mysticrpg.dungeons.DungeonManager;
import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import eu.xaru.mysticrpg.dungeons.gui.DungeonLobbyGUI;
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

    public String getDungeonId() {
        return dungeonId;
    }

    public DungeonConfig getConfig() {
        return config;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public int getMaxPlayers() {
        return config.getMaxPlayers();
    }

    public boolean isFull() {
        return players.size() >= config.getMaxPlayers();
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            updateLobbyGUI();
        } else {
            // Already in lobby, just re-open GUI
            DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
            lobbyGUI.open(player, this);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        readyPlayers.remove(player.getUniqueId());
        if (players.isEmpty()) {
            lobbyManager.removeLobby(lobbyId);
            System.out.println("Lobby " + lobbyId + " is empty and has been removed.");
        } else {
            updateLobbyGUI();
        }
    }

    public void setReady(UUID playerUUID, boolean ready) {
        if (ready) {
            readyPlayers.add(playerUUID);
        } else {
            readyPlayers.remove(playerUUID);
        }
        updateLobbyGUI();
    }

    public boolean isReady(UUID playerUUID) {
        return readyPlayers.contains(playerUUID);
    }

    public boolean allPlayersReady() {
        return !players.isEmpty()
                && readyPlayers.size() == players.size()
                && players.size() >= config.getMinPlayers();
    }

    public void startDungeon() {
        // Close GUI for all
        for (Player player : players) {
            player.closeInventory();
        }
        dungeonManager.createInstance(dungeonId, getPlayerUUIDs());
        lobbyManager.removeLobby(lobbyId);
    }

    private List<UUID> getPlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : players) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    private void updateLobbyGUI() {
        DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
        for (Player player : players) {
            lobbyGUI.open(player, this);
        }
    }

    /**
     * Gets the creator (the first player who joined the lobby).
     * Returns null if no players in the lobby.
     */
    public Player getCreator() {
        return players.isEmpty() ? null : players.get(0);
    }
}
