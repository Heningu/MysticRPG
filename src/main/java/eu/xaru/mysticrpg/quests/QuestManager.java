package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
                    QuestType type = QuestType.valueOf(typeStr);

                    String details = config.getString("details", "");
                    List<String> prerequisites = config.getStringList("prerequisites");

                    List<QuestPhase> phases = new ArrayList<>();
                    ConfigurationSection phasesSection = config.getConfigurationSection("phases");
                    if (phasesSection != null) {
                        for (String phaseKey : phasesSection.getKeys(false)) {
                            ConfigurationSection phSec = phasesSection.getConfigurationSection(phaseKey);
                            String phaseName = phSec.getString("name", phaseKey);
                            String dialogueStart = phSec.getString("dialogue_start", "");
                            String dialogueEnd = phSec.getString("dialogue_end", "");
                            List<String> objectives = phSec.getStringList("objectives");
                            long timeLimit = phSec.getLong("time_limit", 0);
                            Map<String, String> branches = new HashMap<>();
                            ConfigurationSection branchSec = phSec.getConfigurationSection("branches");
                            if (branchSec != null) {
                                for (String b : branchSec.getKeys(false)) {
                                    branches.put(b, branchSec.getString(b));
                                }
                            }
                            boolean showChoices = phSec.getBoolean("show_choices", false);
                            String nextPhase = phSec.getString("next_phase");

                            QuestPhase phase = new QuestPhase(phaseName, dialogueStart, dialogueEnd, objectives, timeLimit, branches, showChoices, nextPhase);
                            phases.add(phase);
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

                    String resetType = config.getString("reset_type", "none");

                    Quest quest = new Quest(id, name, levelRequirement, type, details, prerequisites, phases, rewards, resetType);
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
