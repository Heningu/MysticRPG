// File: eu/xaru/mysticrpg/dungeons/lobby/DungeonLobby.java

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
        // Return a copy to prevent external modifications
        return new ArrayList<>(players);
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            // Notify players and update GUI
            updateLobbyGUI();
        } else {
            // If the player is already in the lobby, just open the updated GUI for them
            DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
            lobbyGUI.open(player, this);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        readyPlayers.remove(player.getUniqueId());
        if (players.isEmpty()) {
            // Remove the lobby
            lobbyManager.removeLobby(lobbyId);
            System.out.println("Lobby " + lobbyId + " is empty and has been removed.");
        } else {
            // Notify players and update GUI
            updateLobbyGUI();
        }
    }

    public boolean isFull() {
        return players.size() >= config.getMaxPlayers();
    }

    public int getMaxPlayers() {
        return config.getMaxPlayers();
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
        // Close the lobby GUI for all players
        DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
        for (Player player : players) {
            player.closeInventory();
        }
        dungeonManager.createInstance(dungeonId, getPlayerUUIDs());
        // Remove the lobby
        lobbyManager.removeLobby(lobbyId);
    }

    private void updateLobbyGUI() {
        // Update the lobby GUI for all players
        DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
        for (Player player : players) {
            lobbyGUI.open(player, this);
        }
    }
}
