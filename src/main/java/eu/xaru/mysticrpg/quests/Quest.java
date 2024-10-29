package eu.xaru.mysticrpg.quests;

import java.util.List;
import java.util.Map;

public class Quest {

    public enum QuestType {
        PVE,
        PVP
    }

    private String id;
    private String name;
    private int levelRequirement;
    private QuestType type;
    private String details;
    private List<String> prerequisites; // List of quest IDs that need to be completed
    private Map<String, Integer> objectives; // e.g., "kill_zombie": 10
    private Map<String, Object> rewards; // e.g., "currency": 100, "experience": 500, "items": ["sword", "shield"]

    public Quest(String id, String name, int levelRequirement, QuestType type, String details,
                 List<String> prerequisites, Map<String, Integer> objectives, Map<String, Object> rewards) {
        this.id = id;
        this.name = name;
        this.levelRequirement = levelRequirement;
        this.type = type;
        this.details = details;
        this.prerequisites = prerequisites;
        this.objectives = objectives;
        this.rewards = rewards;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public QuestType getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public Map<String, Integer> getObjectives() {
        return objectives;
    }

    public Map<String, Object> getRewards() {
        return rewards;
    }
}
