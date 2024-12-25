package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.storage.Callback;

import java.util.List;
import java.util.UUID;

/**
 * Generic repository interface for CRUD operations.
 *
 * @param <T> The data model type
 */
public interface IRepository<T> {
    void save(T entity, Callback<Void> callback);
    void load(UUID uuid, Callback<T> callback);
    void delete(UUID uuid, Callback<Void> callback);
    void loadAll(Callback<List<T>> callback);

    void loadByDiscordId(long discordId, Callback<T> callback);
}
