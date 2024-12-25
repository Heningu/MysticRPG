package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.storage.annotations.Persist;

import java.util.*;

/**
 * Represents a player's data with fields marked for persistence.
 */
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

    @Persist
    private List<String> unlockedTitles;

    @Persist
    private String currentTitle;

    public PlayerData() {
        // Default constructor required for deserialization
    }
    private static List<MutableFieldRef> fieldRefs;


    {
        fieldRefs = new ArrayList<>();
        fieldRefs.add(new MutableFieldRef(
                () -> this.unlockedTitles,
                x -> this.unlockedTitles = (List<String>) x,
                CollectionKind.LIST
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.activeQuests,
                x -> this.activeQuests = (List<String>) x,
                CollectionKind.LIST
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.completedQuests,
                x -> this.completedQuests = (List<String>) x,
                CollectionKind.LIST
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.pendingItems,
                x -> this.pendingItems = (List<String>) x,
                CollectionKind.LIST
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.completedDialogues,
                x -> this.completedDialogues = (List<String>) x,
                CollectionKind.LIST
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.friends,
                x -> this.friends = (Set<String>) x,
                CollectionKind.SET
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.friendRequests,
                x -> this.friendRequests = (Set<String>) x,
                CollectionKind.SET
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.blockedPlayers,
                x -> this.blockedPlayers = (Set<String>) x,
                CollectionKind.SET
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.attributes,
                x -> this.attributes = (Map<String, Integer>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.unlockedRecipes,
                x -> this.unlockedRecipes = (Map<String, Boolean>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.questProgress,
                x -> this.questProgress = (Map<String, Map<String, Integer>>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.equipment,
                x -> this.equipment = (Map<String, String>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.questPhaseIndex,
                x -> this.questPhaseIndex = (Map<String, Integer>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.questStartTime,
                x -> this.questStartTime = (Map<String, Long>) x,
                CollectionKind.MAP
        ));
        fieldRefs.add(new MutableFieldRef(
                () -> this.ownedPets,
                x -> this.ownedPets = (Set<String>) x,
                CollectionKind.SET
        ));



    }

    public PlayerData(String uuid,
                      int heldGold,
                      int bankGold,
                      int xp,
                      int level,
                      int nextLevelXP,
                      int currentHp,
                      Map<String, Integer> attributes,
                      Map<String, Boolean> unlockedRecipes,
                      Set<String> friendRequests,
                      Set<String> friends,
                      Set<String> blockedPlayers,
                      boolean blockingRequests,
                      int attributePoints,
                      List<String> activeQuests,
                      Map<String, Map<String, Integer>> questProgress,
                      List<String> completedQuests,
                      String pinnedQuest,
                      int pendingBalance,
                      List<String> pendingItems,
                      boolean remindersEnabled,
                      Map<String, String> equipment,
                      List<String> completedDialogues,
                      Long discordId,
                      Map<String, Integer> questPhaseIndex,
                      Map<String, Long> questStartTime,
                      Set<String> ownedPets,
                      String equippedPet,
                      List<String> unlockedTitles,
                      String currentTitle) {

        this.uuid = uuid;
        this.heldGold = heldGold;
        this.bankGold = bankGold;
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
        this.unlockedTitles = unlockedTitles;
        this.currentTitle = currentTitle;
    }

    public static PlayerData defaultData(String uuid) {
        return new PlayerData(
                uuid,
                0,
                0,
                0,
                1,
                100,
                20,
                new HashMap<>(Map.of(
                        "HEALTH", 20,
                        "DEFENSE", 0,
                        "STRENGTH", 1,
                        "INTELLIGENCE", 1,
                        "CRIT_CHANCE", 5,
                        "CRIT_DAMAGE", 10,
                        "ATTACK_SPEED", 0,
                        "HEALTH_REGEN", 1,
                        "MOVEMENT_SPEED", 0,
                        "MANA", 10
                )),
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
                null,
                new ArrayList<>(),
                null
        );
    }

    /**
     * Ensures that collections are mutable after deserialization.
     */
    public void ensureMutableCollections() {
        for (MutableFieldRef ref : fieldRefs) {
            Object val = ref.getter().get(); // read the field
            if (val == null) {
                // If it's null, create a new empty collection
                switch (ref.collectionKind()) {
                    case LIST ->
                            ref.setter().accept(new ArrayList<>());
                    case SET ->
                            ref.setter().accept(new HashSet<>());
                    case MAP ->
                            ref.setter().accept(new HashMap<>());
                }
            } else {
                // It's non-null, we check if it's an instance of the correct *mutable* class
                switch (ref.collectionKind()) {
                    case LIST -> {
                        if (!(val instanceof ArrayList)) {
                            // Re-wrap
                            ref.setter().accept(new ArrayList<>((Collection<?>) val));
                        }
                    }
                    case SET -> {
                        if (!(val instanceof HashSet)) {
                            ref.setter().accept(new HashSet<>((Collection<?>) val));
                        }
                    }
                    case MAP -> {
                        if (!(val instanceof HashMap)) {
                            ref.setter().accept(new HashMap<>((Map<?, ?>) val));
                        }
                    }
                }
            }
        }
    }
    // Getters and setters


    public List<String> getUnlockedTitles() {
        return unlockedTitles;
    }

    public void setUnlockedTitles(List<String> unlockedTitles) {
        this.unlockedTitles = unlockedTitles;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }


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
