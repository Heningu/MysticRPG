package eu.xaru.mysticrpg.storage.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.*;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.*;

public class MongoRepository implements eu.xaru.mysticrpg.storage.database.IDatabaseRepository {

    private final MongoCollection<PlayerData> playerCollection;
    private final MongoCollection<Auction> auctionCollection;
    

    public MongoRepository(String connectionString, String databaseName) {
 
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

            this.playerCollection = database.getCollection("playerData", PlayerData.class).withCodecRegistry(pojoCodecRegistry);
            this.auctionCollection = database.getCollection("auctions", Auction.class).withCodecRegistry(pojoCodecRegistry);

            DebugLogger.getInstance().log(Level.INFO, "MongoRepository connected to MongoDB and initialized collections", 0);
        } catch (Exception e) {
            DebugLogger.getInstance().error("MongoRepository failed to connect to MongoDB:", e);
            throw new RuntimeException(e);
        }
    }

    // Player Data Operations

    @Override
    public void savePlayerData(PlayerData playerData, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Saving player data for UUID: " + playerData.getUuid(), 0);
        playerCollection.replaceOne(Filters.eq("_id", playerData.getUuid()), playerData, new ReplaceOptions().upsert(true))
                .subscribe(new Subscriber<UpdateResult>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(UpdateResult updateResult) {
                        // Optionally handle update result
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Error saving player data for UUID: " + playerData.getUuid() + ". ", t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        DebugLogger.getInstance().log(Level.INFO, "Successfully saved player data for UUID: " + playerData.getUuid(), 0);
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void loadPlayerData(UUID uuid, Callback<PlayerData> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading player data for UUID: " + uuid, 0);
        playerCollection.find(Filters.eq("_id", uuid.toString())).first().subscribe(new Subscriber<PlayerData>() {
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
                DebugLogger.getInstance().error("Error loading player data for UUID: " + uuid + ". ", t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                if (playerData == null) {
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
                } else {
                    DebugLogger.getInstance().log(Level.INFO, "Player data loaded for UUID: " + uuid, 0);
                    callback.onSuccess(playerData);
                }
            }
        });
    }

    @Override
    public void deletePlayerData(UUID uuid, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Deleting player data for UUID: " + uuid, 0);
        playerCollection.deleteOne(Filters.eq("_id", uuid.toString())).subscribe(new Subscriber<DeleteResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(DeleteResult deleteResult) {
                // Optionally handle delete result
            }

            @Override
            public void onError(Throwable t) {
                DebugLogger.getInstance().error("Error deleting player data for UUID: " + uuid + ". ", t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                DebugLogger.getInstance().log(Level.INFO, "Successfully deleted player data for UUID: " + uuid, 0);
                callback.onSuccess(null);
            }
        });
    }

    // Auction Operations

    @Override
    public void saveAuction(Auction auction, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Saving auction with ID: " + auction.getAuctionId(), 0);
        auctionCollection.replaceOne(Filters.eq("_id", auction.getAuctionId()), auction, new ReplaceOptions().upsert(true))
                .subscribe(new Subscriber<UpdateResult>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(UpdateResult updateResult) {
                        // Optionally handle update result
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Error saving auction: ", t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        DebugLogger.getInstance().log(Level.INFO, "Successfully saved auction with ID: " + auction.getAuctionId(), 0);
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void loadAuctions(Callback<List<Auction>> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Loading auctions from MongoDB", 0);
        List<Auction> auctions = new ArrayList<>();
        auctionCollection.find().subscribe(new Subscriber<Auction>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Auction auction) {
                auctions.add(auction);
            }

            @Override
            public void onError(Throwable t) {
                DebugLogger.getInstance().error("Error loading auctions: ", t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                DebugLogger.getInstance().log(Level.INFO, "Loaded " + auctions.size() + " auctions from MongoDB", 0);
                callback.onSuccess(auctions);
            }
        });
    }

    @Override
    public void deleteAuction(UUID auctionId, Callback<Void> callback) {
        DebugLogger.getInstance().log(Level.INFO, "Deleting auction with ID: " + auctionId, 0);
        auctionCollection.deleteOne(Filters.eq("_id", auctionId.toString())).subscribe(new Subscriber<DeleteResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(DeleteResult deleteResult) {
                // Optionally handle delete result
            }

            @Override
            public void onError(Throwable t) {
                DebugLogger.getInstance().error("Error deleting auction: ", t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                DebugLogger.getInstance().log(Level.INFO, "Successfully deleted auction with ID: " + auctionId, 0);
                callback.onSuccess(null);
            }
        });
    }
    public void loadAllPlayers(Callback<List<PlayerData>> callback) {
        List<PlayerData> allPlayers = new ArrayList<>();
        playerCollection.find().subscribe(new Subscriber<PlayerData>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(PlayerData playerData) {
                allPlayers.add(playerData);
            }

            @Override
            public void onError(Throwable t) {
                DebugLogger.getInstance().error("Error loading all player data: ", t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                callback.onSuccess(allPlayers);
            }
        });
    }
}