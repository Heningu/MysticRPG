package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.storage.annotations.Persist;

import java.util.*;

/**
 * Represents a player's data with fields marked for persistence.
 */
public class PlayerData {
    @Persist(key = "uuid")
    private String uuid;

    @Persist
    private int heldGold;

    @Persist
    private int bankGold;

    @Persist
    private int xp;

    @Persist
    private int level;

    @Persist
    private int nextLevelXP;

    @Persist
    private int currentHp;

    @Persist
    private Map<String, Integer> attributes;

    @Persist
    private Map<String, Boolean> unlockedRecipes;

    @Persist
    private Set<String> friendRequests;

    @Persist
    private Set<String> friends;

    @Persist
    private Set<String> blockedPlayers;

    @Persist
    private boolean blockingRequests;

    @Persist
    private int attributePoints;

    @Persist
    private List<String> activeQuests;

    @Persist
    private Map<String, Map<String, Integer>> questProgress;

    @Persist
    private List<String> completedQuests;

    @Persist
    private String pinnedQuest;

    @Persist
    private int pendingBalance;

    @Persist
    private List<String> pendingItems;

    @Persist
    private boolean remindersEnabled;

    @Persist
    private Map<String, String> equipment;

    @Persist
    private List<String> completedDialogues;

    @Persist
    private Long discordId;

    @Persist
    private Map<String, Integer> questPhaseIndex;

    @Persist
    private Map<String, Long> questStartTime;

    @Persist
    private Set<String> ownedPets;

    @Persist
    private String equippedPet;


    public PlayerData() {
        // Default constructor required for deserialization
    }

    public PlayerData(String uuid, int heldGold, int bankGold, int xp, int level, int nextLevelXP, int currentHp,
                      Map<String, Integer> attributes, Map<String, Boolean> unlockedRecipes,
                      Set<String> friendRequests, Set<String> friends, Set<String> blockedPlayers,
                      boolean blockingRequests, int attributePoints, List<String> activeQuests,
                      Map<String, Map<String, Integer>> questProgress, List<String> completedQuests,
                      String pinnedQuest, int pendingBalance, List<String> pendingItems,
                      boolean remindersEnabled, Map<String, String> equipment,
                      List<String> completedDialogues, Long discordId,
                      Map<String, Integer> questPhaseIndex, Map<String, Long> questStartTime, Set<String> ownedPets, String equippedPet) {
        this.uuid = uuid;
        this.bankGold = bankGold;
        this.heldGold = heldGold;
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
        this.equipment = equipment;
        this.completedDialogues = completedDialogues;
        this.discordId = discordId;
        this.questPhaseIndex = questPhaseIndex;
        this.questStartTime = questStartTime;
        this.ownedPets = ownedPets;
        this.equippedPet = equippedPet;
    }

    public static PlayerData defaultData(String uuid) {
        return new PlayerData(
                uuid,
                0, // heldGold default
                0, // bankGold default
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
                0,
                new ArrayList<>(),
                true,
                new HashMap<>(),
                new ArrayList<>(),
                null,
                new HashMap<>(),
                new HashMap<>(),
                new HashSet<>(),
                null
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

        if (!(ownedPets instanceof HashSet)) {
            ownedPets = new HashSet<>(ownedPets);
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
        if (!(completedDialogues instanceof ArrayList)) {
            completedDialogues = new ArrayList<>(completedDialogues);
        }
        if (!(equipment instanceof HashMap)) {
            equipment = new HashMap<>(equipment);
        }
        if (!(questPhaseIndex instanceof HashMap)) {
            questPhaseIndex = new HashMap<>(questPhaseIndex);
        }
        if (!(questStartTime instanceof HashMap)) {
            questStartTime = new HashMap<>(questStartTime);
        }
    }

    // Getters and setters

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getHeldGold() {
        return heldGold;
    }

    public void setHeldGold(int heldGold) {
        this.heldGold = heldGold;
    }

    public int getBankGold() {
        return bankGold;
    }

    public void setBankGold(int bankGold) {
        this.bankGold = bankGold;
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

    public Set<String> getOwnedPets() {
        return ownedPets;
    }
    public void setOwnedPets(Set<String> ownedPets) {
        this.ownedPets = ownedPets;
    }
    public String getEquippedPet() {
        return equippedPet;
    }
    public void setEquippedPet(String equippedPet) {
        this.equippedPet = equippedPet;
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

    public int getPendingBalance() {
        return pendingBalance;
    }

    public void setPendingBalance(int pendingBalance) {
        this.pendingBalance = pendingBalance;
    }

    public List<String> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<String> pendingItems) {
        this.pendingItems = pendingItems;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public Map<String, String> getEquipment() {
        return equipment;
    }

    public void setEquipment(Map<String, String> equipment) {
        this.equipment = equipment;
    }

    public List<String> getCompletedDialogues() {
        return completedDialogues;
    }

    public void setCompletedDialogues(List<String> completedDialogues) {
        this.completedDialogues = completedDialogues;
    }

    public Long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(Long discordId) {
        this.discordId = discordId;
    }

    public Map<String, Integer> getQuestPhaseIndex() {
        return questPhaseIndex;
    }

    public void setQuestPhaseIndex(Map<String, Integer> questPhaseIndex) {
        this.questPhaseIndex = questPhaseIndex;
    }

    public Map<String, Long> getQuestStartTime() {
        return questStartTime;
    }

    public void setQuestStartTime(Map<String, Long> questStartTime) {
        this.questStartTime = questStartTime;
    }
}
