package eu.xaru.mysticrpg.storage;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDataCache {

    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final SaveHelper saveHelper;
    private final DebugLoggerModule logger;

    public PlayerDataCache(SaveHelper saveHelper, DebugLoggerModule logger) {
        this.saveHelper = saveHelper;
        this.logger = logger;
        registerCheckCachedDataCommand(); // Register the command
    }

    // Load data into cache when player joins
    public void loadPlayerData(UUID playerUUID, Callback<PlayerData> callback) {
        saveHelper.loadPlayer(playerUUID, new Callback<PlayerData>() {
            @Override
            public void onSuccess(PlayerData playerData) {
                cache.put(playerUUID, playerData);
                callback.onSuccess(playerData);
            }

            @Override
            public void onFailure(Throwable throwable) {
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
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callback.onFailure(throwable);
                }
            });
        } else {
            callback.onFailure(new IllegalStateException("No cached data found for player: " + playerUUID));
        }
    }

    // Access cached data
    public PlayerData getCachedPlayerData(UUID playerUUID) {
        return cache.get(playerUUID);
    }

    // Remove player data from cache
    public void removePlayerData(UUID playerUUID) {
        cache.remove(playerUUID);
    }

    // -------------------------------------------------------
    // Methods to modify cached player data (friends-related)
    // -------------------------------------------------------

    public void addFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.friends.add(friendUUID.toString());
        }
    }

    public void removeFriend(UUID playerUUID, UUID friendUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.friends.remove(friendUUID.toString());
        }
    }

    public void addFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.friendRequests.add(requesterUUID.toString());
        }
    }

    public void removeFriendRequest(UUID playerUUID, UUID requesterUUID) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.friendRequests.remove(requesterUUID.toString());
        }
    }

    public void blockPlayer(UUID blockerUUID, UUID toBlockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.blockedPlayers.add(toBlockUUID.toString());
        }
    }

    public void unblockPlayer(UUID blockerUUID, UUID toUnblockUUID) {
        PlayerData playerData = cache.get(blockerUUID);
        if (playerData != null) {
            playerData.blockedPlayers.remove(toUnblockUUID.toString());
        }
    }

    // -------------------------------------------------------
    // Methods to modify general cached player data
    // -------------------------------------------------------

    public double getBalance(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.balance : 0.0;
    }

    public void setBalance(UUID playerUUID, double balance) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.balance = balance;
        }
    }

    public int getXp(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.xp : 0;
    }

    public void setXp(UUID playerUUID, int xp) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.xp = xp;
        }
    }

    public int getLevel(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.level : 0;
    }

    public void setLevel(UUID playerUUID, int level) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.level = level;
        }
    }

    public int getNextLevelXP(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.nextLevelXP : 0;
    }

    public void setNextLevelXP(UUID playerUUID, int nextLevelXP) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.nextLevelXP = nextLevelXP;
        }
    }

    public int getCurrentHp(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.currentHp : 0;
    }

    public void setCurrentHp(UUID playerUUID, int currentHp) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.currentHp = currentHp;
        }
    }

    public Map<String, Integer> getAttributes(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.attributes : null;
    }

    public void setAttributes(UUID playerUUID, Map<String, Integer> attributes) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.attributes = attributes;
        }
    }

    public Map<String, Boolean> getUnlockedRecipes(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.unlockedRecipes : null;
    }

    public void setUnlockedRecipes(UUID playerUUID, Map<String, Boolean> unlockedRecipes) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.unlockedRecipes = unlockedRecipes;
        }
    }

    public boolean isBlockingRequests(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null && playerData.blockingRequests;
    }

    public void setBlockingRequests(UUID playerUUID, boolean blockingRequests) {
        PlayerData playerData = cache.get(playerUUID);
        if (playerData != null) {
            playerData.blockingRequests = blockingRequests;
        }
    }

    // -------------------------------------------------------
    // New Methods for accessing friend requests and blocked players
    // -------------------------------------------------------

    public Set<String> getFriendRequests(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.friendRequests : Set.of();
    }

    public Set<String> getBlockedPlayers(UUID playerUUID) {
        PlayerData playerData = cache.get(playerUUID);
        return playerData != null ? playerData.blockedPlayers : Set.of();
    }

    // -------------------------------------------------------
    // Registering the checkCachedData Command
    // -------------------------------------------------------

    private void registerCheckCachedDataCommand() {
        new CommandAPICommand("checkCachedData")
                .withAliases("checkCache")
                .withPermission("mysticrpg.checkCachedData")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    PlayerData playerData = getCachedPlayerData(playerUUID);

                    if (playerData != null) {
                        player.sendMessage("Your cached data:");
                        player.sendMessage("Balance: " + playerData.balance);
                        player.sendMessage("XP: " + playerData.xp);
                        player.sendMessage("Level: " + playerData.level);
                        player.sendMessage("Next Level XP: " + playerData.nextLevelXP);
                        player.sendMessage("Current HP: " + playerData.currentHp);
                        player.sendMessage("Attributes: " + playerData.attributes.toString());
                        player.sendMessage("Unlocked Recipes: " + playerData.unlockedRecipes.toString());
                        player.sendMessage("Friend Requests: " + playerData.friendRequests.toString());
                        player.sendMessage("Friends: " + playerData.friends.toString());
                        player.sendMessage("Blocked Players: " + playerData.blockedPlayers.toString());
                        player.sendMessage("Blocking Requests: " + playerData.blockingRequests);
                    } else {
                        player.sendMessage("No cached data found for you.");
                    }
                })
                .register();
    }
}
