package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class SQLiteRepository implements eu.xaru.mysticrpg.storage.database.IDatabaseRepository {

    private final Connection connection;
    private final DebugLoggerModule logger;

    public SQLiteRepository(String databasePath, DebugLoggerModule logger) {
        this.logger = logger;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            initializeTables();
            logger.log(Level.INFO, "SQLiteRepository connected to SQLite database", 0);
        } catch (SQLException e) {
            logger.error("SQLiteRepository failed to connect to SQLite: " + e.getMessage());
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
        logger.log(Level.INFO, "Saving player data for UUID: " + playerData.getUuid(), 0);
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
            logger.log(Level.INFO, "Successfully saved player data for UUID: " + playerData.getUuid(), 0);
        } catch (SQLException e) {
            logger.error("Error saving player data for UUID: " + playerData.getUuid() + ". " + e.getMessage());
            callback.onFailure(e);
        }
    }

    @Override
    public void loadPlayerData(UUID uuid, Callback<PlayerData> callback) {
        logger.log(Level.INFO, "Loading player data for UUID: " + uuid, 0);
        String sql = "SELECT * FROM playerData WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                PlayerData playerData = new PlayerData();
                playerData.setUuid(rs.getString("uuid"));
                playerData.setBalance(rs.getDouble("balance"));
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
                playerData.setPendingBalance(rs.getDouble("pendingBalance"));
                playerData.setPendingItems(deserializeList(rs.getString("pendingItems")));
                playerData.setRemindersEnabled(rs.getInt("remindersEnabled") == 1);

                callback.onSuccess(playerData);
                logger.log(Level.INFO, "Successfully loaded player data for UUID: " + uuid, 0);
            } else {
                logger.log(Level.INFO, "No data found for UUID: " + uuid + ". Creating default data.", 0);
                PlayerData newPlayerData = PlayerData.defaultData(uuid.toString());
                savePlayerData(newPlayerData, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        logger.log(Level.INFO, "Default player data created for UUID: " + uuid, 0);
                        callback.onSuccess(newPlayerData);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("Failed to save default player data for UUID: " + uuid + ". " + throwable.getMessage());
                        callback.onFailure(throwable);
                    }
                });
            }
        } catch (SQLException e) {
            logger.error("Error loading player data for UUID: " + uuid + ". " + e.getMessage());
            callback.onFailure(e);
        }
    }

    @Override
    public void deletePlayerData(UUID uuid, Callback<Void> callback) {
        logger.log(Level.INFO, "Deleting player data for UUID: " + uuid, 0);
        String sql = "DELETE FROM playerData WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
            callback.onSuccess(null);
            logger.log(Level.INFO, "Successfully deleted player data for UUID: " + uuid, 0);
        } catch (SQLException e) {
            logger.error("Error deleting player data for UUID: " + uuid + ". " + e.getMessage());
            callback.onFailure(e);
        }
    }

    // Auction Operations

    @Override
    public void saveAuction(Auction auction, Callback<Void> callback) {
        logger.log(Level.INFO, "Saving auction with ID: " + auction.getAuctionId(), 0);
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
            logger.log(Level.INFO, "Successfully saved auction with ID: " + auction.getAuctionId(), 0);
        } catch (SQLException e) {
            logger.error("Error saving auction with ID: " + auction.getAuctionId() + ". " + e.getMessage());
            callback.onFailure(e);
        }
    }

    @Override
    public void loadAuctions(Callback<List<Auction>> callback) {
        logger.log(Level.INFO, "Loading auctions from SQLite", 0);
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Auction auction = new Auction();
                auction.setAuctionId(UUID.fromString(rs.getString("auctionId")));
                auction.setSeller(UUID.fromString(rs.getString("sellerUUID")));
                auction.setItemData(rs.getString("itemData")); // Deserialize if necessary
                auction.setStartingPrice(rs.getDouble("price"));
                auction.setEndTime(rs.getLong("expirationTime"));
                auctions.add(auction);
            }
            callback.onSuccess(auctions);
            logger.log(Level.INFO, "Loaded " + auctions.size() + " auctions from SQLite", 0);
        } catch (SQLException e) {
            logger.error("Error loading auctions from SQLite: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    @Override
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        logger.log(Level.INFO, "Deleting auction with ID: " + auctionId, 0);
        String sql = "DELETE FROM auctions WHERE auctionId = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId.toString());
            pstmt.executeUpdate();
            callback.onSuccess(null);
            logger.log(Level.INFO, "Successfully deleted auction with ID: " + auctionId, 0);
        } catch (SQLException e) {
            logger.error("Error deleting auction with ID: " + auctionId + ". " + e.getMessage());
            callback.onFailure(e);
        }
    }

    // Utility serialization/deserialization methods
    // Implement these methods based on your serialization preference (e.g., JSON)

    private String serializeMap(Map<String, Integer> map) {
        // Implement JSON serialization or other
        return new org.json.JSONObject(map).toString();
    }

    private Map<String, Integer> deserializeMap(String data) {
        // Implement JSON deserialization or other
        Map<String, Integer> map = new HashMap<>();
        org.json.JSONObject json = new org.json.JSONObject(data);
        for (String key : json.keySet()) {
            map.put(key, json.getInt(key));
        }
        return map;
    }

    private String serializeMapBoolean(Map<String, Boolean> map) {
        return new org.json.JSONObject(map).toString();
    }

    private Map<String, Boolean> deserializeMapBoolean(String data) {
        Map<String, Boolean> map = new HashMap<>();
        org.json.JSONObject json = new org.json.JSONObject(data);
        for (String key : json.keySet()) {
            map.put(key, json.getBoolean(key));
        }
        return map;
    }

    private String serializeSet(Set<String> set) {
        return new org.json.JSONArray(set).toString();
    }

    private Set<String> deserializeSet(String data) {
        Set<String> set = new HashSet<>();
        org.json.JSONArray json = new org.json.JSONArray(data);
        for (int i = 0; i < json.length(); i++) {
            set.add(json.getString(i));
        }
        return set;
    }

    private String serializeList(List<String> list) {
        return new org.json.JSONArray(list).toString();
    }

    private List<String> deserializeList(String data) {
        List<String> list = new ArrayList<>();
        org.json.JSONArray json = new org.json.JSONArray(data);
        for (int i = 0; i < json.length(); i++) {
            list.add(json.getString(i));
        }
        return list;
    }

    private String serializeQuestProgress(Map<String, Map<String, Integer>> questProgress) {
        // Implement serialization
        return new org.json.JSONObject(questProgress).toString();
    }

    private Map<String, Map<String, Integer>> deserializeQuestProgress(String data) {
        Map<String, Map<String, Integer>> questProgress = new HashMap<>();
        org.json.JSONObject json = new org.json.JSONObject(data);
        for (String key : json.keySet()) {
            org.json.JSONObject inner = json.getJSONObject(key);
            Map<String, Integer> innerMap = new HashMap<>();
            for (String innerKey : inner.keySet()) {
                innerMap.put(innerKey, inner.getInt(innerKey));
            }
            questProgress.put(key, innerMap);
        }
        return questProgress;
    }
}
