package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class PlayerDataCache {

    private static PlayerDataCache instance;
    private final Map<UUID, PlayerData> cache = Collections.synchronizedMap(new HashMap<>());
    private final DatabaseManager databaseManager;
    

    // Private constructor to prevent direct instantiation
    private PlayerDataCache(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
 
        registerCheckCachedDataCommand();
    }

    /**
     * Initializes the singleton instance. Should be called once during plugin initialization.
     *
     * @param databaseManager The DatabaseManager instance.
                The DebugLoggerModule instance.
     */
    public static synchronized void initialize(DatabaseManager databaseManager) {
        if (instance == null) {
            instance = new PlayerDataCache(databaseManager);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "PlayerDataCache is already initialized.", 0);
        }
    }

    /**
     * Retrieves the singleton instance.
     *
     * @return The PlayerDataCache instance.
     */
    public static PlayerDataCache getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerDataCache is not initialized. Call initialize() first.");
        }
        return instance;
    }

    // Load data into cache when player joins
    public void loadPlayerData(UUID playerUUID, Callback<PlayerData> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Attempting to load player data for UUID: " + playerUUID, 0);
        databaseManager.loadPlayerData(playerUUID, new Callback<PlayerData>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                cache.put(playerUUID, playerData);
                DebugLogger.getInstance().log(Level.INFO, "Player data loaded and cached for UUID: " + playerUUID, 0);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().error("Failed to load player data for UUID: " + playerUUID + ". ", throwable);
                callback.onFailure(throwable);
            }
        });
    }

    // Save cached data to the database when player disconnects
    public void savePlayerData(UUID playerUUID, Callback<Void> callback) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            databaseManager.savePlayerData(playerData, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "Player data saved to database for UUID: " + playerUUID, 0);
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().error("Failed to save player data for UUID: " + playerUUID + ". ", throwable);
                    callback.onFailure(throwable);
                }
            });
        } else {
            DebugLogger.getInstance().error("No cached data found for player UUID: " + playerUUID);
            DebugLogger.getInstance().log(Level.INFO, "Current cache contents: " + cache.toString(), 0);
            callback.onFailure(new IllegalStateException("No cached data found for player UUID: " + playerUUID));
        }
    }

    // Clear player data from cache
    public void clearPlayerData(UUID playerUUID) {
        if (cache.containsKey(playerUUID)) {
            cache.remove(playerUUID);
            DebugLogger.getInstance().log(Level.INFO, "Cleared cache for player UUID: " + playerUUID, 0);
        }
    }

    // Access cached data
    public PlayerData getCachedPlayerData(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found when accessing for player UUID: " + playerUUID);
            DebugLogger.getInstance().log(Level.INFO, "Current cache contents: " + cache.toString(), 0);
        }
        return data;
    }

    // Methods to modify cached player data (friends-related)
    public void addFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().add(friendUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Added friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().remove(friendUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Removed friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().add(requesterUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Added friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().remove(requesterUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Removed friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().add(toBlockUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Blocked player " + toBlockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().remove(toUnblockUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Unblocked player " + toUnblockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    public Set<UUID> getAllCachedPlayerUUIDs() {
        return cache.keySet();
    }

    // Registering the checkCachedData Command
    private void registerCheckCachedDataCommand() {
        new CommandAPICommand("checkCachedData")
                .withAliases("checkCache")
                .withPermission("mysticrpg.debug")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    PlayerData playerData = getCachedPlayerData(playerUUID);

                    if (playerData != null) {
                        player.sendMessage(Utils.getInstance().$("Your cached data:"));
                        player.sendMessage(Utils.getInstance().$("Balance: " + playerData.getBalance()));
                        player.sendMessage(Utils.getInstance().$("XP: " + playerData.getXp()));
                        player.sendMessage(Utils.getInstance().$("Level: " + playerData.getLevel()));
                        player.sendMessage(Utils.getInstance().$("Next Level XP: " + playerData.getNextLevelXP()));
                        player.sendMessage(Utils.getInstance().$("Current HP: " + playerData.getCurrentHp()));
                        player.sendMessage(Utils.getInstance().$("Attributes: " + playerData.getAttributes().toString()));
                        player.sendMessage(Utils.getInstance().$("Attribute Points: " + playerData.getAttributePoints()));
                        player.sendMessage(Utils.getInstance().$("Unlocked Recipes: " + playerData.getUnlockedRecipes().toString()));
                        player.sendMessage(Utils.getInstance().$("Friend Requests: " + playerData.getFriendRequests().toString()));
                        player.sendMessage(Utils.getInstance().$("Friends: " + playerData.getFriends().toString()));
                        player.sendMessage(Utils.getInstance().$("Blocked Players: " + playerData.getBlockedPlayers().toString()));
                        player.sendMessage(Utils.getInstance().$("Blocking Requests: " + playerData.isBlockingRequests()));
                        DebugLogger.getInstance().log(Level.INFO, "Displayed cached data for player UUID: " + playerUUID, 0);
                    } else {
                        player.sendMessage(Utils.getInstance().$("No cached data found for you."));
                        DebugLogger.getInstance().error("No cached data found for player: " + playerUUID);
                    }
                })
                .register();
    }
    /**
     * Retrieves the player's current level.
     *
     * @param uuid The UUID of the player.
     * @return The level of the player, or 1 if not found.
     */
    public int getPlayerLevel(UUID uuid) {
        PlayerData playerData = getCachedPlayerData(uuid);
        if (playerData != null) {
            return playerData.getLevel();
        }
        return 1; // Default level
    }
}
