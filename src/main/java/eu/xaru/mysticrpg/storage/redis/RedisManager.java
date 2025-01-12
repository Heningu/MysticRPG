package eu.xaru.mysticrpg.storage.redis;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages optional Redis connectivity or an in-memory fallback (mock).
 */
public class RedisManager {

    private static RedisManager instance;

    private final boolean redisEnabled;
    private boolean mockInMemory;
    private JedisPool jedisPool;

    private final Map<String, String> mockData = new ConcurrentHashMap<>();

    /**
     * Private constructor. Use initialize(...) to set up the singleton.
     */
    private RedisManager(boolean enabled, boolean mock, String host, int port, String password) {
        this.redisEnabled = enabled;
        this.mockInMemory = mock;

        if (redisEnabled && !mockInMemory) {
            try {
                // Connect to Redis (password handling may vary)
                jedisPool = new JedisPool(host, port);
                DebugLogger.getInstance().log(Level.INFO,
                        "RedisManager: Connected to Redis at " + host + ":" + port, 0);
                // If password is required, you might do Jedis auth each time you get a resource.
                // e.g., if(!password.isEmpty()) { jedis.auth(password); } inside try-with-resources.
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: failed to connect, using in-memory fallback", e);
                this.mockInMemory = true;
            }
        } else {
            DebugLogger.getInstance().log(Level.INFO, "RedisManager: using in-memory mock", 0);
        }
    }

    /**
     * Initializes the RedisManager singleton using values from a Bukkit {@link FileConfiguration}.
     *
     * @param config the plugin's config containing "database.redis.*" keys
     */
    public static synchronized void initialize(FileConfiguration config) {
        if (instance == null) {
            boolean enabled = config.getBoolean("database.redis.enabled", false);
            boolean mock = config.getBoolean("database.redis.mockInMemory", false);
            String host = config.getString("database.redis.host", "localhost");
            int port = config.getInt("database.redis.port", 6379);
            String password = config.getString("database.redis.password", "");
            instance = new RedisManager(enabled, mock, host, port, password);
        }
    }

    public static RedisManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RedisManager not initialized. Call initialize(...) first!");
        }
        return instance;
    }

    public boolean isRedisFullyEnabled() {
        return redisEnabled && !mockInMemory && jedisPool != null;
    }

    public void set(String key, String value) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                // If a password is needed:
                // if (!password.isEmpty()) { jedis.auth(password); }
                jedis.set(key, value);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: set failed, falling back to mock data", e);
                mockData.put(key, value);
            }
        } else {
            mockData.put(key, value);
        }
    }

    public String get(String key) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                // if (!password.isEmpty()) { jedis.auth(password); }
                return jedis.get(key);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: get failed, falling back to mock data", e);
                return mockData.get(key);
            }
        }
        return mockData.get(key);
    }

    public void delete(String key) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                // if (!password.isEmpty()) { jedis.auth(password); }
                jedis.del(key);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: delete failed, falling back to mock data", e);
                mockData.remove(key);
            }
        } else {
            mockData.remove(key);
        }
    }
}
