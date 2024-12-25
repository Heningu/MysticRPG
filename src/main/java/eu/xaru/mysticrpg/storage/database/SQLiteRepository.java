package eu.xaru.mysticrpg.storage.database;

import com.google.gson.Gson;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite repository with table auto-creation and migration.
 */
public class SQLiteRepository<T> extends BaseRepository<T> {

    private final Connection connection;
    private final String tableName;
    private final String idField;
    private final Gson gson = new Gson();

    public SQLiteRepository(Class<T> type, String databasePath, String tableName, String idField) {
        super(type);
        this.tableName = tableName;
        this.idField = idField;
        this.connection = connect(databasePath);
        initializeTable();
    }

    private Connection connect(String path) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            DebugLogger.getInstance().log(Level.INFO, "Connected to SQLite at " + path, 0);
            return conn;
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Failed to connect SQLite at " + path, e);
            throw new RuntimeException(e);
        }
    }

    private void initializeTable() {
        try (Statement stmt = connection.createStatement()) {
            // Create table if not exists
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            sql.append(tableName).append(" (");
            sql.append(idField).append(" TEXT PRIMARY KEY, ");
            int count = 0;
            for (String key : persistFields.keySet()) {
                Field f = persistFields.get(key);
                String type = getSQLType(f.getType());
                sql.append(key).append(" ").append(type);
                if (++count < persistFields.size()) {
                    sql.append(", ");
                }
            }
            sql.append(");");
            stmt.execute(sql.toString());

            // Migrate: add missing columns
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ");");
            Set<String> existing = new HashSet<>();
            while (rs.next()) {
                existing.add(rs.getString("name"));
            }
            rs.close();
            for (String k : persistFields.keySet()) {
                if (!existing.contains(k)) {
                    String colType = getSQLType(persistFields.get(k).getType());
                    String alter = "ALTER TABLE " + tableName + " ADD COLUMN " + k + " " + colType + ";";
                    stmt.execute(alter);
                    DebugLogger.getInstance().log(Level.INFO, "Added missing column " + k + " to " + tableName, 0);
                }
            }

            DebugLogger.getInstance().log(Level.INFO, "Table " + tableName + " initialized.", 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Failed to init or migrate " + tableName, e);
            throw new RuntimeException(e);
        }
    }

    private String getSQLType(Class<?> cls) {
        if (cls == int.class || cls == Integer.class || cls == long.class || cls == Long.class ||
                cls == double.class || cls == Double.class || cls == float.class || cls == Float.class) {
            return "REAL";
        } else if (cls == boolean.class || cls == Boolean.class) {
            return "INTEGER";
        } else if (cls == String.class) {
            return "TEXT";
        } else {
            // for complex types, store JSON
            return "TEXT";
        }
    }

    @Override
    public void save(T entity, Callback<Void> callback) {
        Map<String, Object> data = serialize(entity);
        if (!data.containsKey(idField)) {
            callback.onFailure(new IllegalArgumentException("Missing ID field: " + idField));
            return;
        }
        Object idVal = data.get(idField);

        // Build upsert query
        StringBuilder fields = new StringBuilder();
        StringBuilder qs = new StringBuilder();
        List<String> updates = new ArrayList<>();

        int size = data.size();
        int idx = 0;
        for (String key : data.keySet()) {
            fields.append(key);
            qs.append('?');
            if (idx < size - 1) {
                fields.append(", ");
                qs.append(", ");
            }
            if (!key.equals(idField)) {
                updates.add(key + " = excluded." + key);
            }
            idx++;
        }

        String sql = "INSERT INTO " + tableName + " (" + fields + ") VALUES (" + qs + ")"
                + " ON CONFLICT(" + idField + ") DO UPDATE SET "
                + String.join(", ", updates) + ";";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            for (String key : data.keySet()) {
                Object value = data.get(key);
                if (value instanceof Map || value instanceof Collection) {
                    String json = gson.toJson(value);
                    pstmt.setString(paramIndex++, json);
                } else if (value instanceof Boolean) {
                    pstmt.setInt(paramIndex++, (Boolean) value ? 1 : 0);
                } else {
                    pstmt.setObject(paramIndex++, value);
                }
            }
            pstmt.executeUpdate();
            callback.onSuccess(null);
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void load(UUID uuid, Callback<T> callback) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + idField + " = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = extractFromResultSet(rs);
                T entity = deserialize(row);
                callback.onSuccess(entity);
            } else {
                callback.onFailure(new NoSuchElementException("Entity not found"));
            }
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void delete(UUID uuid, Callback<Void> callback) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idField + " = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                callback.onSuccess(null);
            } else {
                callback.onFailure(new NoSuchElementException("Entity not found to delete"));
            }
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void loadAll(Callback<List<T>> callback) {
        List<T> all = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Map<String, Object> row = extractFromResultSet(rs);
                T entity = deserialize(row);
                if (entity != null) {
                    all.add(entity);
                }
            }
            callback.onSuccess(all);
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void loadByDiscordId(long discordId, Callback<T> callback) {
        String sql = "SELECT * FROM " + tableName + " WHERE discordId = ? LIMIT 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = extractFromResultSet(rs);
                T entity = deserialize(row);
                if (entity != null) {
                    callback.onSuccess(entity);
                } else {
                    callback.onFailure(new NoSuchElementException("No entity found for discordId=" + discordId));
                }
            } else {
                callback.onFailure(new NoSuchElementException("No entity found for discordId=" + discordId));
            }
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    private Map<String, Object> extractFromResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        for (String key : persistFields.keySet()) {
            Object val = rs.getObject(key);
            if (val instanceof String && isComplexType(persistFields.get(key).getType())) {
                // JSON -> complex
                String json = (String) val;
                Field f = persistFields.get(key);
                Type fieldType = f.getGenericType();
                Object deserialized = gson.fromJson(json, fieldType);
                data.put(key, deserialized);
            } else if (val instanceof Integer && persistFields.get(key).getType() == boolean.class) {
                data.put(key, ((Integer) val) != 0);
            } else {
                data.put(key, val);
            }
        }
        return data;
    }

    private boolean isComplexType(Class<?> cls) {
        return !(cls.isPrimitive() ||
                cls == Integer.class ||
                cls == Long.class ||
                cls == Double.class ||
                cls == Float.class ||
                cls == Boolean.class ||
                cls == String.class);
    }
}
