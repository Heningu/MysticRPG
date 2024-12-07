package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {

    private final Map<String, Quest> quests = new HashMap<>();
    private final JavaPlugin plugin;

    public QuestManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        loadQuests();
    }

    private void loadQuests() {
        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists() && !questsFolder.mkdirs()) {
            DebugLogger.getInstance().severe("Failed to create quests folder.");
            return;
        }

        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    String id = config.getString("id");
                    if (id == null || id.isEmpty()) {
                        DebugLogger.getInstance().severe("Quest ID is missing in file: " + file.getName());
                        continue;
                    }

                    String name = config.getString("name", "Unnamed Quest");
                    int levelRequirement = config.getInt("level_requirement", 1);
                    String typeStr = config.getString("type", "PVE").toUpperCase();
                    Quest.QuestType type = Quest.QuestType.valueOf(typeStr);

                    String details = config.getString("details", "");

                    List<String> prerequisites = config.getStringList("prerequisites");

                    Map<String, Integer> objectives = new HashMap<>();
                    ConfigurationSection objectivesSection = config.getConfigurationSection("objectives");
                    if (objectivesSection != null) {
                        for (String key : objectivesSection.getKeys(false)) {
                            int value = objectivesSection.getInt(key);
                            objectives.put(key, value);
                        }
                    }

                    Map<String, Object> rewards = new HashMap<>();
                    ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        for (String key : rewardsSection.getKeys(false)) {
                            Object value = rewardsSection.get(key);
                            rewards.put(key, value);
                        }
                    }

                    Quest quest = new Quest(id, name, levelRequirement, type, details, prerequisites, objectives, rewards);
                    quests.put(id, quest);

                    DebugLogger.getInstance().log(Level.INFO, "Loaded quest: " + id);
                } catch (Exception e) {
                    DebugLogger.getInstance().severe("Failed to load quest from file " + file.getName() + ":", e);
                }
            }
        }
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Quest getQuestByName(String name) {
        for (Quest quest : quests.values()) {
            if (quest.getName().equalsIgnoreCase(name)) {
                return quest;
            }
        }
        return null;
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }
}
