package eu.xaru.mysticrpg.quests;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestObjectivesHelper {

    // Parse a single objective string like "collect_item:OAK_LOG:16"
    // We'll return a struct or just handle them inline.
    public static boolean isObjectiveComplete(String objective, Map<String,Integer> progress) {
        // This will be checked by comparing progress map with required counts.
        // objective format samples:
        // "collect_item:OAK_LOG:16"
        // "kill_mob:ZOMBIE:5"
        // "talk_to_npc:lumberjack_npc_id"
        // "go_to_location:world:100:64:200"
        // If timed is integrated as a phase constraint, not objective line.

        String[] parts = objective.split(":");
        String type = parts[0];

        int current = progress.getOrDefault(objective, 0);

        switch (type) {
            case "collect_item":
            case "kill_mob":
                int required = Integer.parseInt(parts[2]);
                return current >= required;
            case "talk_to_npc":
            case "go_to_location":
                // These are binary objectives, if progress[objective] >= 1 means done
                return current >= 1;
            default:
                return false;
        }
    }

    public static boolean areAllObjectivesComplete(QuestPhase phase, Map<String, Integer> progress) {
        for (String obj : phase.getObjectives()) {
            if (!isObjectiveComplete(obj, progress)) {
                return false;
            }
        }
        return true;
    }
}
