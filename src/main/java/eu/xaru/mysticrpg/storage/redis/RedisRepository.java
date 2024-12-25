package eu.xaru.mysticrpg.storage.redis;

import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.annotations.Persist;
import eu.xaru.mysticrpg.storage.database.BaseRepository;
import eu.xaru.mysticrpg.storage.database.IRepository;
import eu.xaru.mysticrpg.utils.DebugLogger;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.List;
import java.util.UUID;

/**
 * Wraps a final repository in Redis caching. Reads/writes go to Redis first.
 * Scheduled flush (if you want it) can occur externally, or partial updates, etc.
 */
public class RedisRepository<T> extends BaseRepository<T> {

    private final IRepository<T> finalRepo;
    private final String idField;

    public RedisRepository(Class<T> type, IRepository<T> finalRepo, String idField) {
        super(type);
        this.finalRepo = finalRepo;
        this.idField = idField;
    }

    @Override
    public void save(T entity, Callback<Void> callback) {
        Map<String, Object> data = serialize(entity);
        if (!data.containsKey(idField)) {
            callback.onFailure(new IllegalArgumentException("Missing ID field: " + idField));
            return;
        }
        String idVal = data.get(idField).toString();
        // Write to Redis
        String json = gson.toJson(data);
        RedisManager.getInstance().set(buildKey(idVal), json);

        // We do not forcibly flush to final repository right now,
        // but you could call finalRepo.save(...) if you want immediate sync
        callback.onSuccess(null);
    }

    @Override
    public void load(UUID uuid, Callback<T> callback) {
        String key = buildKey(uuid.toString());
        String json = RedisManager.getInstance().get(key);
        if (json != null) {
            Map<String, Object> map = gson.fromJson(json, Map.class);
            T entity = deserialize(map);
            if (entity != null) {
                callback.onSuccess(entity);
                return;
            }
        }
        // fallback to final repo
        finalRepo.load(uuid, new Callback<>() {
            @Override
            public void onSuccess(T loaded) {
                // optionally cache in Redis
                Map<String, Object> data = serialize(loaded);
                RedisManager.getInstance().set(key, gson.toJson(data));
                callback.onSuccess(loaded);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    @Override
    public void delete(UUID uuid, Callback<Void> callback) {
        RedisManager.getInstance().delete(buildKey(uuid.toString()));
        // also remove from final
        finalRepo.delete(uuid, callback);
    }

    @Override
    public void loadAll(Callback<List<T>> callback) {
        // For large sets, reading from Redis alone is not trivial if you haven't stored a keyset.
        // We'll just do finalRepo.loadAll for now.
        finalRepo.loadAll(callback);
    }

    @Override
    public void loadByDiscordId(long discordId, Callback<T> callback) {
        // We don't have a direct "discordId->key" in Redis, fallback:
        finalRepo.loadByDiscordId(discordId, callback);
    }

    private String buildKey(String idVal) {
        // e.g. "playerData:UUID"
        return type.getSimpleName().toLowerCase() + ":" + idVal;
    }
}
