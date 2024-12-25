package eu.xaru.mysticrpg.config.format;

import com.google.gson.*;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfigurationOptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JSON-based backend using Gson for simple key->value structures.
 * For nested lists/maps, it merges them. No direct "copyDefaults" is done
 * unless you parse the resource and do a manual merge.
 *
 * If you want advanced "config sections" logic, you'll have to adapt it more deeply.
 */
public class JsonConfigBackend implements IConfigBackend {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Object> root;
    private boolean changed;
    private Set<String> defaultKeys;

    @Override
    public Set<String> load(File file, InputStream resourceIn) throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        // read existing
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            if (file.length() == 0) {
                root = new LinkedHashMap<>();
            } else {
                root = gson.fromJson(reader, Map.class);
                if (root == null) {
                    root = new LinkedHashMap<>();
                }
            }
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to parse JSON config. Creating new one.", e);
            root = new LinkedHashMap<>();
        }

        defaultKeys = new HashSet<>();

        // Merge defaults if resourceIn is provided
        if (resourceIn != null) {
            try (InputStreamReader isr = new InputStreamReader(resourceIn, StandardCharsets.UTF_8)) {
                Map<String, Object> defMap = gson.fromJson(isr, Map.class);
                if (defMap != null) {
                    mergeMaps(defMap, root); // merges defMap into root
                    defaultKeys = collectAllKeys(defMap, "");
                }
            }
        }

        save(file); // write out any merges
        changed = false;
        return defaultKeys;
    }

    private void mergeMaps(Map<String, Object> from, Map<String, Object> into) {
        for (Map.Entry<String, Object> e : from.entrySet()) {
            if (!into.containsKey(e.getKey())) {
                // doesn't exist => add
                into.put(e.getKey(), e.getValue());
                changed = true;
            } else {
                // If it's a map, we can do deep merging
                Object existingVal = into.get(e.getKey());
                if (existingVal instanceof Map<?,?> && e.getValue() instanceof Map<?,?>) {
                    // cast
                    Map<String, Object> existingMap = (Map<String, Object>) existingVal;
                    Map<String, Object> fromMap = (Map<String, Object>) e.getValue();
                    mergeMaps(fromMap, existingMap);
                }
                // else we do not override user values
            }
        }
    }

    private Set<String> collectAllKeys(Map<String,Object> map, String prefix) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String,Object> e : map.entrySet()) {
            String full = prefix.isEmpty() ? e.getKey() : prefix+"."+e.getKey();
            result.add(full);
            if (e.getValue() instanceof Map<?,?> m2) {
                result.addAll(collectAllKeys((Map<String,Object>)m2, full));
            }
        }
        return result;
    }

    @Override
    public void save(File file) throws IOException {
        if (!changed && file.length() > 0) {
            // No changes => skip
            return;
        }
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }
        changed = false;
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public Object get(String path) {
        if (path == null || path.isEmpty()) return root;
        String[] parts = path.split("\\.");
        Map<String, Object> cursor = root;
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i];
            if (!cursor.containsKey(key)) {
                return null;
            }
            if (i == parts.length - 1) {
                return cursor.get(key);
            }
            Object val = cursor.get(key);
            if (!(val instanceof Map<?,?>)) {
                return null;
            }
            cursor = (Map<String, Object>)val;
        }
        return null;
    }

    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String,Object> cursor = root;
        for (int i=0; i< parts.length -1; i++) {
            String k = parts[i];
            if (!cursor.containsKey(k) || !(cursor.get(k) instanceof Map<?,?>)) {
                Map<String,Object> newMap = new LinkedHashMap<>();
                cursor.put(k, newMap);
                changed = true;
                cursor = newMap;
            } else {
                cursor = (Map<String,Object>)cursor.get(k);
            }
        }
        String lastKey = parts[parts.length-1];
        Object oldVal = cursor.get(lastKey);
        if (!Objects.equals(oldVal, value)) {
            cursor.put(lastKey, value);
            changed = true;
        }
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (!deep) {
            return root.keySet();
        }
        // Collect all
        return collectAllKeys(root, "");
    }

    @Override
    public FileConfigurationOptions getOptions() {
        // Not applicable for JSON, return null
        return null;
    }
}
