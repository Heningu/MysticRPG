package eu.xaru.mysticrpg.storage;

import java.util.Map;
import java.util.Set;

public class PlayerData {
    private String uuid;
    private double balance;
    private int xp;
    private int level;
    private int nextLevelXP;
    private int currentHp;
    private Map<String, Integer> attributes;
    private Map<String, Boolean> unlockedRecipes;
    private Set<String> friendRequests;
    private Set<String> friends;
    private Set<String> blockedPlayers;
    private boolean blockingRequests;

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

    // Getters and setters
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getNextLevelXP() {
        return nextLevelXP;
    }

    public void setNextLevelXP(int nextLevelXP) {
        this.nextLevelXP = nextLevelXP;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public Map<String, Integer> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Integer> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Boolean> getUnlockedRecipes() {
        return unlockedRecipes;
    }

    public void setUnlockedRecipes(Map<String, Boolean> unlockedRecipes) {
        this.unlockedRecipes = unlockedRecipes;
    }

    public Set<String> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(Set<String> friendRequests) {
        this.friendRequests = friendRequests;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public void setFriends(Set<String> friends) {
        this.friends = friends;
    }

    public Set<String> getBlockedPlayers() {
        return blockedPlayers;
    }

    public void setBlockedPlayers(Set<String> blockedPlayers) {
        this.blockedPlayers = blockedPlayers;
    }

    public boolean isBlockingRequests() {
        return blockingRequests;
    }

    public void setBlockingRequests(boolean blockingRequests) {
        this.blockingRequests = blockingRequests;
    }
}
