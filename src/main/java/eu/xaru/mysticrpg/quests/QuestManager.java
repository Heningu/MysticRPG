package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

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
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    String id = config.getString("id");
                    if (id == null || id.isEmpty()) {
                        DebugLogger.getInstance().severe("Quest ID is missing in file: " + file.getName());
                        continue;
                    }

                    String name = config.getString("name", "Unnamed Quest");
                    int levelRequirement = config.getInt("level_requirement", 1);
                    String typeStr = config.getString("type", "PVE").toUpperCase();
                    QuestType type = QuestType.valueOf(typeStr);

                    String details = config.getString("details", "");
                    List<String> prerequisites = config.getStringList("prerequisites");

                    List<QuestPhase> phases = new ArrayList<>();
                    ConfigurationSection phasesSection = config.getConfigurationSection("phases");
                    if (phasesSection != null) {
                        for (String phaseKey : phasesSection.getKeys(false)) {
                            ConfigurationSection phSec = phasesSection.getConfigurationSection(phaseKey);
                            String phaseName = phSec.getString("name", phaseKey);
                            String dialogueStart = phSec.getString("dialogue_start", "");
                            String dialogueEnd = phSec.getString("dialogue_end", "");
                            List<String> objectives = phSec.getStringList("objectives");
                            long timeLimit = phSec.getLong("time_limit", 0);
                            Map<String, String> branches = new HashMap<>();
                            ConfigurationSection branchSec = phSec.getConfigurationSection("branches");
                            if (branchSec != null) {
                                for (String b : branchSec.getKeys(false)) {
                                    branches.put(b, branchSec.getString(b));
                                }
                            }
                            boolean showChoices = phSec.getBoolean("show_choices", false);
                            String nextPhase = phSec.getString("next_phase");

                            QuestPhase phase = new QuestPhase(phaseName, dialogueStart, dialogueEnd, objectives, timeLimit, branches, showChoices, nextPhase);
                            phases.add(phase);
                        }
                    }

                    Map<String, Object> rewards = new HashMap<>();
                    ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        for (String key : rewardsSection.getKeys(false)) {
                            Object value = rewardsSection.get(key);
                            rewards.put(key, value);
                        }
                    }

                    String resetType = config.getString("reset_type", "none");

                    Quest quest = new Quest(id, name, levelRequirement, type, details, prerequisites, phases, rewards, resetType);
                    quests.put(id, quest);

                    DebugLogger.getInstance().log(Level.INFO, "Loaded quest: " + id);
                } catch (Exception e) {
                    DebugLogger.getInstance().severe("Failed to load quest from file " + file.getName() + ":", e);
                }
            }
        }
    }




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
                // Show user feedback for phase completion
                player.sendMessage(Utils.getInstance().$("Phase completed!"));

                if (!phase.getDialogueEnd().isEmpty()) {
                    player.sendMessage(Utils.getInstance().$(phase.getDialogueEnd()));
                }

                String nextStepMessage = "Check quest log";
                if (phase.getNextPhase() != null) {
                    nextStepMessage = "Next step: " + phase.getNextPhase();
                }

                // Show title for phase completion
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
        for (int i=0;i<quest.getPhases().size();i++) {
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
                data.setBalance(data.getBalance() + amount);
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

        // If player is null here, try retrieving again (just in case)
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
        if (quest==null||data==null) return;
        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
        QuestPhase phase = quest.getPhases().get(phaseIndex);
        if (phase.getBranches().containsKey(choice)) {
            String nextPhaseName = phase.getBranches().get(choice);
            int idx = getPhaseIndexByName(quest,nextPhaseName);
            data.getQuestPhaseIndex().put(questId, idx);
            data.getQuestStartTime().put(questId, System.currentTimeMillis());
            Player playerObj = Bukkit.getPlayer(data.getUuid());
            if (playerObj!=null && !quest.getPhases().get(idx).getDialogueStart().isEmpty()) {
                playerObj.sendMessage(Utils.getInstance().$(quest.getPhases().get(idx).getDialogueStart()));
            }
        }
    }

    public void checkLocationObjectives(PlayerData data, Location loc) {
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
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
                            Location requiredLoc = new Location(w,x,y,z);
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

        // Iterate through objectives and find the first incomplete one
        for (String objective : currentPhase.getObjectives()) {
            int required = 1; // Default requirement
            String[] parts = objective.split(":");
            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                if (parts.length < 3) continue; // Skip malformed objectives
                required = Integer.parseInt(parts[2]);
            }

            int current = progressMap.getOrDefault(objective, 0);
            if (parts[0].equals("talk_to_npc") || parts[0].equals("go_to_location")) {
                if (current < 1) { // These objectives typically require completion once
                    return ObjectiveFormatter.formatObjective(objective, current);
                }
            } else {
                if (current < required) {
                    return ObjectiveFormatter.formatObjective(objective, current);
                }
            }
        }

        // If all objectives are complete, return a completion message
        return "All objectives completed!";
    }

    /**
     * Retrieves all formatted objectives of the pinned quest's current phase.
     *
     * @param playerUUID The UUID of the player.
     * @return A list of formatted objective strings.
     */
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
            int required = 1; // Default requirement
            String[] parts = objective.split(":");
            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                if (parts.length < 3) continue; // Skip malformed objectives
                required = Integer.parseInt(parts[2]);
            }

            int current = progressMap.getOrDefault(objective, 0);
            formattedObjectives.add(ObjectiveFormatter.formatObjective(objective, current));
        }

        return formattedObjectives;
    }

    // Modify the existing getCurrentObjective to use formatted objectives
    /**
     * Retrieves the formatted current objective of the pinned quest for a specific player.
     *
     * @param playerUUID The UUID of the player.
     * @return The formatted current objective as a string, or an appropriate message if not found.
     */
    public String getCurrentObjective(UUID playerUUID) {
        List<String> formattedObjectives = getAllFormattedCurrentObjectives(playerUUID);
        if (formattedObjectives.isEmpty()) {
            return "No objectives available.";
        }

        // Option 1: Show the first incomplete objective
        for (String obj : formattedObjectives) {
            if (!obj.contains("/")) { // For objectives like "Talk to NPC" or "Go to Location"
                return obj;
            } else if (obj.endsWith("/")) {
                return obj;
            } else {
                // Assuming the first one that's not completed
                if (!obj.contains("Completed") && !obj.equals("All objectives completed!")) {
                    return obj;
                }
            }
        }

        // Option 2: If all are complete
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


    /**
     * Retrieves the current progress of the active phase for a specific player's quest.
     *
     * @param playerUUID The UUID of the player.
     * @param questId    The ID of the quest.
     * @return A map where the keys are objective identifiers and the values are the current progress counts.
     *         Returns null if the player data is not found or the quest is not active.
     */
    public Map<String, Integer> getCurrentPhaseProgress(UUID playerUUID, String questId) {
        // Retrieve the player's data
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);
        if (data == null) {
            DebugLogger.getInstance().warning("PlayerData not found for UUID: " + playerUUID);
            return null;
        }

        // Check if the quest is active for the player
        if (!data.getActiveQuests().contains(questId)) {
            DebugLogger.getInstance().log("Player " + playerUUID + " does not have quest " + questId + " active.");
            return null;
        }

        // Retrieve the quest
        Quest quest = getQuest(questId);
        if (quest == null) {
            DebugLogger.getInstance().warning("Quest with ID " + questId + " not found.");
            return null;
        }

        // Get the current phase index
        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
        if (phaseIndex >= quest.getPhases().size()) {
            DebugLogger.getInstance().warning("Phase index " + phaseIndex + " out of bounds for quest " + questId);
            return null;
        }

        // Get the current phase
        QuestPhase currentPhase = quest.getPhases().get(phaseIndex);

        // Get the progress map for the quest
        Map<String, Integer> progressMap = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

        // Prepare the current phase progress
        Map<String, Integer> currentPhaseProgress = new HashMap<>();
        for (String objective : currentPhase.getObjectives()) {
            currentPhaseProgress.put(objective, progressMap.getOrDefault(objective, 0));
        }

        return currentPhaseProgress;
    }

}
