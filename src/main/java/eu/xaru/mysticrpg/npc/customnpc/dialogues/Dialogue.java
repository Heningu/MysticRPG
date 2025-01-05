package eu.xaru.mysticrpg.npc.customnpc.dialogues;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Dialogue {

    private final String id;
    private final int levelRequirement;
    private final String insufficientLevelMessage;
    private final List<String> messages;
    private final String question;
    private final String yesResponse;
    private final String noResponse;
    private final String questId;

    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;

    // The NPC ID for this conversation (used for yes/no clickable commands)
    private String currentNpcId;

    public Dialogue(String id,
                    int levelRequirement,
                    String insufficientLevelMessage,
                    List<String> messages,
                    String question,
                    String yesResponse,
                    String noResponse,
                    String questId) {

        this.id = id;
        this.levelRequirement = levelRequirement;
        this.insufficientLevelMessage = insufficientLevelMessage;
        this.messages = messages;
        this.question = question;
        this.yesResponse = yesResponse;
        this.noResponse = noResponse;
        this.questId = questId;

        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.questManager = questModule.getQuestManager();
        this.playerDataCache = PlayerDataCache.getInstance();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
    }

    public void start(Player player, String npcId) {
        this.currentNpcId = npcId;

        // 1) If this dialogue references a quest => skip if user is "in the middle" (active) or completed
        if (questId != null) {
            PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (pd != null) {
                // Middle of quest => quest is in activeQuests
                boolean isActive = pd.getActiveQuests().contains(questId);
                // Completed => quest is in completedQuests
                boolean isCompleted = pd.getCompletedQuests().contains(questId);

                if (isCompleted) {
                    // They fully finished the quest
                    player.sendMessage(Utils.getInstance().$("You already completed this quest, so the dialogue is skipped."));
                    DialogueManager.getInstance().finishConversation(player, npcId);
                    return;

                } else if (isActive) {
                    // They’re in the middle => skip lines
                    player.sendMessage(Utils.getInstance().$("You are already in the middle of this quest! No need to repeat dialogue."));
                    DialogueManager.getInstance().finishConversation(player, npcId);
                    return;
                }
            }
        }

        // 2) Now check the player's level if needed
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < levelRequirement) {
            player.sendMessage(Utils.getInstance().$(insufficientLevelMessage));
            DialogueManager.getInstance().finishConversation(player, npcId);
            return;
        }

        // 3) Show the lines (and possibly question at the end)
        Iterator<String> it = messages.iterator();
        sendMessagesWithDelay(player, 0, it);
    }

    private int getPlayerLevel(Player player) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        return (pd == null) ? 0 : pd.getLevel();
    }

    private void sendMessagesWithDelay(Player player, int delay, Iterator<String> it) {
        if (!it.hasNext()) {
            if (question != null) {
                // Wait for yes/no => do not remove from set
                DialogueManager.getInstance().askQuestion(player, this, currentNpcId);
            } else {
                // No question => done => remove from set
                DialogueManager.getInstance().finishConversation(player, currentNpcId);
            }
            return;
        }
        String line = it.next();
        Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(Dialogue.class),
                () -> {
                    String replaced = line.replace("%player%", player.getName());
                    player.sendMessage(Utils.getInstance().$(replaced));
                    sendMessagesWithDelay(player, 40, it);
                },
                delay
        );
    }

    /**
     * Called after user picks yes/no => we remove from set in DialogueManager
     */
    public void handleResponse(Player player, String response) {
        if ("yes".equalsIgnoreCase(response)) {
            player.sendMessage(Utils.getInstance().$(yesResponse));

            if (questId != null) {
                PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
                if (pd == null) return;
                Quest quest = questManager.getQuest(questId);
                if (quest != null) {
                    if (!pd.getActiveQuests().contains(questId)
                            && !pd.getCompletedQuests().contains(questId)) {

                        pd.getActiveQuests().add(questId);
                        pd.getQuestProgress().put(questId, new HashMap<>());
                        pd.getQuestPhaseIndex().put(questId, 0);
                        pd.getQuestStartTime().put(questId, System.currentTimeMillis());
                        player.sendMessage(Utils.getInstance().$("You have received a new quest: " + quest.getName()));
                    } else {
                        player.sendMessage(Utils.getInstance().$("You have already received this quest."));
                    }
                }
            }
        } else {
            // "no"
            player.sendMessage(Utils.getInstance().$(noResponse));
        }
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
}
