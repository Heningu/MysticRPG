package eu.xaru.mysticrpg.storage;

import java.util.*;

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
    private int attributePoints;
    private List<String> activeQuests;
    private Map<String, Map<String, Integer>> questProgress;
    private List<String> completedQuests;
    private String pinnedQuest;
    private boolean remindersEnabled;
    private double pendingBalance;
    private List<String> pendingItems;

    // New field for Discord User ID (nullable)
    private Long discordId;

    public PlayerData() {
        // Default constructor for MongoDB POJO codec
    }

    public PlayerData(String uuid, double balance, int xp, int level, int nextLevelXP, int currentHp,
                      Map<String, Integer> attributes, Map<String, Boolean> unlockedRecipes,
                      Set<String> friendRequests, Set<String> friends, Set<String> blockedPlayers,
                      boolean blockingRequests, int attributePoints, List<String> activeQuests,
                      Map<String, Map<String, Integer>> questProgress, List<String> completedQuests,
                      String pinnedQuest, double pendingBalance, List<String> pendingItems,
                      boolean remindersEnabled, Long discordId) { // Updated constructor
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
        this.attributePoints = attributePoints;
        this.activeQuests = activeQuests;
        this.questProgress = questProgress;
        this.completedQuests = completedQuests;
        this.pinnedQuest = pinnedQuest;
        this.pendingBalance = pendingBalance;
        this.pendingItems = pendingItems;
        this.remindersEnabled = remindersEnabled;
        this.discordId = discordId;
    }

    public static PlayerData defaultData(String uuid) {
        return new PlayerData(
                uuid,
                0.0,
                0,
                1,
                100,
                20,
                new HashMap<>(Map.of("HP", 20, "MANA", 10, "Vitality", 1, "Intelligence", 1, "Dexterity", 1, "Strength", 1)),
                new HashMap<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                false,
                1,
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>(),
                null,
                0.0,
                new ArrayList<>(),
                true, // Reminders enabled by default
                null  // Discord ID initially null
        );
    }

    /**
     * Ensures that collections are mutable after deserialization.
     */
    public void ensureMutableCollections() {
        if (!(friendRequests instanceof HashSet)) {
            friendRequests = new HashSet<>(friendRequests);
        }
        if (!(friends instanceof HashSet)) {
            friends = new HashSet<>(friends);
        }
        if (!(blockedPlayers instanceof HashSet)) {
            blockedPlayers = new HashSet<>(blockedPlayers);
        }
        if (!(attributes instanceof HashMap)) {
            attributes = new HashMap<>(attributes);
        }
        if (!(unlockedRecipes instanceof HashMap)) {
            unlockedRecipes = new HashMap<>(unlockedRecipes);
        }
        if (!(activeQuests instanceof ArrayList)) {
            activeQuests = new ArrayList<>(activeQuests);
        }
        if (!(questProgress instanceof HashMap)) {
            questProgress = new HashMap<>(questProgress);
        }
        if (!(completedQuests instanceof ArrayList)) {
            completedQuests = new ArrayList<>(completedQuests);
        }
        if (!(pendingItems instanceof ArrayList)) {
            pendingItems = new ArrayList<>(pendingItems);
        }
    }

    // Getters and setters for all fields

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

    public int getNextLevelXP() {
        return nextLevelXP;
    }

    public void setNextLevelXP(int nextLevelXP) {
        this.nextLevelXP = nextLevelXP;
    }

    public void setLevel(int level) {
        this.level = level;
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

    public int getAttributePoints() {
        return attributePoints;
    }

    public void setAttributePoints(int attributePoints) {
        this.attributePoints = attributePoints;
    }

    public List<String> getActiveQuests() {
        return activeQuests;
    }

    public void setActiveQuests(List<String> activeQuests) {
        this.activeQuests = activeQuests;
    }

    public Map<String, Map<String, Integer>> getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(Map<String, Map<String, Integer>> questProgress) {
        this.questProgress = questProgress;
    }

    public List<String> getCompletedQuests() {
        return completedQuests;
    }

    public void setCompletedQuests(List<String> completedQuests) {
        this.completedQuests = completedQuests;
    }

    public String getPinnedQuest() {
        return pinnedQuest;
    }

    public void setPinnedQuest(String pinnedQuest) {
        this.pinnedQuest = pinnedQuest;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public double getPendingBalance() {
        return pendingBalance;
    }

    public void setPendingBalance(double pendingBalance) {
        this.pendingBalance = pendingBalance;
    }

    public List<String> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<String> pendingItems) {
        this.pendingItems = pendingItems;
    }

    // New getter and setter for Discord ID

    public Long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(Long discordId) {
        this.discordId = discordId;
    }
}
