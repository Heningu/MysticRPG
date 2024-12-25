package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * PlayerDataCache manages cached player data, providing efficient access and modification,
 * plus scheduled asynchronous flushing to the DB.
 */
public class PlayerDataCache {

    private static PlayerDataCache instance;

    /**
     * In-memory cache of UUID -> PlayerData
     */
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    /**
     * Track which players have “dirty” data that must be flushed to DB.
     */
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    private final DatabaseManager databaseManager;

    /**
     * How often (in ticks) to flush dirty data in bulk. (1200 ticks = 60s at 20 TPS)
     */
    private static final long FLUSH_PERIOD_TICKS = 1200L;

    private PlayerDataCache(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        // Schedule a repeating async task to flush dirty data
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                eu.xaru.mysticrpg.cores.MysticCore.getInstance(),
                this::flushDirtyData,
                FLUSH_PERIOD_TICKS,
                FLUSH_PERIOD_TICKS
        );

        registerCheckCachedDataCommand();
    }

    /**
     * Initializes the singleton instance. Should be called once.
     */
    public static synchronized void initialize(DatabaseManager dbManager) {
        if (instance == null) {
            instance = new PlayerDataCache(dbManager);
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "PlayerDataCache is already initialized.", 0);
        }
    }

    public static PlayerDataCache getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerDataCache not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Load from DB or create default if none found.
     */
    public void loadPlayerData(UUID playerUUID, Callback<PlayerData> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading data for UUID: " + playerUUID, 0);
        databaseManager.getPlayerRepository().load(playerUUID, new Callback<>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                playerData.ensureMutableCollections();
                cache.put(playerUUID, playerData);
                DebugLogger.getInstance().log(Level.INFO, "Player data loaded/cached for: " + playerUUID, 0);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // If not found in DB, create default
                if (throwable instanceof NoSuchElementException) {
                    PlayerData defaultData = PlayerData.defaultData(playerUUID.toString());
                    defaultData.ensureMutableCollections();
                    // Cache it
                    cache.put(playerUUID, defaultData);
                    markDirty(playerUUID);
                    DebugLogger.getInstance().log(Level.INFO, "Created default data for new player: " + playerUUID, 0);
                    callback.onSuccess(defaultData);
                } else {
                    DebugLogger.getInstance().error("Failed to load data for " + playerUUID, throwable);
                    callback.onFailure(throwable);
                }
            }
        });
    }

    /**
     * Put a new PlayerData in cache, mark dirty, and optionally do immediate save with a callback.
     */
    public void cacheAndMarkDirty(UUID playerUUID, PlayerData data, Callback<Void> callback) {
        cache.put(playerUUID, data);
        markDirty(playerUUID);
        doSave(data, callback);
    }

    /**
     * Mark a player's data as dirty for next scheduled flush.
     */
    public void markDirty(UUID playerUUID) {
        dirtyPlayers.add(playerUUID);
    }

    /**
     * Immediately save data for the given player, plus callback on success/failure.
     */
    public void savePlayerData(UUID playerUUID, Callback<Void> callback) {
        PlayerData data = cache.get(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().error("No data in cache for: " + playerUUID);
            callback.onFailure(new IllegalStateException("No cached data for " + playerUUID));
            return;
        }
        markDirty(playerUUID);
        doSave(data, callback);
    }

    private void doSave(PlayerData data, Callback<Void> callback) {
        databaseManager.getPlayerRepository().save(data, new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                dirtyPlayers.remove(UUID.fromString(data.getUuid()));
                DebugLogger.getInstance().log(Level.INFO, "Async saved data for: " + data.getUuid(), 0);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().error("Failed to save data for: " + data.getUuid(), throwable);
                if (callback != null) {
                    callback.onFailure(throwable);
                }
            }
        });
    }

    /**
     * Periodic flush of all dirty data to DB.
     */
    private void flushDirtyData() {
        if (dirtyPlayers.isEmpty()) {
            return;
        }
        DebugLogger.getInstance().log(Level.INFO, "Flushing " + dirtyPlayers.size() + " dirty entries...", 0);

        List<UUID> snapshot = new ArrayList<>(dirtyPlayers);
        for (UUID uuid : snapshot) {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                doSave(data, null);
            }
        }
    }

    /**
     * Clear a player's data from cache entirely (e.g., on quit).
     */
    public void clearPlayerData(UUID playerUUID) {
        cache.remove(playerUUID);
        dirtyPlayers.remove(playerUUID);
        DebugLogger.getInstance().log(Level.INFO, "Cleared cache for " + playerUUID, 0);
    }

    /**
     * Retrieve cached data if present, else null.
     */
    public PlayerData getCachedPlayerData(UUID playerUUID) {
        return cache.get(playerUUID);
    }

    /**
     * Load by discord ID, store in cache.
     */
    public void loadPlayerDataByDiscordId(long discordId, Callback<PlayerData> callback) {
        databaseManager.getPlayerRepository().loadByDiscordId(discordId, new Callback<>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                playerData.ensureMutableCollections();
                UUID uuid = UUID.fromString(playerData.getUuid());
                cache.put(uuid, playerData);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    // Friend / block methods
    public void addFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            data.getFriends().add(friendUUID.toString());
            markDirty(playerUUID);
        }
    }

    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            data.getFriends().remove(friendUUID.toString());
            markDirty(playerUUID);
        }
    }

    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            data.getFriendRequests().add(requesterUUID.toString());
            markDirty(playerUUID);
        }
    }

    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            data.getFriendRequests().remove(requesterUUID.toString());
            markDirty(playerUUID);
        }
    }

    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        PlayerData data = cache.get(blockerUUID);
        if (data != null) {
            data.getBlockedPlayers().add(toBlockUUID.toString());
            markDirty(blockerUUID);
        }
    }

    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        PlayerData data = cache.get(blockerUUID);
        if (data != null) {
            data.getBlockedPlayers().remove(toUnblockUUID.toString());
            markDirty(blockerUUID);
        }
    }

    public Set<UUID> getAllCachedPlayerUUIDs() {
        return cache.keySet();
    }

    /**
     * Registers /checkCachedData command for debug
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
                        player.sendMessage(Utils.getInstance().$("Attributes: " + playerData.getAttributes()));
                        player.sendMessage(Utils.getInstance().$("Attribute Points: " + playerData.getAttributePoints()));
                        player.sendMessage(Utils.getInstance().$("Unlocked Recipes: " + playerData.getUnlockedRecipes()));
                        player.sendMessage(Utils.getInstance().$("Friend Requests: " + playerData.getFriendRequests()));
                        player.sendMessage(Utils.getInstance().$("Friends: " + playerData.getFriends()));
                        player.sendMessage(Utils.getInstance().$("Blocked Players: " + playerData.getBlockedPlayers()));
                        player.sendMessage(Utils.getInstance().$("Blocking Requests: " + playerData.isBlockingRequests()));
                    } else {
                        player.sendMessage(Utils.getInstance().$("No cached data found for you."));
                        DebugLogger.getInstance().error("No cached data for player: " + playerUUID);
                    }
                })
                .register();
    }
}
