package eu.xaru.mysticrpg.storage.database;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.*;
import java.util.logging.Level;

/**
 * MongoDB implementation of the IRepository interface, using Reactive Streams.
 *
 * @param <T> The data model type
 */
public class MongoRepository<T> extends BaseRepository<T> {

    private final MongoCollection<Document> collection;
    private final String idField;

    public MongoRepository(Class<T> type, MongoCollection<Document> collection, String idField) {
        super(type);
        this.collection = collection;
        this.idField = idField;
    }

    @Override
    public void save(T entity, Callback<Void> callback) {
        Map<String, Object> data = serialize(entity);
        if (!data.containsKey(idField)) {
            DebugLogger.getInstance().error("Entity missing ID field: " + idField);
            callback.onFailure(new IllegalArgumentException("Missing ID field"));
            return;
        }
        Object idVal = data.get(idField);
        Document doc = new Document(data);

        collection.replaceOne(Filters.eq(idField, idVal), doc, new ReplaceOptions().upsert(true))
                .subscribe(new Subscriber<UpdateResult>() {
                    Subscription sub;

                    @Override
                    public void onSubscribe(Subscription s) {
                        sub = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(UpdateResult updateResult) {
                        // updated or inserted
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Mongo save error: " + t.getMessage(), t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void load(UUID uuid, Callback<T> callback) {
        collection.find(Filters.eq(idField, uuid.toString()))
                .first()
                .subscribe(new Subscriber<Document>() {
                    T entity;
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document doc) {
                        if (doc != null) {
                            entity = deserialize(doc);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        if (entity != null) {
                            callback.onSuccess(entity);
                        } else {
                            callback.onFailure(new NoSuchElementException("Entity not found"));
                        }
                    }
                });
    }

    @Override
    public void delete(UUID uuid, Callback<Void> callback) {
        collection.deleteOne(Filters.eq(idField, uuid.toString()))
                .subscribe(new Subscriber<DeleteResult>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(DeleteResult deleteResult) {
                        // optionally check deletedCount
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void loadAll(Callback<List<T>> callback) {
        List<T> results = new ArrayList<>();
        collection.find().subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Document doc) {
                T entity = deserialize(doc);
                if (entity != null) {
                    results.add(entity);
                }
            }

            @Override
            public void onError(Throwable t) {
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                callback.onSuccess(results);
            }
        });
    }

    @Override
    public void loadByDiscordId(long discordId, Callback<T> callback) {
        collection.find(Filters.eq("discordId", discordId))
                .first()
                .subscribe(new Subscriber<Document>() {
                    T entity;
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document doc) {
                        entity = deserialize(doc);
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        if (entity != null) {
                            callback.onSuccess(entity);
                        } else {
                            callback.onFailure(new NoSuchElementException("No entity found for discordId=" + discordId));
                        }
                    }
                });
    }
}
