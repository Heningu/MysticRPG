package eu.xaru.mysticrpg.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static com.mongodb.client.model.Filters.eq;

public class SaveHelper {

    private final MongoCollection<PlayerData> playerCollection;
    private final DebugLoggerModule logger;

    public SaveHelper(String connectionString, String databaseName, String collectionName, DebugLoggerModule logger) {
        this.logger = logger;

        try {
            // Create a custom CodecRegistry to handle POJOs
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            // Configure MongoClientSettings with the CodecRegistry
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)  // Use the POJO CodecRegistry
                    .build();

            // Create the MongoClient with the settings
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Check if the collection exists, if not, create it
            List<String> collectionNames = new ArrayList<>();
            database.listCollectionNames().subscribe(new Subscriber<String>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(String collectionName) {
                    collectionNames.add(collectionName);
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Failed to list collections: " + t.getMessage());
                }

                @Override
                public void onComplete() {
                    if (!collectionNames.contains(collectionName)) {
                        database.createCollection(collectionName).subscribe(new Subscriber<Void>() {
                            @Override
                            public void onSubscribe(Subscription s) {
                                s.request(1);
                            }

                            @Override
                            public void onNext(Void aVoid) {
                                // Not used
                            }

                            @Override
                            public void onError(Throwable t) {
                                logger.error("Failed to create collection: " + t.getMessage());
                            }

                            @Override
                            public void onComplete() {
                                logger.log(Level.INFO, "Created collection: " + collectionName, 0);
                            }
                        });
                    } else {
                        logger.log(Level.INFO, "Collection already exists: " + collectionName, 0);
                    }
                }
            });

            this.playerCollection = database.getCollection(collectionName, PlayerData.class).withCodecRegistry(pojoCodecRegistry);
            logger.log(Level.INFO, "Connected to MongoDB and initialized playerData collection", 0);

            // Register the saveData command
            registerSaveDataCommand();

        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void loadPlayer(UUID uuid, Callback<PlayerData> callback) {
        String uuidString = uuid.toString();
        logger.log(Level.INFO, "Attempting to load player data for UUID: " + uuidString, 0);
        playerCollection.find(eq("_id", uuidString)).first().subscribe(new Subscriber<PlayerData>() {
            private PlayerData playerData;

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(PlayerData data) {
                this.playerData = data;
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error loading player data for UUID: " + uuidString + ". " + t.getMessage());
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                if (playerData == null) {
                    logger.log(Level.INFO, "No existing data found for UUID: " + uuidString + ". Creating default data.", 0);
                    PlayerData newPlayerData = PlayerData.defaultData(uuidString);
                    logger.logObject(newPlayerData); // Log the default data being created
                    savePlayer(newPlayerData, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            logger.log(Level.INFO, "Successfully saved data for new player UUID: " + newPlayerData.getUuid(), 0);
                            callback.onSuccess(newPlayerData);
                        }

                        @Override
                        public void onFailure(Throwable saveError) {
                            logger.error("Error saving player data for UUID: " + newPlayerData.getUuid() + ". " + saveError.getMessage());
                            callback.onFailure(saveError);
                        }
                    });
                } else {
                    logger.log(Level.INFO, "Successfully loaded data for UUID: " + uuidString, 0);
                    callback.onSuccess(playerData);
                }
            }
        });
    }

    public void savePlayer(PlayerData playerData, Callback<Void> callback) {
        logger.log(Level.INFO, "Attempting to save player data for UUID: " + playerData.getUuid(), 0);
        playerCollection.replaceOne(eq("_id", playerData.getUuid()), playerData, new ReplaceOptions().upsert(true)).subscribe(new Subscriber<UpdateResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(UpdateResult updateResult) {
                // You can handle updateResult here if needed
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error saving player data for UUID: " + playerData.getUuid() + ". " + t.getMessage());
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                logger.log(Level.INFO, "Successfully saved data for UUID: " + playerData.getUuid(), 0);
                callback.onSuccess(null);
            }
        });
    }

    // Command Logic for /saveData
    private void registerSaveDataCommand() {
        new CommandAPICommand("saveData")
                .withAliases("saveDB")
                .withPermission("mysticrpg.saveData")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    logger.log(Level.INFO, "Player " + player.getName() + " executed /saveData command.", 0);

                    loadPlayer(playerUUID, new Callback<PlayerData>() {
                        @Override
                        public void onSuccess(PlayerData playerData) {
                            savePlayer(playerData, new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    player.sendMessage("Your data has been saved to the database.");
                                    logger.log(Level.INFO, "Data saved successfully for player: " + player.getName(), 0);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    player.sendMessage("Failed to save your data. Please try again later.");
                                    logger.error("Failed to save data for player: " + player.getName() + ". " + throwable.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            player.sendMessage("Failed to load your data. Please try again later.");
                            logger.error("Failed to load data for player: " + player.getName() + ". " + throwable.getMessage());
                        }
                    });
                })
                .register();
    }
}
