package eu.xaru.mysticrpg.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.*;
import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.Base64;
import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.*;

/**
 * SaveHelper handles direct interactions with MongoDB for saving and loading player data and auctions.
 */
public class SaveHelper {

    private final MongoCollection<PlayerData> playerCollection;
    private final MongoCollection<Auction> auctionCollection; // Collection for auctions
    private final DebugLoggerModule logger;

    /**
     * Constructor for SaveHelper.
     *
     * @param connectionString     MongoDB connection string.
     * @param databaseName         Name of the database.
     * @param playerCollectionName Name of the player data collection.
     * @param logger               Logger instance for logging messages.
     */
    public SaveHelper(String connectionString, String databaseName, String playerCollectionName, DebugLoggerModule logger) {
        this.logger = logger;

        try {
            // Create a custom CodecRegistry to handle POJOs
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            // Configure MongoClientSettings with the CodecRegistry and UuidRepresentation
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)  // Use the POJO CodecRegistry
                    .uuidRepresentation(UuidRepresentation.STANDARD) // Specify UUID representation
                    .build();

            // Create the MongoClient with the settings
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Initialize collections
            this.playerCollection = database.getCollection(playerCollectionName, PlayerData.class).withCodecRegistry(pojoCodecRegistry);
            this.auctionCollection = database.getCollection("auctions", Auction.class).withCodecRegistry(pojoCodecRegistry); // Auction collection

            logger.log(Level.INFO, "Connected to MongoDB and initialized collections", 0);

            // Register the saveData command
            registerSaveDataCommand();

        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // -------------------------- Player Data Methods --------------------------

    /**
     * Loads player data from the database.
     *
     * @param uuid     UUID of the player.
     * @param callback Callback for success or failure.
     */
    public void loadPlayer(UUID uuid, Callback<PlayerData> callback) {
        String uuidString = uuid.toString();
        logger.log(Level.INFO, "Attempting to load player data for UUID: " + uuidString, 0);
        playerCollection.find(Filters.eq("_id", uuidString)).first().subscribe(new Subscriber<PlayerData>() {
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

    /**
     * Saves player data to the database.
     *
     * @param playerData PlayerData object to save.
     * @param callback   Callback for success or failure.
     */
    public void savePlayer(PlayerData playerData, Callback<Void> callback) {
        logger.log(Level.INFO, "Attempting to save player data for UUID: " + playerData.getUuid(), 0);
        playerCollection.replaceOne(Filters.eq("_id", playerData.getUuid()), playerData, new ReplaceOptions().upsert(true)).subscribe(new Subscriber<UpdateResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(UpdateResult updateResult) {
                // Handle updateResult if needed
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

    // -------------------------- Auction Methods --------------------------

    /**
     * Saves an auction to the database.
     *
     * @param auction  The auction to save.
     * @param callback Callback for success or failure.
     */
    public void saveAuction(Auction auction, Callback<Void> callback) {
        logger.log(Level.INFO, "Saving auction with ID: " + auction.getAuctionId(), 0);

        auctionCollection.replaceOne(Filters.eq("_id", auction.getAuctionId()), auction, new ReplaceOptions().upsert(true))
                .subscribe(new Subscriber<UpdateResult>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(UpdateResult updateResult) {
                        // Handle result if needed
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.error("Error saving auction: " + t.getMessage());
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        logger.log(Level.INFO, "Auction saved successfully: " + auction.getAuctionId(), 0);
                        callback.onSuccess(null);
                    }
                });
    }

    /**
     * Loads all auctions from the database.
     *
     * @param callback Callback with the list of auctions.
     */
    public void loadAuctions(Callback<List<Auction>> callback) {
        logger.log(Level.INFO, "Loading auctions from database", 0);
        List<Auction> auctions = new ArrayList<>();
        auctionCollection.find().subscribe(new Subscriber<Auction>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Auction auction) {
                // Deserialize the ItemStack
                ItemStack item = SaveHelper.itemStackFromBase64(auction.getItemData());
                auction.setItem(item);
                auctions.add(auction);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error loading auctions: " + t.getMessage());
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                logger.log(Level.INFO, "Auctions loaded: " + auctions.size(), 0);
                callback.onSuccess(auctions);
            }
        });
    }

    /**
     * Deletes an auction from the database.
     *
     * @param auctionId The UUID of the auction to delete.
     * @param callback  Callback for success or failure.
     */
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        logger.log(Level.INFO, "Deleting auction with ID: " + auctionId, 0);
        auctionCollection.deleteOne(Filters.eq("_id", auctionId)).subscribe(new Subscriber<DeleteResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(DeleteResult deleteResult) {
                // Handle result if needed
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error deleting auction: " + t.getMessage());
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                logger.log(Level.INFO, "Auction deleted successfully: " + auctionId, 0);
                callback.onSuccess(null);
            }
        });
    }

    // ---------------------- End of Auction Methods -----------------------

    /**
     * Registers the /saveData command for manual saving.
     */
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

    // Utility methods for serializing and deserializing ItemStacks

    /**
     * Serializes an ItemStack to a Base64 encoded string.
     *
     * @param item The ItemStack to serialize.
     * @return The Base64 encoded string.
     */
    public static String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializes an ItemStack from a Base64 encoded string.
     *
     * @param data The Base64 encoded string.
     * @return The deserialized ItemStack.
     */
    public static ItemStack itemStackFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
