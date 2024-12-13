package eu.xaru.mysticrpg.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class ObjectiveFormatter {

    /**
     * Formats an objective string into a human-readable format with progress.
     *
     * @param objective The raw objective string (e.g., "collect_item:OAK_LOG:16").
     * @param current   The current progress count.
     * @return A formatted string (e.g., "Collect Oak Log 0/16").
     */
    public static String formatObjective(String objective, int current) {
        String[] parts = objective.split(":");
        if (parts.length < 2) {
            return objective; // Return as is if format is unexpected
        }

        String type = parts[0];
        switch (type) {
            case "collect_item":
                if (parts.length < 3) return objective;
                String itemName = formatItemName(parts[1]);
                int required = Integer.parseInt(parts[2]);
                return "Collect " + itemName + " " + current + "/" + required;

            case "kill_mob":
                if (parts.length < 3) return objective;
                String mobName = formatMobName(parts[1]);
                int mobRequired = Integer.parseInt(parts[2]);
                return "Kill " + mobName + " " + current + "/" + mobRequired;

            case "talk_to_npc":
                String npcName = formatNPCName(parts[1]);
                return "Talk to " + npcName;

            case "go_to_location":
                String location = formatLocation(parts);
                return "Go to " + location;

            default:
                return objective; // Unknown type
        }
    }

    /**
     * Formats the item name from uppercase with underscores to title case with spaces.
     *
     * @param rawName The raw item name (e.g., "OAK_LOG").
     * @return Formatted item name (e.g., "Oak Log").
     */
    private static String formatItemName(String rawName) {
        try {
            Material material = Material.valueOf(rawName.toUpperCase());
            String name = material.toString().toLowerCase().replace("_", " ");
            return capitalizeWords(name);
        } catch (IllegalArgumentException e) {
            return capitalizeWords(rawName.toLowerCase().replace("_", " "));
        }
    }

    /**
     * Formats the mob name from uppercase with underscores to title case with spaces.
     *
     * @param rawName The raw mob name (e.g., "ZOMBIE").
     * @return Formatted mob name (e.g., "Zombie").
     */
    private static String formatMobName(String rawName) {
        try {
            EntityType entityType = EntityType.valueOf(rawName.toUpperCase());
            String name = entityType.toString().toLowerCase().replace("_", " ");
            return capitalizeWords(name);
        } catch (IllegalArgumentException e) {
            return capitalizeWords(rawName.toLowerCase().replace("_", " "));
        }
    }

    /**
     * Formats the NPC name.
     *
     * @param rawName The raw NPC name.
     * @return Formatted NPC name.
     */
    private static String formatNPCName(String rawName) {
        return capitalizeWords(rawName.replace("_", " "));
    }

    /**
     * Formats the location string.
     *
     * @param parts The split parts of the location objective.
     * @return Formatted location string.
     */
    private static String formatLocation(String[] parts) {
        if (parts.length < 5) return "Unknown Location";
        String world = capitalizeWords(parts[1]);
        double x = Double.parseDouble(parts[2]);
        double y = Double.parseDouble(parts[3]);
        double z = Double.parseDouble(parts[4]);
        return world + " (" + (int)x + ", " + (int)y + ", " + (int)z + ")";
    }

    /**
     * Capitalizes the first letter of each word.
     *
     * @param input The input string.
     * @return The capitalized string.
     */
    private static String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if(word.length() == 0) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
