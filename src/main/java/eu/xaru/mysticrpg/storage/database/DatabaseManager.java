package eu.xaru.mysticrpg.storage.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.redis.RedisManager;
import eu.xaru.mysticrpg.storage.redis.RedisRepository;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.*;

/**
 * DatabaseManager manages the database repositories (MongoDB or SQLite),
 * optionally wrapping them in Redis if enabled.
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    private IRepository<PlayerData> playerRepository;
    private IRepository<Auction> auctionRepository;

    private DatabaseManager() {
        initializeRepositories();
    }

    /**
     * Initializes the singleton instance (if not already).
     */
    public static synchronized void initialize() {
        if (instance == null) {
            instance = new DatabaseManager();
        } else {
            DebugLogger.getInstance().log(Level.WARNING, "DatabaseManager already initialized.", 0);
        }
    }

    /**
     * @return The singleton instance, or throws if not yet initialized.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager not initialized.");
        }
        return instance;
    }

    /**
     * Reads from plugin config to set up DB type (mongo vs. sqlite) and Redis (optional).
     */
    private void initializeRepositories() {
        FileConfiguration config = MysticCore.getInstance().getConfig();

        // Initialize Redis first with values from config
        RedisManager.initialize(config);

        String dbType = config.getString("database.type", "sqlite").toLowerCase();
        DebugLogger.getInstance().log(Level.INFO, "Using DB type: " + dbType, 0);

        boolean redisEnabled = config.getBoolean("database.redis.enabled", false);

        if (dbType.equals("mongo")) {
            String connStr = config.getString("database.mongo.connectionString", "mongodb://localhost:27017");
            String dbName = config.getString("database.mongo.databaseName", "xarumystic");
            initializeMongoDB(connStr, dbName);
        } else {
            String dbPath = config.getString("database.sqlite.path", "plugins/MysticRPG/database.db");
            initializeSQLite(dbPath);
        }

        // If redis is enabled, wrap repositories in RedisRepository
        if (redisEnabled) {
            this.playerRepository = new RedisRepository<>(
                    PlayerData.class,
                    this.playerRepository,
                    "uuid"
            );
            this.auctionRepository = new RedisRepository<>(
                    Auction.class,
                    this.auctionRepository,
                    "auctionId"
            );
            DebugLogger.getInstance().log(Level.INFO, "DatabaseManager: Repositories wrapped in RedisRepository", 0);
        }
    }

    private void initializeMongoDB(String connectionString, String dbName) {
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

            MongoClient client = MongoClients.create(settings);
            MongoDatabase db = client.getDatabase(dbName);

            this.playerRepository = new MongoRepository<>(
                    PlayerData.class,
                    db.getCollection("playerData"),
                    "uuid"
            );
            this.auctionRepository = new MongoRepository<>(
                    Auction.class,
                    db.getCollection("auctions"),
                    "auctionId"
            );

            DebugLogger.getInstance().log(Level.INFO, "DatabaseManager: MongoDB repositories initialized", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to init MongoDB repos:", e);
            throw new RuntimeException(e);
        }
    }

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

            DebugLogger.getInstance().log(Level.INFO, "DatabaseManager: SQLite repositories initialized", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to init SQLite repos:", e);
            throw new RuntimeException(e);
        }
    }

    public IRepository<PlayerData> getPlayerRepository() {
        return playerRepository;
    }

    public IRepository<Auction> getAuctionRepository() {
        return auctionRepository;
    }
}
