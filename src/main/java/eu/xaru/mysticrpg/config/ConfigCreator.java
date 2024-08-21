package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.cores.MysticCore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigCreator {
    private final MysticCore plugin;

    public ConfigCreator(MysticCore plugin) {
        this.plugin = plugin;
    }

    public void createFiles() {
        createConfigFile();
        createLevelsFile();
        createXPValuesFile();
        createPlayerDataFolder();
    }

    private void createConfigFile() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    private void createLevelsFile() {
        File levelsFolder = new File(plugin.getDataFolder(), "leveling");
        if (!levelsFolder.exists()) {
            levelsFolder.mkdirs();
        }

        File levelsFile = new File(levelsFolder, "Levels.json");
        if (!levelsFile.exists()) {
            try (InputStream in = plugin.getResource("leveling/Levels.json")) {
                Files.copy(in, levelsFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createXPValuesFile() {
        File levelsFolder = new File(plugin.getDataFolder(), "leveling");
        if (!levelsFolder.exists()) {
            levelsFolder.mkdirs();
        }

        File xpValuesFile = new File(levelsFolder, "XPValues.json");
        if (!xpValuesFile.exists()) {
            try (InputStream in = plugin.getResource("leveling/XPValues.json")) {
                Files.copy(in, xpValuesFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPlayerDataFolder() {
        File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }
}
