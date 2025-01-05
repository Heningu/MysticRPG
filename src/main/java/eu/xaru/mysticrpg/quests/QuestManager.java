package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * A quest manager that:
 *  - Reads each quest .yml from /plugins/MysticRPG/quests
 *  - Parses fields (id, name, details, phases, rewards, etc.) using standard Bukkit YamlConfiguration
 *  - Stores them in memory in "quests" Map
 *  - Provides the same methods for quest progression logic, checking objectives, etc.
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

        // Load all quest .yml files from /quests folder using normal Bukkit YAML
        loadQuestsFromFolder();
    }

    /**
     * Replaces the dynamic config approach. We do standard YamlConfiguration now.
     * Reads each .yml from "plugins/<pluginName>/quests" => builds Quest objects.
     */
    private void loadQuestsFromFolder() {
        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists() && !questsFolder.mkdirs()) {
            DebugLogger.getInstance().severe("Failed to create quests folder: " + questsFolder.getPath());
            return;
        }

        File[] files = questsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            DebugLogger.getInstance().log(Level.INFO, "No quest files found in " + questsFolder.getPath(), 0);
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration yml = new YamlConfiguration();
                yml.load(file);

                // read fields
                String questId = yml.getString("id", "").trim();
                if (questId.isEmpty()) {
                    DebugLogger.getInstance().severe("Quest ID is missing in file: " + file.getName());
                    continue;
                }

                String name = yml.getString("name", "Unnamed Quest");
                int levelReq = yml.getInt("level_requirement", 1);
                String typeStr = yml.getString("type", "PVE").toUpperCase(Locale.ROOT);
                QuestType type = QuestType.valueOf(typeStr);

                String details = yml.getString("details", "");
                List<String> prerequisites = yml.getStringList("prerequisites");
                if (prerequisites == null) {
                    prerequisites = new ArrayList<>();
                }

                // parse phases
                List<QuestPhase> phases = new ArrayList<>();
                if (yml.contains("phases")) {
                    // we assume 'phases' is a config section with keys like 'phase1', 'phase2', ...
                    // each is a sub-node
                    // e.g. phases.phase1.name, phases.phase1.dialogue_start, ...
                    // We'll read them in alphabetical or any order you like.
                    // Let's do a keySet approach:
                    ConfigurationSection phasesSection = yml.getConfigurationSection("phases");
                    if (phasesSection != null) {
                        for (String phaseKey : phasesSection.getKeys(false)) {
                            ConfigurationSection pSec = phasesSection.getConfigurationSection(phaseKey);
                            if (pSec == null) continue;

                            String phaseName = pSec.getString("name", phaseKey);
                            String dialogueStart = pSec.getString("dialogue_start", "");
                            String dialogueEnd = pSec.getString("dialogue_end", "");

                            List<String> objectives = pSec.getStringList("objectives");
                            if (objectives == null) objectives = new ArrayList<>();

                            long timeLimit = pSec.getLong("time_limit", 0L);

                            boolean showChoices = pSec.getBoolean("show_choices", false);
                            String nextPhase = pSec.getString("next_phase", null);

                            // branching
                            Map<String, String> branches = new HashMap<>();
                            if (pSec.isConfigurationSection("branches")) {
                                ConfigurationSection branchSec = pSec.getConfigurationSection("branches");
                                for (String bKey : branchSec.getKeys(false)) {
                                    String bVal = branchSec.getString(bKey);
                                    if (bVal != null) {
                                        branches.put(bKey, bVal);
                                    }
                                }
                            }

                            QuestPhase qp = new QuestPhase(
                                    phaseName, dialogueStart, dialogueEnd,
                                    objectives, timeLimit,
                                    branches, showChoices, nextPhase
                            );
                            phases.add(qp);
                        }
                    }
                }

                // parse rewards
                Map<String, Object> rewards = new HashMap<>();
                if (yml.contains("rewards")) {
                    ConfigurationSection rewSec = yml.getConfigurationSection("rewards");
                    if (rewSec != null) {
                        for (String key : rewSec.getKeys(false)) {
                            rewards.put(key, rewSec.get(key));
                            // e.g. "currency" -> 100, "experience" -> 250, "items" -> List
                        }
                    }
                }

                String resetType = yml.getString("reset_type", "none");

                Quest quest = new Quest(
                        questId,
                        name,
                        levelReq,
                        type,
                        details,
                        prerequisites,
                        phases,
                        rewards,
                        resetType
                );
                quests.put(questId, quest);

                DebugLogger.getInstance().log(Level.INFO, "Loaded quest: " + questId, 0);

            } catch (Exception e) {
                DebugLogger.getInstance().severe("Failed to load quest from file " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // All your original quest management methods remain below

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
                        int newVal = Math.min(current + amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if (obj.startsWith("kill_mob:") && objectivePrefix.startsWith("kill_mob:")) {
                    String requiredMob = obj.split(":")[1];
                    if (objectivePrefix.contains(requiredMob)) {
                        int current = progressMap.getOrDefault(obj,0);
                        int required = Integer.parseInt(obj.split(":")[2]);
                        int newVal = Math.min(current + amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if ((obj.startsWith("talk_to_npc:") && objectivePrefix.startsWith("talk_to_npc:"))
                        || (obj.startsWith("go_to_location:") && objectivePrefix.startsWith("go_to_location:"))) {
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

        long start = data.getQuestStartTime().getOrDefault(quest.getId(), 0L);
        if (phase.getTimeLimit() > 0 && System.currentTimeMillis() - start > phase.getTimeLimit()) {
            resetQuestForPlayer(data, quest.getId());
            Player player = Bukkit.getPlayer(UUID.fromString(data.getUuid()));
            if (player != null) {
                player.sendMessage(Utils.getInstance().$("You ran out of time to complete the phase! Quest failed."));
            }
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
        for (int i = 0; i < quest.getPhases().size(); i++) {
            if (quest.getPhases().get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return 0;
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
                Object itemObj = rewards.get("items");
                if (itemObj instanceof List) {
                    List<String> items = (List<String>) itemObj;
                    for (String itemId : items) {
                        CustomItem customItem = itemManager.getCustomItem(itemId);
                        if (customItem != null) {
                            player.getInventory().addItem(customItem.toItemStack());
                            player.sendMessage(Utils.getInstance().$("You have received: " + customItem.getName()));
                        }
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
        if (phaseIndex >= quest.getPhases().size()) return;

        QuestPhase phase = quest.getPhases().get(phaseIndex);
        if (phase.getBranches().containsKey(choice)) {
            String nextPhaseName = phase.getBranches().get(choice);
            int idx = getPhaseIndexByName(quest, nextPhaseName);
            data.getQuestPhaseIndex().put(questId, idx);
            data.getQuestStartTime().put(questId, System.currentTimeMillis());

            if (!quest.getPhases().get(idx).getDialogueStart().isEmpty()) {
                player.sendMessage(Utils.getInstance().$(quest.getPhases().get(idx).getDialogueStart()));
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

        // Return the first incomplete objective
        for (String objective : currentPhase.getObjectives()) {
            int required = 1;
            String[] parts = objective.split(":");
            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                if (parts.length < 3) continue;
                required = Integer.parseInt(parts[2]);
            }
            int current = progressMap.getOrDefault(objective, 0);
            if (current < required) {
                return ObjectiveFormatter.formatObjective(objective, current);
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

        // Return first incomplete
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

    public void sendQuestProgress(org.bukkit.command.CommandSender sender, Player target) {
        PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
            return;
        }

        sender.sendMessage(Utils.getInstance().$(target.getName() + "'s Quest Progress:"));
        for (String questId : data.getActiveQuests()) {
            Quest quest = getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
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

    public void checkPlayerQuests(org.bukkit.command.CommandSender sender, Player target) {
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

    public void giveQuest(org.bukkit.command.CommandSender sender, Player target, String questId) {
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

    public void resetQuest(org.bukkit.command.CommandSender sender, Player target, String questId) {
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
}
