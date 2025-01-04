package eu.xaru.mysticrpg.npc.customnpc.dialogues;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import eu.xaru.mysticrpg.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a single dialogue from a .yml:
 *  - id
 *  - levelRequirement
 *  - insufficientLevelMessage
 *  - messages
 *  - question
 *  - yesResponse / noResponse
 *  - questId (optional)
 */
public class Dialogue {

    private final String id;
    private final int levelRequirement;
    private final String insufficientLevelMessage;
    private final List<String> messages;
    private final String question;
    private final String yesResponse;
    private final String noResponse;
    private final String questId;

    // Modules
    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;

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

        // We assume the manager ensures messages won't be empty. Or if empty => ["Hello, %player%!"].
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

    public void start(Player player) {
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < levelRequirement) {
            player.sendMessage(Utils.getInstance().$(insufficientLevelMessage));
            return;
        }
        // Show messages (2s interval)
        Iterator<String> it = messages.iterator();
        sendMessagesWithDelay(player, 0, it);
    }

    private int getPlayerLevel(Player player) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (pd == null) return 0;
        return pd.getLevel(); // or your custom leveling system
    }

    private void sendMessagesWithDelay(Player player, int delay, Iterator<String> it) {
        if (!it.hasNext()) {
            if (question != null) {
                DialogueManager.getInstance().askQuestion(player, this);
            }
            return;
        }
        String line = it.next();
        Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(Dialogue.class),
                () -> {
                    // Replace %player% in the line if desired
                    String replaced = line.replace("%player%", player.getName());
                    player.sendMessage(Utils.getInstance().$(replaced));
                    sendMessagesWithDelay(player, 40, it);
                },
                delay
        );
    }

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
    public int getLevelRequirement() { return levelRequirement; }
    public String getInsufficientLevelMessage() { return insufficientLevelMessage; }
    public String getQuestion() { return question; }
}
