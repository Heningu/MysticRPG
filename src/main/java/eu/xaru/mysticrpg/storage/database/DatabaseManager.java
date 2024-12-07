package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private static DatabaseManager instance;
    private eu.xaru.mysticrpg.storage.database.IDatabaseRepository repository;

    private DatabaseManager() {
        initializeRepository();
    }

    /**
     * Initializes the singleton instance. Should be called once during plugin initialization.
     *
     */
    public static synchronized void initialize() {
        if (instance == null) {
            instance = new DatabaseManager();
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "DatabaseManager is already initialized.", 0);
        }
    }

    /**
     * Retrieves the singleton instance.
     *
     * @return The DatabaseManager instance.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager is not initialized. Call initialize() first.");
        }
        return instance;
    }

    private void initializeRepository() {
        FileConfiguration config = getConfig(); // Implement this method to fetch your config
        String dbType = config.getString("database.type", "sqlite").toLowerCase();

        switch (dbType) {
            case "mongo":
                String connectionString = config.getString("database.mongo.connectionString", "mongodb://localhost:27017");
                String databaseName = config.getString("database.mongo.databaseName", "xarumystic");
                repository = new MongoRepository(connectionString, databaseName);
                DebugLogger.getInstance().log(Level.INFO, "DatabaseManager initialized with MongoRepository", 0);
                break;
            case "sqlite":
            default:
                String dbPath = config.getString("database.sqlite.path", "plugins/MysticRPG/database.db");
                repository = new SQLiteRepository(dbPath);
                DebugLogger.getInstance().log(Level.INFO, "DatabaseManager initialized with SQLiteRepository", 0);
                break;
        }
    }

    // Delegated Player Data Methods

    public void savePlayerData(PlayerData playerData, Callback<Void> callback) {
        repository.savePlayerData(playerData, callback);
    }

    public void loadPlayerData(UUID uuid, Callback<PlayerData> callback) {
        repository.loadPlayerData(uuid, callback);
    }

    public void deletePlayerData(UUID uuid, Callback<Void> callback) {
        repository.deletePlayerData(uuid, callback);
    }

    // Delegated Auction Methods

    public void saveAuction(Auction auction, Callback<Void> callback) {
        repository.saveAuction(auction, callback);
    }

    public void loadAuctions(Callback<List<Auction>> callback) {
        repository.loadAuctions(callback);
    }

    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        repository.deleteAuction(auctionId, callback);
    }

    public void loadAllPlayers(Callback<List<PlayerData>> callback) {
        repository.loadAllPlayers(callback);
    }

    // Implement this method to fetch your plugin's config
    private FileConfiguration getConfig() {
        // Assuming you have access to your main plugin instance
        return eu.xaru.mysticrpg.cores.MysticCore.getInstance().getConfig();
    }
}