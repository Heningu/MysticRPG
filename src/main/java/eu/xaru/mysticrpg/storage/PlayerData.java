package eu.xaru.mysticrpg.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PlayerData(
        UUID uuid,
        double balance,
        int xp,
        int level,
        int nextLevelXP,
        int currentHp,
        Map<String, Integer> attributes,
        Map<String, Boolean> unlockedRecipes,
        Set<UUID> friendRequests,
        Set<UUID> friends,
        Set<UUID> blockedPlayers,
        boolean blockingRequests
) {
    public static PlayerData defaultData(UUID uuid) {
        // Provide default values for a new player
        return new PlayerData(
                uuid,
                0.0,
                0,
                1,
                100,
                20,
                Map.of(
                        "HP", 20,
                        "MANA", 10,
                        "Vitality", 1,
                        "Intelligence", 1,
                        "Dexterity", 1,
                        "Strength", 1
                ),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                false
        );
    }
}
