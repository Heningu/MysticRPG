package eu.xaru.mysticrpg.storage.database;

import com.google.gson.Gson;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.annotations.Persist;
import eu.xaru.mysticrpg.utils.DebugLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base repository that handles serialization/deserialization
 * of fields annotated with @Persist.
 *
 * @param <T> The type of the data model.
 */
public abstract class BaseRepository<T> implements IRepository<T> {

    protected final Gson gson = new Gson();
    protected final Class<T> type;
    protected final Map<String, Field> persistFields;

    public BaseRepository(Class<T> type) {
        this.type = type;
        this.persistFields = new ConcurrentHashMap<>();
        initializePersistFields();
    }

    private void initializePersistFields() {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Persist.class)) {
                field.setAccessible(true);
                Persist persist = field.getAnnotation(Persist.class);
                String key = persist.key().isEmpty() ? field.getName() : persist.key();
                persistFields.put(key, field);
            }
        }
    }

    protected Map<String, Object> serialize(T entity) {
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Field> entry : persistFields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();
            try {
                Object value = field.get(entity);
                data.put(key, value);
            } catch (IllegalAccessException e) {
                DebugLogger.getInstance().error("Failed to serialize field: " + field.getName(), e);
            }
        }
        return data;
    }

    protected T deserialize(Map<String, Object> data) {
        try {
            T entity = type.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Field> entry : persistFields.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();
                if (data.containsKey(key)) {
                    Object value = data.get(key);
                    if (value != null) {
                        Class<?> fieldType = field.getType();
                        if (fieldType.isAssignableFrom(value.getClass())) {
                            field.set(entity, value);
                        } else {
                            // basic conversions
                            if (fieldType == int.class || fieldType == Integer.class) {
                                field.set(entity, ((Number) value).intValue());
                            } else if (fieldType == long.class || fieldType == Long.class) {
                                field.set(entity, ((Number) value).longValue());
                            } else if (fieldType == double.class || fieldType == Double.class) {
                                field.set(entity, ((Number) value).doubleValue());
                            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                                field.set(entity, value);
                            } else if (fieldType == String.class) {
                                field.set(entity, value.toString());
                            } else {
                                // attempt complex type with gson
                                String json = gson.toJson(value);
                                Object deserialized = gson.fromJson(json, fieldType);
                                field.set(entity, deserialized);
                            }
                        }
                    }
                }
            }
            return entity;
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to deserialize entity: " + type.getName(), e);
            return null;
        }
    }

    @Override
    public abstract void save(T entity, Callback<Void> callback);

    @Override
    public abstract void load(UUID uuid, Callback<T> callback);

    @Override
    public abstract void delete(UUID uuid, Callback<Void> callback);

    @Override
    public abstract void loadAll(Callback<List<T>> callback);

    @Override
    public abstract void loadByDiscordId(long discordId, Callback<T> callback);
}
