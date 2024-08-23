package eu.xaru.mysticrpg.storage;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import static com.mongodb.client.model.Filters.eq;

public class SaveHelper {

    private final MongoCollection<PlayerData> playerCollection;
    private final DebugLoggerModule logger;

    public SaveHelper(String connectionString, String databaseName, String collectionName, DebugLoggerModule logger) {
        this.logger = logger;

        try {
            // Create a custom CodecRegistry to handle POJOs
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            // Configure MongoClientSettings with the CodecRegistry
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new com.mongodb.ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            // Create the MongoClient with the settings
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Check if the collection exists, if not, create it
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                database.createCollection(collectionName);
                logger.log(Level.INFO, "Created collection: " + collectionName, 0);
            }

            this.playerCollection = database.getCollection(collectionName, PlayerData.class);
            logger.log(Level.INFO, "Connected to MongoDB and initialized playerData collection", 0);

        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: " + e.getMessage());
            throw e;
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        try {
            PlayerData playerData = playerCollection.find(eq("uuid", uuid)).first();
            if (playerData == null) {
                logger.log(Level.INFO, "No existing data found for UUID: " + uuid + ". Creating default data.", 0);
                playerData = PlayerData.defaultData(uuid);
                savePlayer(playerData);
            } else {
                logger.log(Level.INFO, "Successfully loaded data for UUID: " + uuid, 0);
            }
            return playerData;
        } catch (Exception e) {
            logger.error("Error loading player data for UUID: " + uuid + ". " + e.getMessage());
            throw e;
        }
    }

    public void savePlayer(PlayerData playerData) {
        try {
            playerCollection.replaceOne(eq("uuid", playerData.uuid()), playerData, new ReplaceOptions().upsert(true));
            logger.log(Level.INFO, "Successfully saved data for UUID: " + playerData.uuid(), 0);
        } catch (Exception e) {
            logger.error("Error saving player data for UUID: " + playerData.uuid() + ". " + e.getMessage());
            throw e;
        }
    }
}
