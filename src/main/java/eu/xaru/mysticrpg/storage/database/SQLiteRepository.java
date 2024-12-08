package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class SQLiteRepository implements eu.xaru.mysticrpg.storage.database.IDatabaseRepository {

    private final Connection connection;
    

    public SQLiteRepository(String databasePath) {
 
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            initializeTables();
            DebugLogger.getInstance().log(Level.INFO, "SQLiteRepository connected to SQLite database", 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("SQLiteRepository failed to connect to SQLite:", e);
            throw new RuntimeException(e);
        }
    }

    private void initializeTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Player Data Table
        stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS playerData (" +
                        "uuid TEXT PRIMARY KEY," +
                        "balance REAL," +
                        "xp INTEGER," +
                        "level INTEGER," +
                        "nextLevelXP INTEGER," +
                        "currentHp INTEGER," +
                        "attributes TEXT," + // JSON or serialized string
                        "unlockedRecipes TEXT," + // JSON or serialized string
                        "friendRequests TEXT," + // JSON or serialized string
                        "friends TEXT," + // JSON or serialized string
                        "blockedPlayers TEXT," + // JSON or serialized string
                        "blockingRequests INTEGER," +
                        "attributePoints INTEGER," +
                        "activeQuests TEXT," + // JSON or serialized string
                        "questProgress TEXT," + // JSON or serialized string
                        "completedQuests TEXT," + // JSON or serialized string
                        "pinnedQuest TEXT," +
                        "pendingBalance REAL," +
                        "pendingItems TEXT," + // JSON or serialized string
                        "remindersEnabled INTEGER" +
                        ");"
        );

        // Auctions Table
        stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS auctions (" +
                        "auctionId TEXT PRIMARY KEY," +
                        "sellerUUID TEXT," +
                        "itemData TEXT," + // Serialized ItemStack
                        "price REAL," +
                        "expirationTime INTEGER" +
                        ");"
        );

        stmt.close();
    }

    // Player Data Operations

    @Override
    public void savePlayerData(PlayerData playerData, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Saving player data for UUID: " + playerData.getUuid(), 0);
        String sql = "INSERT INTO playerData (uuid, balance, xp, level, nextLevelXP, currentHp, attributes, unlockedRecipes, " +
                "friendRequests, friends, blockedPlayers, blockingRequests, attributePoints, activeQuests, questProgress, " +
                "completedQuests, pinnedQuest, pendingBalance, pendingItems, remindersEnabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "balance=excluded.balance, xp=excluded.xp, level=excluded.level, nextLevelXP=excluded.nextLevelXP, " +
                "currentHp=excluded.currentHp, attributes=excluded.attributes, unlockedRecipes=excluded.unlockedRecipes, " +
                "friendRequests=excluded.friendRequests, friends=excluded.friends, blockedPlayers=excluded.blockedPlayers, " +
                "blockingRequests=excluded.blockingRequests, attributePoints=excluded.attributePoints, " +
                "activeQuests=excluded.activeQuests, questProgress=excluded.questProgress, completedQuests=excluded.completedQuests, " +
                "pinnedQuest=excluded.pinnedQuest, pendingBalance=excluded.pendingBalance, pendingItems=excluded.pendingItems, " +
                "remindersEnabled=excluded.remindersEnabled;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerData.getUuid());
            pstmt.setDouble(2, playerData.getBalance());
            pstmt.setInt(3, playerData.getXp());
            pstmt.setInt(4, playerData.getLevel());
            pstmt.setInt(5, playerData.getNextLevelXP());
            pstmt.setInt(6, playerData.getCurrentHp());
            pstmt.setString(7, serializeMap(playerData.getAttributes()));
            pstmt.setString(8, serializeMapBoolean(playerData.getUnlockedRecipes()));
            pstmt.setString(9, serializeSet(playerData.getFriendRequests()));
            pstmt.setString(10, serializeSet(playerData.getFriends()));
            pstmt.setString(11, serializeSet(playerData.getBlockedPlayers()));
            pstmt.setInt(12, playerData.isBlockingRequests() ? 1 : 0);
            pstmt.setInt(13, playerData.getAttributePoints());
            pstmt.setString(14, serializeList(playerData.getActiveQuests()));
            pstmt.setString(15, serializeQuestProgress(playerData.getQuestProgress()));
            pstmt.setString(16, serializeList(playerData.getCompletedQuests()));
            pstmt.setString(17, playerData.getPinnedQuest());
            pstmt.setDouble(18, playerData.getPendingBalance());
            pstmt.setString(19, serializeList(playerData.getPendingItems()));
            pstmt.setInt(20, playerData.isRemindersEnabled() ? 1 : 0);

            pstmt.executeUpdate();
            callback.onSuccess(null);
            DebugLogger.getInstance().log(Level.INFO, "Successfully saved player data for UUID: " + playerData.getUuid(), 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error saving player data for UUID: " + playerData.getUuid() + ".", e);
            callback.onFailure(e);
        }
    }

    @Override
    public void loadPlayerData(UUID uuid, Callback<PlayerData> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading player data for UUID: " + uuid, 0);
        String sql = "SELECT * FROM playerData WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                PlayerData playerData = new PlayerData();
                playerData.setUuid(rs.getString("uuid"));
                playerData.setBalance(rs.getInt("balance"));
                playerData.setXp(rs.getInt("xp"));
                playerData.setLevel(rs.getInt("level"));
                playerData.setNextLevelXP(rs.getInt("nextLevelXP"));
                playerData.setCurrentHp(rs.getInt("currentHp"));
                playerData.setAttributes(deserializeMap(rs.getString("attributes")));
                playerData.setUnlockedRecipes(deserializeMapBoolean(rs.getString("unlockedRecipes")));
                playerData.setFriendRequests(deserializeSet(rs.getString("friendRequests")));
                playerData.setFriends(deserializeSet(rs.getString("friends")));
                playerData.setBlockedPlayers(deserializeSet(rs.getString("blockedPlayers")));
                playerData.setBlockingRequests(rs.getInt("blockingRequests") == 1);
                playerData.setAttributePoints(rs.getInt("attributePoints"));
                playerData.setActiveQuests(deserializeList(rs.getString("activeQuests")));
                playerData.setQuestProgress(deserializeQuestProgress(rs.getString("questProgress")));
                playerData.setCompletedQuests(deserializeList(rs.getString("completedQuests")));
                playerData.setPinnedQuest(rs.getString("pinnedQuest"));
                playerData.setPendingBalance(rs.getInt("pendingBalance"));
                playerData.setPendingItems(deserializeList(rs.getString("pendingItems")));
                playerData.setRemindersEnabled(rs.getInt("remindersEnabled") == 1);

                callback.onSuccess(playerData);
                DebugLogger.getInstance().log(Level.INFO, "Successfully loaded player data for UUID: " + uuid, 0);
            } else {
                DebugLogger.getInstance().log(Level.INFO, "No data found for UUID: " + uuid + ". Creating default data.", 0);
                PlayerData newPlayerData = PlayerData.defaultData(uuid.toString());
                savePlayerData(newPlayerData, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        DebugLogger.getInstance().log(Level.INFO, "Default player data created for UUID: " + uuid, 0);
                        callback.onSuccess(newPlayerData);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        DebugLogger.getInstance().error("Failed to save default player data for UUID: " + uuid + ". ", throwable);
                        callback.onFailure(throwable);
                    }
                });
            }
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error loading player data for UUID: " + uuid + ".", e);
            callback.onFailure(e);
        }
    }

    @Override
    public void deletePlayerData(UUID uuid, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Deleting player data for UUID: " + uuid, 0);
        String sql = "DELETE FROM playerData WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
            callback.onSuccess(null);
            DebugLogger.getInstance().log(Level.INFO, "Successfully deleted player data for UUID: " + uuid, 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error deleting player data for UUID: " + uuid + ".", e);
            callback.onFailure(e);
        }
    }

    // Auction Operations

    @Override
    public void saveAuction(Auction auction, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Saving auction with ID: " + auction.getAuctionId(), 0);
        String sql = "INSERT INTO auctions (auctionId, sellerUUID, itemData, price, expirationTime) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(auctionId) DO UPDATE SET " +
                "sellerUUID=excluded.sellerUUID, itemData=excluded.itemData, price=excluded.price, expirationTime=excluded.expirationTime;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auction.getAuctionId().toString());
            pstmt.setString(2, auction.getSeller().toString());
            pstmt.setString(3, auction.getItemData()); // Ensure itemData is serialized
            pstmt.setDouble(4, auction.getStartingPrice());
            pstmt.setLong(5, auction.getEndTime());

            pstmt.executeUpdate();
            callback.onSuccess(null);
            DebugLogger.getInstance().log(Level.INFO, "Successfully saved auction with ID: " + auction.getAuctionId(), 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error saving auction with ID: " + auction.getAuctionId() + ".", e);
            callback.onFailure(e);
        }
    }

    @Override
    public void loadAuctions(Callback<List<Auction>> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading auctions from SQLite", 0);
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Auction auction = new Auction();
                auction.setAuctionId(UUID.fromString(rs.getString("auctionId")));
                auction.setSeller(UUID.fromString(rs.getString("sellerUUID")));
                auction.setItemData(rs.getString("itemData")); // Deserialize if necessary
                auction.setStartingPrice(rs.getInt("price"));
                auction.setEndTime(rs.getLong("expirationTime"));
                auctions.add(auction);
            }
            callback.onSuccess(auctions);
            DebugLogger.getInstance().log(Level.INFO, "Loaded " + auctions.size() + " auctions from SQLite", 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error loading auctions from SQLite:", e);
            callback.onFailure(e);
        }
    }

    @Override
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Deleting auction with ID: " + auctionId, 0);
        String sql = "DELETE FROM auctions WHERE auctionId = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId.toString());
            pstmt.executeUpdate();
            callback.onSuccess(null);
            DebugLogger.getInstance().log(Level.INFO, "Successfully deleted auction with ID: " + auctionId, 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error deleting auction with ID: " + auctionId + ".", e);
            callback.onFailure(e);
        }
    }

    // New Method: Load All Players

    @Override
    public void loadAllPlayers(Callback<List<PlayerData>> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading all player data from SQLite", 0);
        List<PlayerData> allPlayers = new ArrayList<>();
        String sql = "SELECT * FROM playerData;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                PlayerData playerData = new PlayerData();
                playerData.setUuid(rs.getString("uuid"));
                playerData.setBalance(rs.getInt("balance"));
                playerData.setXp(rs.getInt("xp"));
                playerData.setLevel(rs.getInt("level"));
                playerData.setNextLevelXP(rs.getInt("nextLevelXP"));
                playerData.setCurrentHp(rs.getInt("currentHp"));
                playerData.setAttributes(deserializeMap(rs.getString("attributes")));
                playerData.setUnlockedRecipes(deserializeMapBoolean(rs.getString("unlockedRecipes")));
                playerData.setFriendRequests(deserializeSet(rs.getString("friendRequests")));
                playerData.setFriends(deserializeSet(rs.getString("friends")));
                playerData.setBlockedPlayers(deserializeSet(rs.getString("blockedPlayers")));
                playerData.setBlockingRequests(rs.getInt("blockingRequests") == 1);
                playerData.setAttributePoints(rs.getInt("attributePoints"));
                playerData.setActiveQuests(deserializeList(rs.getString("activeQuests")));
                playerData.setQuestProgress(deserializeQuestProgress(rs.getString("questProgress")));
                playerData.setCompletedQuests(deserializeList(rs.getString("completedQuests")));
                playerData.setPinnedQuest(rs.getString("pinnedQuest"));
                playerData.setPendingBalance(rs.getInt("pendingBalance"));
                playerData.setPendingItems(deserializeList(rs.getString("pendingItems")));
                playerData.setRemindersEnabled(rs.getInt("remindersEnabled") == 1);

                allPlayers.add(playerData);
            }
            callback.onSuccess(allPlayers);
            DebugLogger.getInstance().log(Level.INFO, "Successfully loaded all player data. Total players: " + allPlayers.size(), 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error loading all player data from SQLite:", e);
            callback.onFailure(e);
        }
    }



    // Utility serialization/deserialization methods
    // Implement these methods based on your serialization preference (e.g., JSON)

    private String serializeMap(Map<String, Integer> map) {
        return new JSONObject(map).toString();
    }

    private Map<String, Integer> deserializeMap(String data) {
        Map<String, Integer> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;
        JSONObject json = new JSONObject(data);
        for (String key : json.keySet()) {
            map.put(key, json.getInt(key));
        }
        return map;
    }

    private String serializeMapBoolean(Map<String, Boolean> map) {
        return new JSONObject(map).toString();
    }

    private Map<String, Boolean> deserializeMapBoolean(String data) {
        Map<String, Boolean> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;
        JSONObject json = new JSONObject(data);
        for (String key : json.keySet()) {
            map.put(key, json.getBoolean(key));
        }
        return map;
    }

    private String serializeSet(Set<String> set) {
        return new JSONArray(set).toString();
    }

    private Set<String> deserializeSet(String data) {
        Set<String> set = new HashSet<>();
        if (data == null || data.isEmpty()) return set;
        JSONArray json = new JSONArray(data);
        for (int i = 0; i < json.length(); i++) {
            set.add(json.getString(i));
        }
        return set;
    }

    private String serializeList(List<String> list) {
        return new JSONArray(list).toString();
    }

    private List<String> deserializeList(String data) {
        List<String> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;
        JSONArray json = new JSONArray(data);
        for (int i = 0; i < json.length(); i++) {
            list.add(json.getString(i));
        }
        return list;
    }

    private String serializeQuestProgress(Map<String, Map<String, Integer>> questProgress) {
        return new JSONObject(questProgress).toString();
    }

    private Map<String, Map<String, Integer>> deserializeQuestProgress(String data) {
        Map<String, Map<String, Integer>> questProgress = new HashMap<>();
        if (data == null || data.isEmpty()) return questProgress;
        JSONObject json = new JSONObject(data);
        for (String key : json.keySet()) {
            JSONObject inner = json.getJSONObject(key);
            Map<String, Integer> innerMap = new HashMap<>();
            for (String innerKey : inner.keySet()) {
                innerMap.put(innerKey, inner.getInt(innerKey));
            }
            questProgress.put(key, innerMap);
        }
        return questProgress;
    }
}