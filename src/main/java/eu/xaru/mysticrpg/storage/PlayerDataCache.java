package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.storage.database.IRepository;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * PlayerDataCache manages cached player data, providing efficient access and modification.
 */
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

    /**
     * Loads player data from the database and caches it.
     *
     * @param playerUUID The UUID of the player.
     * @param callback   Callback for success or failure.
     */
    public void loadPlayerData(UUID playerUUID, Callback<PlayerData> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Attempting to load player data for UUID: " + playerUUID, 0);
        databaseManager.getPlayerRepository().load(playerUUID, new Callback<PlayerData>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                cache.put(playerUUID, playerData);
                DebugLogger.getInstance().log(Level.INFO, "Player data loaded and cached for UUID: " + playerUUID, 0);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // If no entity is found, create default data
                if (throwable instanceof NoSuchElementException) {
                    // Create default player data
                    PlayerData defaultData = PlayerData.defaultData(playerUUID.toString());
                    // Ensure collections are mutable
                    defaultData.ensureMutableCollections();

                    // Save the new data to DB
                    databaseManager.getPlayerRepository().save(defaultData, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            cache.put(playerUUID, defaultData);
                            DebugLogger.getInstance().log(Level.INFO, "Created and saved default data for new player UUID: " + playerUUID, 0);
                            callback.onSuccess(defaultData);
                        }

                        @Override
                        public void onFailure(Throwable saveThrowable) {
                            DebugLogger.getInstance().error("Failed to create default player data for UUID: " + playerUUID, saveThrowable);
                            callback.onFailure(saveThrowable);
                        }
                    });
                } else {
                    DebugLogger.getInstance().error("Failed to load player data for UUID: " + playerUUID + ". ", throwable);
                    callback.onFailure(throwable);
                }
            }
        });
    }


    /**
     * Saves player data to the database.
     *
     * @param playerUUID The UUID of the player.
     * @param callback   Callback for success or failure.
     */
    public void savePlayerData(UUID playerUUID, Callback<Void> callback) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            databaseManager.getPlayerRepository().save(playerData, new Callback<Void>() {
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

    /**
     * Clears cached data for a specific player.
     *
     * @param playerUUID The UUID of the player.
     */
    public void clearPlayerData(UUID playerUUID) {
        if (cache.containsKey(playerUUID)) {
            cache.remove(playerUUID);
            DebugLogger.getInstance().log(Level.INFO, "Cleared cache for player UUID: " + playerUUID, 0);
        }
    }

    /**
     * Retrieves cached player data.
     *
     * @param playerUUID The UUID of the player.
     * @return The PlayerData instance, or null if not found.
     */
    public PlayerData getCachedPlayerData(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found when accessing for player UUID: " + playerUUID);
            DebugLogger.getInstance().log(Level.INFO, "Current cache contents: " + cache.toString(), 0);
        }
        return data;
    }



    public void loadPlayerDataByDiscordId(long discordId, Callback<PlayerData> callback) {
        databaseManager.getPlayerRepository().loadByDiscordId(discordId, new Callback<PlayerData>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                UUID playerUUID = UUID.fromString(playerData.getUuid());
                cache.put(playerUUID, playerData);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    /**
     * Adds a friend to a player's friend list.
     *
     * @param playerUUID The UUID of the player.
     * @param friendUUID The UUID of the friend to add.
     */
    public void addFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().add(friendUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Added friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    /**
     * Removes a friend from a player's friend list.
     *
     * @param playerUUID The UUID of the player.
     * @param friendUUID The UUID of the friend to remove.
     */
    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().remove(friendUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Removed friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    /**
     * Adds a friend request to a player's friend requests list.
     *
     * @param playerUUID    The UUID of the player.
     * @param requesterUUID The UUID of the player sending the friend request.
     */
    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().add(requesterUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Added friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    /**
     * Removes a friend request from a player's friend requests list.
     *
     * @param playerUUID    The UUID of the player.
     * @param requesterUUID The UUID of the player whose friend request is to be removed.
     */
    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().remove(requesterUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Removed friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    /**
     * Blocks a player.
     *
     * @param blockerUUID The UUID of the player blocking.
     * @param toBlockUUID The UUID of the player to block.
     */
    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().add(toBlockUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Blocked player " + toBlockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    /**
     * Unblocks a player.
     *
     * @param blockerUUID   The UUID of the player unblocking.
     * @param toUnblockUUID The UUID of the player to unblock.
     */
    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().remove(toUnblockUUID.toString());
            DebugLogger.getInstance().log(Level.INFO, "Unblocked player " + toUnblockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    /**
     * Retrieves all cached player UUIDs.
     *
     * @return A set of all cached player UUIDs.
     */
    public Set<UUID> getAllCachedPlayerUUIDs() {
        return cache.keySet();
    }

    /**
     * Registers the /checkCachedData command for debugging purposes.
     */
    private void registerCheckCachedDataCommand() {
        new dev.jorel.commandapi.CommandAPICommand("checkCachedData")
                .withAliases("checkCache")
                .withPermission("mysticrpg.debug")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    PlayerData playerData = getCachedPlayerData(playerUUID);

                    if (playerData != null) {
                        player.sendMessage(Utils.getInstance().$("Your cached data:"));
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
}
