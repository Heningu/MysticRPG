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
 * MongoDB implementation of the IRepository interface.
 *
 * @param <T> The type of the data model.
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
            DebugLogger.getInstance().error("Entity does not contain the ID field: " + idField);
            callback.onFailure(new IllegalArgumentException("Missing ID field"));
            return;
        }
        Object idValue = data.get(idField);
        Document document = new Document(data);
        collection.replaceOne(Filters.eq(idField, idValue), document, new ReplaceOptions().upsert(true))
                .subscribe(new Subscriber<UpdateResult>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(UpdateResult updateResult) {
                        // Can log or handle update result if needed
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Error saving entity: " + t.getMessage(), t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        DebugLogger.getInstance().log(Level.INFO, "Entity saved successfully.", 0);
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void load(UUID uuid, Callback<T> callback) {
        collection.find(Filters.eq(idField, uuid.toString()))
                .first()
                .subscribe(new Subscriber<Document>() {
                    private T entity;

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document document) {
                        if (document != null) {
                            entity = deserialize(document);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Error loading entity: " + t.getMessage(), t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        if (entity != null) {
                            DebugLogger.getInstance().log(Level.INFO, "Entity loaded successfully.", 0);
                            callback.onSuccess(entity);
                        } else {
                            DebugLogger.getInstance().log(Level.INFO, "No entity found with UUID: " + uuid, 0);
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
                        // Can handle delete result if needed
                    }

                    @Override
                    public void onError(Throwable t) {
                        DebugLogger.getInstance().error("Error deleting entity: " + t.getMessage(), t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onComplete() {
                        DebugLogger.getInstance().log(Level.INFO, "Entity deleted successfully.", 0);
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    public void loadAll(Callback<List<T>> callback) {
        List<T> entities = new ArrayList<>();
        collection.find().subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE); // Request all documents
            }

            @Override
            public void onNext(Document document) {
                T entity = deserialize(document);
                if (entity != null) {
                    entities.add(entity);
                }
            }

            @Override
            public void onError(Throwable t) {
                DebugLogger.getInstance().error("Error loading all entities: " + t.getMessage(), t);
                callback.onFailure(t);
            }

            @Override
            public void onComplete() {
                DebugLogger.getInstance().log(Level.INFO, "All entities loaded successfully.", 0);
                callback.onSuccess(entities);
            }
        });
    }

    @Override
    public void loadByDiscordId(long discordId, Callback<T> callback) {
        collection.find(Filters.eq("discordId", discordId))
                .first()
                .subscribe(new Subscriber<Document>() {
                    private T entity;

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
                            callback.onFailure(new NoSuchElementException("No entity found with discordId: " + discordId));
                        }
                    }
                });
    }

    /**
     * Deserializes a MongoDB Document to an entity.
     *
     * @param document The document to deserialize.
     * @return The entity instance.
     */
    private T deserialize(Document document) {
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
        return deserialize(data);
    }
}
