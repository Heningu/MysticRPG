// File: eu.xaru.mysticrpg.dungeons.lobby.DungeonLobby.java

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

    /**
     * Adds a player to the lobby. If they are already inside, just refresh their GUI.
     */
    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            // Update GUI for all players, including the new one
            updateLobbyGUI();
        } else {
            // Player is already in lobby, just open the updated GUI
            DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
            lobbyGUI.open(player, this);
        }
    }

    /**
     * Removes a player from the lobby and updates the GUI.
     * If the lobby becomes empty, remove it.
     */
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

    /**
     * Sets whether a given player is ready or not, then updates the GUI.
     */
    public void setReady(UUID playerUUID, boolean ready) {
        if (ready) {
            readyPlayers.add(playerUUID);
        } else {
            readyPlayers.remove(playerUUID);
        }
        updateLobbyGUI();
    }

    /**
     * Checks if a given player is ready.
     */
    public boolean isReady(UUID playerUUID) {
        return readyPlayers.contains(playerUUID);
    }

    /**
     * Checks if all players are ready and if minimum player requirements are met.
     */
    public boolean allPlayersReady() {
        return !players.isEmpty() &&
                readyPlayers.size() == players.size() &&
                players.size() >= config.getMinPlayers();
    }

    /**
     * Start the dungeon instance. Called by the GUI start button if all players are ready.
     */
    public void startDungeon() {
        // Close the lobby GUI for all players
        DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
        for (Player player : players) {
            player.closeInventory();
        }
        dungeonManager.createInstance(dungeonId, getPlayerUUIDs());
        // Remove the lobby
        lobbyManager.removeLobby(lobbyId);
    }

    private List<UUID> getPlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : players) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    /**
     * Updates the lobby GUI for all players in the lobby.
     */
    private void updateLobbyGUI() {
        DungeonLobbyGUI lobbyGUI = dungeonManager.getLobbyGUI();
        for (Player player : players) {
            lobbyGUI.open(player, this);
        }
    }

}
