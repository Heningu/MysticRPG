package eu.xaru.mysticrpg.quests;

import java.util.List;
import java.util.Map;

public class QuestPhase {
    private String name;
    private String dialogueStart;
    private String dialogueEnd;
    // Objectives are represented as a list of strings like "collect_item:OAK_LOG:16"
    private List<String> objectives;
    private long timeLimit; // in milliseconds, 0 = no limit
    private Map<String, String> branches;
    // branches can map a choice key to the next phase index or next quest ID. For simplicity, we store them as strings referencing the next phase ID or final quest completion scenario.

    private String nextPhase; // If linear progression, specify next phase ID
    // If branching occurs, nextPhase might be null and branches might be used.

    private boolean showChoices; // If true, player must choose a branch

    private String locationObjective; // For location-based objective: "go_to_location:world:x:y:z"

    public QuestPhase(String name, String dialogueStart, String dialogueEnd, List<String> objectives, long timeLimit, Map<String, String> branches, boolean showChoices, String nextPhase) {
        this.name = name;
        this.dialogueStart = dialogueStart;
        this.dialogueEnd = dialogueEnd;
        this.objectives = objectives;
        this.timeLimit = timeLimit;
        this.branches = branches;
        this.showChoices = showChoices;
        this.nextPhase = nextPhase;
    }

    public String getName() { return name; }
    public String getDialogueStart() { return dialogueStart; }
    public String getDialogueEnd() { return dialogueEnd; }
    public List<String> getObjectives() { return objectives; }
    public long getTimeLimit() { return timeLimit; }
    public Map<String, String> getBranches() { return branches; }
    public boolean isShowChoices() { return showChoices; }
    public String getNextPhase() { return nextPhase; }
    public String getObjectivesAsString() {
        return String.join(", ", objectives);
    }
}
