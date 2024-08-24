package eu.xaru.mysticrpg.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    public UUID uuid;
    public double balance;
    public int xp;
    public int level;
    public int nextLevelXP;
    public int currentHp;
    public Map<String, Integer> attributes;
    public Map<String, Boolean> unlockedRecipes;
    public Set<UUID> friendRequests;
    public Set<UUID> friends;
    public Set<UUID> blockedPlayers;
    public boolean blockingRequests;

    public PlayerData() {
        // Default constructor required for POJO codec
    }

    public PlayerData(UUID uuid, double balance, int xp, int level, int nextLevelXP, int currentHp,
                      Map<String, Integer> attributes, Map<String, Boolean> unlockedRecipes,
                      Set<UUID> friendRequests, Set<UUID> friends, Set<UUID> blockedPlayers,
                      boolean blockingRequests) {
        this.uuid = uuid;
        this.balance = balance;
        this.xp = xp;
        this.level = level;
        this.nextLevelXP = nextLevelXP;
        this.currentHp = currentHp;
        this.attributes = attributes;
        this.unlockedRecipes = unlockedRecipes;
        this.friendRequests = friendRequests;
        this.friends = friends;
        this.blockedPlayers = blockedPlayers;
        this.blockingRequests = blockingRequests;
    }

    public static PlayerData defaultData(UUID uuid) {
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
