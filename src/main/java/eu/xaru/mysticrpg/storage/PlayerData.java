package eu.xaru.mysticrpg.storage;

import java.util.Map;
import java.util.Set;

public class PlayerData {
    public String uuid;
    public double balance;
    public int xp;
    public int level;
    public int nextLevelXP;
    public int currentHp;
    public Map<String, Integer> attributes;
    public Map<String, Boolean> unlockedRecipes;
    public Set<String> friendRequests;
    public Set<String> friends;
    public Set<String> blockedPlayers;
    public boolean blockingRequests;

    public PlayerData() {
        // Default constructor for MongoDB POJO codec
    }

    public PlayerData(String uuid, double balance, int xp, int level, int nextLevelXP, int currentHp,
                      Map<String, Integer> attributes, Map<String, Boolean> unlockedRecipes,
                      Set<String> friendRequests, Set<String> friends, Set<String> blockedPlayers,
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

    public static PlayerData defaultData(String uuid) {
        return new PlayerData(
                uuid,
                0.0,
                0,
                1,
                100,
                20,
                Map.of("HP", 20, "MANA", 10, "Vitality", 1, "Intelligence", 1, "Dexterity", 1, "Strength", 1),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                false
        );
    }
}
