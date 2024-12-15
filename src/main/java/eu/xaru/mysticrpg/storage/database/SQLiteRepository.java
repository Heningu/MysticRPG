package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLogger;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite implementation of the IRepository interface with automatic schema migration.
 *
 * @param <T> The type of the data model.
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

    /**
     * Establishes a connection to the SQLite database.
     *
     * @param databasePath The path to the SQLite database file.
     * @return The Connection instance.
     */
    private Connection connect(String databasePath) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            DebugLogger.getInstance().log(Level.INFO, "Connected to SQLite database at: " + databasePath, 0);
            return conn;
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Failed to connect to SQLite database at: " + databasePath, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the table by creating it if it doesn't exist and performing schema migrations.
     */
    private void initializeTable() {
        try (Statement stmt = connection.createStatement()) {
            // Create table if it does not exist
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            sqlBuilder.append(idField).append(" TEXT PRIMARY KEY, ");

            int count = 0;
            for (String key : persistFields.keySet()) {
                Field field = persistFields.get(key);
                String sqlType = getSQLType(field.getType());
                sqlBuilder.append(key).append(" ").append(sqlType);
                if (++count < persistFields.size()) {
                    sqlBuilder.append(", ");
                }
            }
            sqlBuilder.append(");");
            stmt.execute(sqlBuilder.toString());

            // Perform schema migration: add missing columns
            String pragmaQuery = "PRAGMA table_info(" + tableName + ");";
            ResultSet rs = stmt.executeQuery(pragmaQuery);
            Set<String> existingColumns = new HashSet<>();
            while (rs.next()) {
                existingColumns.add(rs.getString("name"));
            }
            rs.close();

            for (String key : persistFields.keySet()) {
                if (!existingColumns.contains(key)) {
                    String sqlType = getSQLType(persistFields.get(key).getType());
                    String alterSQL = "ALTER TABLE " + tableName + " ADD COLUMN " + key + " " + sqlType + ";";
                    stmt.execute(alterSQL);
                    DebugLogger.getInstance().log(Level.INFO, "Added missing column '" + key + "' to table '" + tableName + "'.", 0);
                }
            }

            DebugLogger.getInstance().log(Level.INFO, "Table '" + tableName + "' initialized and migrated successfully.", 0);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Failed to initialize or migrate table: " + tableName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines the SQL data type based on the Java field type.
     *
     * @param fieldType The Java field type.
     * @return The corresponding SQL data type.
     */
    private String getSQLType(Class<?> fieldType) {
        if (fieldType == int.class || fieldType == Integer.class ||
                fieldType == long.class || fieldType == Long.class ||
                fieldType == double.class || fieldType == Double.class ||
                fieldType == float.class || fieldType == Float.class) {
            return "REAL";
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return "INTEGER";
        } else if (fieldType == String.class) {
            return "TEXT";
        } else {
            // For complex types, store as JSON
            return "TEXT";
        }
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

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
        sqlBuilder.append(String.join(", ", data.keySet()));
        sqlBuilder.append(") VALUES (");
        sqlBuilder.append(String.join(", ", Collections.nCopies(data.size(), "?")));
        sqlBuilder.append(") ON CONFLICT(").append(idField).append(") DO UPDATE SET ");
        List<String> updates = new ArrayList<>();
        for (String key : data.keySet()) {
            if (!key.equals(idField)) {
                updates.add(key + " = excluded." + key);
            }
        }
        sqlBuilder.append(String.join(", ", updates));
        sqlBuilder.append(";");

        try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
            int index = 1;
            for (String key : data.keySet()) {
                Object value = data.get(key);
                if (value instanceof Map || value instanceof Collection) {
                    // Serialize complex types as JSON
                    String json = gson.toJson(value);
                    pstmt.setString(index++, json);
                } else if (value instanceof Boolean) {
                    pstmt.setInt(index++, (Boolean) value ? 1 : 0);
                } else {
                    pstmt.setObject(index++, value);
                }
            }
            pstmt.executeUpdate();
            DebugLogger.getInstance().log(Level.INFO, "Entity saved successfully with ID: " + idValue, 0);
            callback.onSuccess(null);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error saving entity with ID: " + idValue, e);
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
                Map<String, Object> data = extractDataFromResultSet(rs);
                T entity = deserialize(data);
                DebugLogger.getInstance().log(Level.INFO, "Entity loaded successfully with ID: " + uuid, 0);
                callback.onSuccess(entity);
            } else {
                DebugLogger.getInstance().log(Level.INFO, "No entity found with UUID: " + uuid, 0);
                callback.onFailure(new NoSuchElementException("Entity not found"));
            }
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error loading entity with UUID: " + uuid, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void delete(UUID uuid, Callback<Void> callback) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idField + " = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                DebugLogger.getInstance().log(Level.INFO, "Entity deleted successfully with UUID: " + uuid, 0);
                callback.onSuccess(null);
            } else {
                DebugLogger.getInstance().log(Level.INFO, "No entity found to delete with UUID: " + uuid, 0);
                callback.onFailure(new NoSuchElementException("Entity not found"));
            }
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error deleting entity with UUID: " + uuid, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void loadAll(Callback<List<T>> callback) {
        List<T> entities = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + ";";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Map<String, Object> data = extractDataFromResultSet(rs);
                T entity = deserialize(data);
                if (entity != null) {
                    entities.add(entity);
                }
            }
            DebugLogger.getInstance().log(Level.INFO, "All entities loaded successfully. Total: " + entities.size(), 0);
            callback.onSuccess(entities);
        } catch (SQLException e) {
            DebugLogger.getInstance().error("Error loading all entities.", e);
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
                Map<String, Object> data = extractDataFromResultSet(rs);
                T entity = deserialize(data);
                if (entity != null) {
                    callback.onSuccess(entity);
                } else {
                    callback.onFailure(new NoSuchElementException("No entity found with discordId: " + discordId));
                }
            } else {
                callback.onFailure(new NoSuchElementException("No entity found with discordId: " + discordId));
            }
        } catch (SQLException e) {
            callback.onFailure(e);
        }
    }

    /**
     * Extracts the data from a ResultSet row into a Map for deserialization.
     *
     * @param rs The ResultSet positioned at the current row.
     * @return A Map with field names and their corresponding values.
     */
    private Map<String, Object> extractDataFromResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        for (String key : persistFields.keySet()) {
            Object value = rs.getObject(key);
            if (value instanceof String && isComplexType(persistFields.get(key).getType())) {
                // Deserialize JSON to complex types
                Field field = persistFields.get(key);
                Type typeOfField = field.getGenericType();
                Object deserialized = gson.fromJson((String) value, typeOfField);
                data.put(key, deserialized);
            } else if (value instanceof Integer && persistFields.get(key).getType() == boolean.class) {
                data.put(key, ((Integer) value) != 0);
            } else {
                data.put(key, value);
            }
        }
        return data;
    }

    /**
     * Checks if a field type is a complex type (not primitive or String).
     *
     * @param fieldType The field type.
     * @return True if complex, false otherwise.
     */
    private boolean isComplexType(Class<?> fieldType) {
        return !(fieldType.isPrimitive() ||
                fieldType == Integer.class ||
                fieldType == Long.class ||
                fieldType == Double.class ||
                fieldType == Float.class ||
                fieldType == Boolean.class ||
                fieldType == String.class);
    }
}
