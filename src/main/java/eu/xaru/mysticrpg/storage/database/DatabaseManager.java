package eu.xaru.mysticrpg.storage.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.*;

/**
 * DatabaseManager manages the database repositories (MongoDB or SQLite) based on configuration.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private IRepository<PlayerData> playerRepository;
    private IRepository<Auction> auctionRepository;

    private DatabaseManager() {
        initializeRepositories();
    }

    /**
     * Initializes the singleton instance. Should be called once during plugin initialization.
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

    /**
     * Initializes the appropriate repositories based on configuration.
     */
    private void initializeRepositories() {
        FileConfiguration config = getConfig(); // Implement this method to fetch your config
        String dbType = config.getString("database.type", "sqlite").toLowerCase();

        switch (dbType) {
            case "mongo":
                String connectionString = config.getString("database.mongo.connectionString", "mongodb://localhost:27017");
                String databaseName = config.getString("database.mongo.databaseName", "xarumystic");
                initializeMongoDB(connectionString, databaseName);
                break;
            case "sqlite":
            default:
                String dbPath = config.getString("database.sqlite.path", "plugins/MysticRPG/database.db");
                initializeSQLite(dbPath);
                break;
        }
    }

    /**
     * Initializes MongoDB repositories.
     *
     * @param connectionString The MongoDB connection string.
     * @param databaseName     The name of the MongoDB database.
     */
    private void initializeMongoDB(String connectionString, String databaseName) {
        try {
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();

            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            this.playerRepository = new MongoRepository<>(
                    PlayerData.class,
                    database.getCollection("playerData"),
                    "uuid"
            );

            this.auctionRepository = new MongoRepository<>(
                    Auction.class,
                    database.getCollection("auctions"),
                    "auctionId"
            );

            DebugLogger.getInstance().log(Level.INFO, "DatabaseManager initialized with MongoRepository", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("DatabaseManager failed to initialize MongoDB repositories:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes SQLite repositories.
     *
     * @param databasePath The path to the SQLite database file.
     */
    private void initializeSQLite(String databasePath) {
        try {
            this.playerRepository = new SQLiteRepository<>(
                    PlayerData.class,
                    databasePath,
                    "playerData",
                    "uuid"
            );

            this.auctionRepository = new SQLiteRepository<>(
                    Auction.class,
                    databasePath,
                    "auctions",
                    "auctionId"
            );

            DebugLogger.getInstance().log(Level.INFO, "DatabaseManager initialized with SQLiteRepository", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("DatabaseManager failed to initialize SQLite repositories:", e);
            throw new RuntimeException(e);
        }
    }

    // Getters for repositories

    public IRepository<PlayerData> getPlayerRepository() {
        return playerRepository;
    }

    public IRepository<Auction> getAuctionRepository() {
        return auctionRepository;
    }

    /**
     * Retrieves the plugin's configuration.
     * Implement this method to fetch your plugin's config.
     *
     * @return The FileConfiguration instance.
     */
    private FileConfiguration getConfig() {
        // Assuming you have access to your main plugin instance
        return eu.xaru.mysticrpg.cores.MysticCore.getInstance().getConfig();
    }
}
