package eu.xaru.mysticrpg.storage.database;

import java.util.List;
import java.util.UUID;

import eu.xaru.mysticrpg.storage.Callback;

/**
 * Generic repository interface for CRUD operations.
 *
 * @param <T> The type of the data model.
 */
public interface IRepository<T> {
    void save(T entity, Callback<Void> callback);
    void load(UUID uuid, Callback<T> callback);
    void delete(UUID uuid, Callback<Void> callback);
    void loadAll(Callback<List<T>> callback);

    // Added method to load by Discord ID
    void loadByDiscordId(long discordId, Callback<T> callback);
}
