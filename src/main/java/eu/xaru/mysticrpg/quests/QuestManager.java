package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the loading, storing, and progression logic of Quests,
 * using the new DynamicConfig system (nested Maps instead of ConfigurationSections).
 */
public class QuestManager {

    private final Map<String, Quest> quests = new HashMap<>();
    private final JavaPlugin plugin;
    private final PlayerDataCache playerDataCache;
    private final ItemManager itemManager;

    public QuestManager(JavaPlugin plugin, PlayerDataCache playerDataCache, ItemManager itemManager) {
        this.plugin = plugin;
        this.playerDataCache = playerDataCache;
        this.itemManager = itemManager;
        loadQuests();
    }

    /**
     * Loads all quests from the /quests folder using DynamicConfigManager.
     * No calls to ConfigurationSection or getKeysByPath().
     */
    private void loadQuests() {
        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists() && !questsFolder.mkdirs()) {
            DebugLogger.getInstance().severe("Failed to create quests folder.");
            return;
        }

        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String userFileName = "quests/" + file.getName();
                    // 1) Register/load config in manager
                    DynamicConfigManager.loadConfig(userFileName, userFileName);
                    // 2) Retrieve the config
                    DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
                    if (config == null) {
                        DebugLogger.getInstance().severe("Could not load DynamicConfig for " + userFileName);
                        continue;
                    }

                    // 3) Read quest data
                    String id = config.getString("id", "").trim();
                    if (id.isEmpty()) {
                        DebugLogger.getInstance().severe("Quest ID is missing in file: " + file.getName());
                        continue;
                    }

                    String name = config.getString("name", "Unnamed Quest");
                    int levelRequirement = config.getInt("level_requirement", 1);
                    String typeStr = config.getString("type", "PVE").toUpperCase(Locale.ROOT);
                    QuestType type = QuestType.valueOf(typeStr);

                    String details = config.getString("details", "");
                    List<String> prerequisites = config.getStringList("prerequisites", new ArrayList<>());

                    // Load phases from a nested Map
                    List<QuestPhase> phases = new ArrayList<>();
                    Object phasesObj = config.get("phases");
                    if (phasesObj instanceof Map<?,?> phasesMap) {
                        // For each "phaseKey" -> subMap
                        for (Map.Entry<?,?> phaseEntry : phasesMap.entrySet()) {
                            String phaseKey = String.valueOf(phaseEntry.getKey());
                            if (phaseEntry.getValue() instanceof Map<?,?> phaseDataMap) {
                                // Read fields from the sub-map
                                String phaseName = parseString(phaseDataMap.get("name"), phaseKey);
                                String dialogueStart = parseString(phaseDataMap.get("dialogue_start"), "");
                                String dialogueEnd = parseString(phaseDataMap.get("dialogue_end"), "");

                                // objectives: a List<String> if present
                                List<String> objectives = new ArrayList<>();
                                Object objList = phaseDataMap.get("objectives");
                                if (objList instanceof List<?> rawList) {
                                    for (Object o : rawList) {
                                        objectives.add(String.valueOf(o));
                                    }
                                }

                                long timeLimit = parseLong(phaseDataMap.get("time_limit"), 0L);

                                // branches: a sub-map
                                Map<String, String> branches = new HashMap<>();
                                Object branchObj = phaseDataMap.get("branches");
                                if (branchObj instanceof Map<?,?> branchMap) {
                                    for (Map.Entry<?,?> branchEntry : branchMap.entrySet()) {
                                        String bKey = String.valueOf(branchEntry.getKey());
                                        String bVal = parseString(branchEntry.getValue(), null);
                                        if (bVal != null) {
                                            branches.put(bKey, bVal);
                                        }
                                    }
                                }

                                boolean showChoices = parseBoolean(phaseDataMap.get("show_choices"), false);
                                String nextPhase = parseString(phaseDataMap.get("next_phase"), null);

                                QuestPhase phase = new QuestPhase(
                                        phaseName,
                                        dialogueStart,
                                        dialogueEnd,
                                        objectives,
                                        timeLimit,
                                        branches,
                                        showChoices,
                                        nextPhase
                                );
                                phases.add(phase);
                            }
                        }
                    }

                    // Load rewards
                    Map<String, Object> rewards = new HashMap<>();
                    Object rewardsObj = config.get("rewards");
                    if (rewardsObj instanceof Map<?,?> rewMap) {
                        // e.g. "currency", "experience", "items", ...
                        for (Map.Entry<?,?> e : rewMap.entrySet()) {
                            String key = String.valueOf(e.getKey());
                            rewards.put(key, e.getValue());
                        }
                    }

