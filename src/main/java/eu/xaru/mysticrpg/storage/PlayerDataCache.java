package eu.xaru.mysticrpg.storage;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataCache {

    private static PlayerDataCache instance;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final SaveHelper saveHelper;
    private final DebugLoggerModule logger;

    private PlayerDataCache(SaveHelper saveHelper, DebugLoggerModule logger) {
        this.saveHelper = saveHelper;
        this.logger = logger;
        registerCheckCachedDataCommand();
    }

    public static PlayerDataCache getInstance(SaveHelper saveHelper, DebugLoggerModule logger) {
        if (instance == null) {
            instance = new PlayerDataCache(saveHelper, logger);
        }
        return instance;
    }

    // Load data into cache when player joins
    public void loadPlayerData(UUID playerUUID, Callback<PlayerData> callback) {
        logger.log(Level.INFO, "Attempting to load player data for UUID: " + playerUUID, 0);
        saveHelper.loadPlayer(playerUUID, new Callback<PlayerData>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                cache.put(playerUUID, playerData);
                logger.log(Level.INFO, "Player data loaded and cached for UUID: " + playerUUID, 0);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Failed to load player data for UUID: " + playerUUID + ". " + throwable.getMessage());
                callback.onFailure(throwable);
            }
        });
    }

    // Save cached data to the database when player disconnects
    public void savePlayerData(UUID playerUUID, Callback<Void> callback) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            saveHelper.savePlayer(playerData, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    logger.log(Level.INFO, "Player data saved to database for UUID: " + playerUUID, 0);
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.error("Failed to save player data for UUID: " + playerUUID + ". " + throwable.getMessage());
                    callback.onFailure(throwable);
                }
            });
        } else {
            logger.error("No cached data found for player: " + playerUUID);
            logger.log(Level.INFO, "Current cache contents: " + cache.toString(), 0);
            callback.onFailure(new IllegalStateException("No cached data found for player: " + playerUUID));
        }
    }

    // Clear player data from cache
    public void clearPlayerData(UUID playerUUID) {
        if (cache.containsKey(playerUUID)) {
            cache.remove(playerUUID);
            logger.log(Level.INFO, "Cleared cache for player UUID: " + playerUUID, 0);
        }
    }

    // Access cached data
    public PlayerData getCachedPlayerData(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            logger.log(Level.INFO, "Retrieved cached data for player UUID: " + playerUUID, 0);
        } else {
            logger.error("No cached data found when accessing for player UUID: " + playerUUID);
            logger.log(Level.INFO, "Current cache contents: " + cache.toString(), 0);
        }
        return data;
    }

    // Methods to modify cached player data (friends-related)
    public void addFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().add(friendUUID.toString());
            logger.log(Level.INFO, "Added friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriends().remove(friendUUID.toString());
            logger.log(Level.INFO, "Removed friend " + friendUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().add(requesterUUID.toString());
            logger.log(Level.INFO, "Added friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.getFriendRequests().remove(requesterUUID.toString());
            logger.log(Level.INFO, "Removed friend request from " + requesterUUID + " for player UUID: " + playerUUID, 0);
        }
    }

    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().add(toBlockUUID.toString());
            logger.log(Level.INFO, "Blocked player " + toBlockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.getBlockedPlayers().remove(toUnblockUUID.toString());
            logger.log(Level.INFO, "Unblocked player " + toUnblockUUID + " for player UUID: " + blockerUUID, 0);
        }
    }

    // Registering the checkCachedData Command
    private void registerCheckCachedDataCommand() {
        new CommandAPICommand("checkCachedData")
                .withAliases("checkCache")
                .withPermission("mysticrpg.checkCachedData")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    PlayerData playerData = getCachedPlayerData(playerUUID);

                    if (playerData != null) {
                        player.sendMessage("Your cached data:");
                        player.sendMessage("Balance: " + playerData.getBalance());
                        player.sendMessage("XP: " + playerData.getXp());
                        player.sendMessage("Level: " + playerData.getLevel());
                        player.sendMessage("Next Level XP: " + playerData.getNextLevelXP());
                        player.sendMessage("Current HP: " + playerData.getCurrentHp());
                        player.sendMessage("Attributes: " + playerData.getAttributes().toString());
                        player.sendMessage("Unlocked Recipes: " + playerData.getUnlockedRecipes().toString());
                        player.sendMessage("Friend Requests: " + playerData.getFriendRequests().toString());
                        player.sendMessage("Friends: " + playerData.getFriends().toString());
                        player.sendMessage("Blocked Players: " + playerData.getBlockedPlayers().toString());
                        player.sendMessage("Blocking Requests: " + playerData.isBlockingRequests());
                        logger.log(Level.INFO, "Displayed cached data for player UUID: " + playerUUID, 0);
                    } else {
                        player.sendMessage("No cached data found for you.");
                        logger.error("No cached data found for player: " + playerUUID);
                    }
                })
                .register();
    }
}
