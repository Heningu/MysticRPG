package eu.xaru.mysticrpg.config.format;

import org.bukkit.configuration.file.FileConfigurationOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Defines the core operations needed to load, save, and manage
 * config data in different formats (YAML, JSON).
 */
public interface IConfigBackend {

    /**
     * Loads (or reloads) from disk, merging defaults from an InputStream resource if provided.
     *
     * @param file         The file on disk to load from (creates if not exist).
     * @param resourceIn   The jar resource for defaults (null if none).
     * @return A set of default keys found in the resource (for logging/warning).
     * @throws IOException if something goes wrong reading/writing files.
     */
    Set<String> load(File file, InputStream resourceIn) throws IOException;

    /**
     * Saves the config to disk if necessary.
     * Implementations can track a 'changed' flag or simply always save.
     *
     * @param file The file to save to.
     * @throws IOException if save fails.
     */
    void save(File file) throws IOException;

    /**
     * Returns true if the path exists, false otherwise.
     */
    boolean contains(String path);

    /**
     * Gets an object (could be a primitive, String, List, Map, etc.).
     */
    Object get(String path);

    /**
     * Sets the path to the given value, marking as changed if different from old.
     */
    void set(String path, Object value);

    /**
     * Returns all top-level (or deep) keys.
     */
    Set<String> getKeys(boolean deep);

    /**
     * Some implementations (like YAML) can expose
     * a FileConfigurationOptions for advanced usage (may be null if unsupported).
     */
    FileConfigurationOptions getOptions();
}
