package eu.xaru.mysticrpg.quests;

import java.util.List;
import java.util.Map;

public class Quest {
    private String id;
    private String name;
    private int levelRequirement;
    private QuestType type;
    private String details;
    private List<String> prerequisites;
    private List<QuestPhase> phases;
    private Map<String, Object> rewards;

    // Daily/Weekly Quests (point 7)
    // "none", "daily", or "weekly"
    private String resetType;

    public Quest(String id, String name, int levelRequirement, QuestType type, String details,
                 List<String> prerequisites, List<QuestPhase> phases, Map<String, Object> rewards, String resetType) {
        this.id = id;
        this.name = name;
        this.levelRequirement = levelRequirement;
        this.type = type;
        this.details = details;
        this.prerequisites = prerequisites;
        this.phases = phases;
        this.rewards = rewards;
        this.resetType = resetType == null ? "none" : resetType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getLevelRequirement() { return levelRequirement; }
    public QuestType getType() { return type; }
    public String getDetails() { return details; }
    public List<String> getPrerequisites() { return prerequisites; }
    public List<QuestPhase> getPhases() { return phases; }
    public Map<String, Object> getRewards() { return rewards; }
    public String getResetType() { return resetType; }
}