                    String resetType = config.getString("reset_type", "none");

                    Quest quest = new Quest(
                            id,
                            name,
                            levelRequirement,
                            type,
                            details,
                            prerequisites,
                            phases,
                            rewards,
                            resetType
                    );
                    quests.put(id, quest);

                    DebugLogger.getInstance().log(Level.INFO, "Loaded quest: " + id);
                } catch (Exception e) {
                    DebugLogger.getInstance().severe("Failed to load quest from file " + file.getName() + ":", e);
                }
            }
        }
    }

    // --- Helper parsing methods for raw Objects ---

    private String parseString(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }

    private long parseLong(Object val, long fallback) {
        if (val instanceof Number) {
            return ((Number)val).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object val, boolean fallback) {
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        if (val instanceof String) {
            String s = ((String) val).toLowerCase(Locale.ROOT);
            if (s.equals("true") || s.equals("yes") || s.equals("1")) return true;
            if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
        }
        return fallback;
    }

    // ----------------------------------------------------
    // All original quest management methods below are
    // unchanged from your snippet, except the constructor
    // calls "loadQuests()" with the revised approach.
    // ----------------------------------------------------

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Quest getQuestByName(String name) {
        for (Quest quest : quests.values()) {
            if (quest.getName().equalsIgnoreCase(name)) {
                return quest;
            }
        }
        return null;
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }

    public void startQuestForPlayer(PlayerData data, String questId) {
        data.getActiveQuests().add(questId);
        data.getQuestProgress().put(questId, new HashMap<>());
        data.getQuestPhaseIndex().put(questId, 0);
        data.getQuestStartTime().put(questId, System.currentTimeMillis());
    }

    public void resetQuestForPlayer(PlayerData data, String questId) {
        data.getActiveQuests().remove(questId);
        data.getCompletedQuests().remove(questId);
        data.getQuestProgress().remove(questId);
        data.getQuestPhaseIndex().remove(questId);
        data.getQuestStartTime().remove(questId);
        if (questId.equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
        }
    }

    public void updateObjectiveProgress(PlayerData data, String objectivePrefix, int amount) {
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
            if (phaseIndex >= quest.getPhases().size()) continue;
            QuestPhase phase = quest.getPhases().get(phaseIndex);

            Map<String,Integer> progressMap = data.getQuestProgress().getOrDefault(questId, new HashMap<>());
            boolean updated = false;
            for (String obj : phase.getObjectives()) {
                if (obj.startsWith("collect_item:") && objectivePrefix.startsWith("collect_item:")) {
                    String requiredMat = obj.split(":")[1];
                    if (objectivePrefix.contains(requiredMat)) {
                        int current = progressMap.getOrDefault(obj,0);
                        int required = Integer.parseInt(obj.split(":")[2]);
                        int newVal = Math.min(current+amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if (obj.startsWith("kill_mob:") && objectivePrefix.startsWith("kill_mob:")) {
                    String requiredMob = obj.split(":")[1];
                    if (objectivePrefix.contains(requiredMob)) {
                        int current = progressMap.getOrDefault(obj,0);
                        int required = Integer.parseInt(obj.split(":")[2]);
                        int newVal = Math.min(current+amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if ((obj.startsWith("talk_to_npc:") && objectivePrefix.startsWith("talk_to_npc:")) ||
                        (obj.startsWith("go_to_location:") && objectivePrefix.startsWith("go_to_location:"))) {
                    if (obj.equals(objectivePrefix)) {
                        progressMap.put(obj,1);
                        updated = true;
                    }
                }
            }
            if (updated) {
                data.getQuestProgress().put(questId, progressMap);
                checkPhaseCompletion(data, quest, phaseIndex);
            }
        }
    }

    private void checkPhaseCompletion(PlayerData data, Quest quest, int phaseIndex) {
        QuestPhase phase = quest.getPhases().get(phaseIndex);
        Map<String,Integer> progress = data.getQuestProgress().get(quest.getId());

        long start = data.getQuestStartTime().getOrDefault(quest.getId(),0L);
        if (phase.getTimeLimit() > 0 && System.currentTimeMillis()-start > phase.getTimeLimit()) {
            resetQuestForPlayer(data, quest.getId());
            Player player = Bukkit.getPlayer(UUID.fromString(data.getUuid()));
            if (player != null) player.sendMessage(Utils.getInstance().$("You ran out of time to complete the phase! Quest failed."));
            return;
        }

        if (QuestObjectivesHelper.areAllObjectivesComplete(phase, progress)) {
            Player player = Bukkit.getPlayer(UUID.fromString(data.getUuid()));
            if (player != null) {
                player.sendMessage(Utils.getInstance().$("Phase completed!"));
                if (!phase.getDialogueEnd().isEmpty()) {
                    player.sendMessage(Utils.getInstance().$(phase.getDialogueEnd()));
                }

                String nextStepMessage = "Check quest log";
                if (phase.getNextPhase() != null) {
                    nextStepMessage = "Next step: " + phase.getNextPhase();
                }

                player.sendTitle(
                        Utils.getInstance().$("Phase Completed!"),
                        Utils.getInstance().$(nextStepMessage),
                        10, 70, 20
                );
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            if (phase.isShowChoices() && phase.getBranches() != null && !phase.getBranches().isEmpty()) {
                if (player != null) {
                    player.sendMessage(Utils.getInstance().$("Please choose your path:"));
                    for (Map.Entry<String,String> e : phase.getBranches().entrySet()) {
                        player.sendMessage(Utils.getInstance().$("[Choice: "+e.getKey()+"] /questchoose "+quest.getId()+" "+e.getKey()));
                    }
                }
            } else if (phase.getNextPhase() == null && (phase.getBranches() == null || phase.getBranches().isEmpty())) {
                completeQuest(player, data, quest);
            } else if (phase.getNextPhase() != null) {
                int nextIndex = getPhaseIndexByName(quest, phase.getNextPhase());
                data.getQuestPhaseIndex().put(quest.getId(), nextIndex);
                data.getQuestStartTime().put(quest.getId(), System.currentTimeMillis());
                Player p = Bukkit.getPlayer(UUID.fromString(data.getUuid()));
                if (p != null && !quest.getPhases().get(nextIndex).getDialogueStart().isEmpty()) {
                    p.sendMessage(Utils.getInstance().$(quest.getPhases().get(nextIndex).getDialogueStart()));
                }
            }
        }
    }

    private int getPhaseIndexByName(Quest quest, String name) {
        for (int i=0; i<quest.getPhases().size(); i++) {
            if (quest.getPhases().get(i).getName().equalsIgnoreCase(name)) return i;
        }
        return 0; // fallback
    }

    public void completeQuest(Player player, PlayerData data, Quest quest) {
        data.getActiveQuests().remove(quest.getId());
        data.getCompletedQuests().add(quest.getId());
        data.getQuestProgress().remove(quest.getId());
        data.getQuestPhaseIndex().remove(quest.getId());
        data.getQuestStartTime().remove(quest.getId());
        if (quest.getId().equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
        }

        Map<String, Object> rewards = quest.getRewards();
        if (rewards != null) {
            if (rewards.containsKey("currency")) {
                int amount = ((Number) rewards.get("currency")).intValue();
                data.setHeldGold(data.getHeldGold() + amount);
            }
            if (rewards.containsKey("experience")) {
                int xp = ((Number) rewards.get("experience")).intValue();
                data.setXp(data.getXp() + xp);
            }
            if (rewards.containsKey("items") && player != null) {
                List<String> items = (List<String>) rewards.get("items");
                for (String itemId : items) {
                    CustomItem customItem = itemManager.getCustomItem(itemId);
                    if (customItem != null) {
                        player.getInventory().addItem(customItem.toItemStack());
                        player.sendMessage(Utils.getInstance().$("You have received: " + customItem.getName()));
                    }
                }
            }
        }

        if (player == null) {
            player = Bukkit.getPlayer(UUID.fromString(data.getUuid()));
        }

        if (player != null) {
            player.sendMessage(Utils.getInstance().$("You have completed the quest: " + quest.getName()));
            player.sendTitle(Utils.getInstance().$("Quest Completed!"), Utils.getInstance().$(quest.getName()), 10, 70, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public void chooseQuestBranch(Player player, String questId, String choice) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        Quest quest = getQuest(questId);
        if (quest == null || data == null) return;
        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
        QuestPhase phase = quest.getPhases().get(phaseIndex);
        if (phase.getBranches().containsKey(choice)) {
            String nextPhaseName = phase.getBranches().get(choice);
            int idx = getPhaseIndexByName(quest, nextPhaseName);
            data.getQuestPhaseIndex().put(questId, idx);
            data.getQuestStartTime().put(questId, System.currentTimeMillis());
            Player playerObj = Bukkit.getPlayer(data.getUuid());
            if (playerObj != null && !quest.getPhases().get(idx).getDialogueStart().isEmpty()) {
                playerObj.sendMessage(Utils.getInstance().$(quest.getPhases().get(idx).getDialogueStart()));
            }
        }
    }

    public void checkLocationObjectives(PlayerData data, Location loc) {
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
            if (phaseIndex >= quest.getPhases().size()) continue;

            QuestPhase phase = quest.getPhases().get(phaseIndex);
            for (String obj : phase.getObjectives()) {
                if (obj.startsWith("go_to_location:")) {
                    String[] parts = obj.split(":");
                    if (parts.length == 5) {
                        String worldName = parts[1];
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        World w = Bukkit.getWorld(worldName);
                        if (w != null) {
                            Location requiredLoc = new Location(w, x, y, z);
                            if (loc.getWorld().equals(w) && loc.distance(requiredLoc) < 3) {
                                updateObjectiveProgress(data, obj, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getFormattedCurrentObjective(UUID playerUUID) {
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().warning("PlayerData not found for UUID: " + playerUUID);
            return "No quest data available.";
        }

        String pinnedQuestId = data.getPinnedQuest();
        if (pinnedQuestId == null || pinnedQuestId.isEmpty()) {
            return "No pinned quest.";
        }

        Quest quest = getQuest(pinnedQuestId);
        if (quest == null) {
            DebugLogger.getInstance().warning("Quest with ID " + pinnedQuestId + " not found.");
            return "Pinned quest not found.";
        }

        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(pinnedQuestId, 0);
        QuestPhase currentPhase = quest.getPhase(phaseIndex);
        if (currentPhase == null) {
            return "No current phase.";
        }

        Map<String, Integer> progressMap = data.getQuestProgress().getOrDefault(pinnedQuestId, new HashMap<>());

        for (String objective : currentPhase.getObjectives()) {
            int required = 1;
            String[] parts = objective.split(":");
            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                if (parts.length < 3) continue;
                required = Integer.parseInt(parts[2]);
            }
            int current = progressMap.getOrDefault(objective, 0);
            if (parts[0].equals("talk_to_npc") || parts[0].equals("go_to_location")) {
                if (current < 1) {
                    return ObjectiveFormatter.formatObjective(objective, current);
                }
            } else {
                if (current < required) {
                    return ObjectiveFormatter.formatObjective(objective, current);
                }
            }
        }

        return "All objectives completed!";
    }

    public List<String> getAllFormattedCurrentObjectives(UUID playerUUID) {
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().warning("PlayerData not found for UUID: " + playerUUID);
            return Collections.singletonList("No quest data available.");
        }

        String pinnedQuestId = data.getPinnedQuest();
        if (pinnedQuestId == null || pinnedQuestId.isEmpty()) {
            return Collections.singletonList("No pinned quest.");
        }

        Quest quest = getQuest(pinnedQuestId);
        if (quest == null) {
            DebugLogger.getInstance().warning("Quest with ID " + pinnedQuestId + " not found.");
            return Collections.singletonList("Pinned quest not found.");
        }

        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(pinnedQuestId, 0);
        QuestPhase currentPhase = quest.getPhase(phaseIndex);
        if (currentPhase == null) {
            return Collections.singletonList("No current phase.");
        }

        Map<String, Integer> progressMap = data.getQuestProgress().getOrDefault(pinnedQuestId, new HashMap<>());
        List<String> formattedObjectives = new ArrayList<>();

        for (String objective : currentPhase.getObjectives()) {
            int required = 1;
            String[] parts = objective.split(":");
            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                if (parts.length < 3) continue;
                required = Integer.parseInt(parts[2]);
            }
            int current = progressMap.getOrDefault(objective, 0);
            formattedObjectives.add(ObjectiveFormatter.formatObjective(objective, current));
        }

        return formattedObjectives;
    }

    public String getCurrentObjective(UUID playerUUID) {
        List<String> formattedObjectives = getAllFormattedCurrentObjectives(playerUUID);
        if (formattedObjectives.isEmpty()) {
            return "No objectives available.";
        }

        for (String obj : formattedObjectives) {
            if (!obj.contains("/")) {
                return obj;
            } else if (obj.endsWith("/")) {
                return obj;
            } else {
                if (!obj.contains("Completed") && !obj.equals("All objectives completed!")) {
                    return obj;
                }
            }
        }

        return "All objectives completed!";
    }

    public void sendQuestProgress(CommandSender sender, Player target) {
        PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
            return;
        }

        sender.sendMessage(Utils.getInstance().$(target.getName() + "'s Quest Progress:"));
        for (String questId : data.getActiveQuests()) {
            Quest quest = getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
            if (phaseIndex >= quest.getPhases().size()) {
                sender.sendMessage(Utils.getInstance().$(quest.getName() + " - Completed all phases?"));
                continue;
            }
            sender.sendMessage(Utils.getInstance().$("Quest: " + quest.getName()));
            sender.sendMessage(Utils.getInstance().$("Phase: " + quest.getPhases().get(phaseIndex).getName()));
            sender.sendMessage(Utils.getInstance().$("Objectives:"));
            Map<String,Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());
            for (String obj : quest.getPhases().get(phaseIndex).getObjectives()) {
                int required = 1;
                String[] parts = obj.split(":");
                if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                    required = Integer.parseInt(parts[2]);
                }
                int current = progress.getOrDefault(obj,0);
                sender.sendMessage(Utils.getInstance().$(" - " + obj + " [" + current + "/" + required + "]"));
            }
        }
    }

    public void listAllQuests(Player player) {
        player.sendMessage(Utils.getInstance().$("Available Quests:"));
        for (Quest quest : getAllQuests()) {
            player.sendMessage(Utils.getInstance().$("- " + quest.getId() + ": " + quest.getName()));
        }
    }

    public void checkPlayerQuests(CommandSender sender, Player target) {
        PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());

        if (data == null) {
            sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
            return;
        }

        sender.sendMessage(Utils.getInstance().$("Active Quests:"));
        for (String questId : data.getActiveQuests()) {
            Quest quest = getQuest(questId);
            if (quest != null) {
                sender.sendMessage(Utils.getInstance().$("- " + quest.getName()));
            }
        }

        sender.sendMessage(Utils.getInstance().$("Completed Quests:"));
        for (String questId : data.getCompletedQuests()) {
            Quest quest = getQuest(questId);
            if (quest != null) {
                sender.sendMessage(Utils.getInstance().$("- " + quest.getName()));
            }
        }

        String pinnedQuestId = data.getPinnedQuest();
        if (pinnedQuestId != null) {
            Quest pinnedQuest = getQuest(pinnedQuestId);
            if (pinnedQuest != null) {
                sender.sendMessage(Utils.getInstance().$("Pinned Quest: " + pinnedQuest.getName()));
            }
        }
    }

    public void giveQuest(CommandSender sender, Player target, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            sender.sendMessage(Utils.getInstance().$("Quest not found: " + questId));
            return;
        }

        PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
            return;
        }

        if (data.getActiveQuests().contains(questId) || data.getCompletedQuests().contains(questId)) {
            sender.sendMessage(Utils.getInstance().$(target.getName() + " already has or completed this quest."));
            return;
        }

        startQuestForPlayer(data, questId);
        sender.sendMessage(Utils.getInstance().$("Quest " + quest.getName() + " given to " + target.getName()));
        target.sendMessage(Utils.getInstance().$("You have received a new quest: " + quest.getName()));
    }

    public void resetQuest(CommandSender sender, Player target, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            sender.sendMessage(Utils.getInstance().$("Quest not found: " + questId));
            return;
        }

        PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
            return;
        }

        resetQuestForPlayer(data, questId);
        sender.sendMessage(Utils.getInstance().$("Quest " + quest.getName() + " has been reset for " + target.getName()));
    }

    public Map<String, Integer> getCurrentPhaseProgress(UUID playerUUID, String questId) {
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().warning("PlayerData not found for UUID: " + playerUUID);
            return null;
        }

        if (!data.getActiveQuests().contains(questId)) {
            DebugLogger.getInstance().log("Player " + playerUUID + " does not have quest " + questId + " active.");
            return null;
        }

        Quest quest = getQuest(questId);
        if (quest == null) {
            DebugLogger.getInstance().warning("Quest with ID " + questId + " not found.");
            return null;
        }

        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
        if (phaseIndex >= quest.getPhases().size()) {
            DebugLogger.getInstance().warning("Phase index " + phaseIndex + " out of bounds for quest " + questId);
            return null;
        }

        QuestPhase currentPhase = quest.getPhases().get(phaseIndex);
        Map<String, Integer> progressMap = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

        Map<String, Integer> currentPhaseProgress = new HashMap<>();
        for (String objective : currentPhase.getObjectives()) {
            currentPhaseProgress.put(objective, progressMap.getOrDefault(objective, 0));
        }

        return currentPhaseProgress;
    }
}
