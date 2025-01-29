package eu.xaru.mysticrpg.storage.redis;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
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

    private RedisManager(boolean enabled, boolean mock, String host, int port, String password) {
        this.redisEnabled = enabled;
        this.mockInMemory = mock;

        if (redisEnabled && !mockInMemory) {
            try {
                // If your redis requires auth, you'd do that with Jedis
                jedisPool = new JedisPool(host, port);
                DebugLogger.getInstance().log(Level.INFO, "RedisManager: Connected to Redis at " + host + ":" + port, 0);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: failed to connect, using in-memory fallback", e);
                this.mockInMemory = true;
            }
        } else {
            DebugLogger.getInstance().log(Level.INFO, "RedisManager: using in-memory mock", 0);
        }
    }

    public static synchronized void initialize(DynamicConfig config) {
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
            throw new IllegalStateException("RedisManager not initialized.");
        }
        return instance;
    }

    public boolean isRedisFullyEnabled() {
        return redisEnabled && !mockInMemory && jedisPool != null;
    }

    public void set(String key, String value) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(key, value);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: set failed, fallback to mock", e);
                mockData.put(key, value);
            }
        } else {
            mockData.put(key, value);
        }
    }

    public String get(String key) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                return jedis.get(key);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: get failed, fallback to mock", e);
                return mockData.get(key);
            }
        }
        return mockData.get(key);
    }

    public void delete(String key) {
        if (isRedisFullyEnabled()) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(key);
            } catch (Exception e) {
                DebugLogger.getInstance().error("RedisManager: delete failed, fallback to mock", e);
                mockData.remove(key);
            }
        } else {
            mockData.remove(key);
        }
    }
}
