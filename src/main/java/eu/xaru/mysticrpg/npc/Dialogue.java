//package eu.xaru.mysticrpg.npc;
//
//import eu.xaru.mysticrpg.managers.ModuleManager;
//import eu.xaru.mysticrpg.quests.Quest;
//import eu.xaru.mysticrpg.quests.QuestManager;
//import eu.xaru.mysticrpg.quests.QuestModule;
//import eu.xaru.mysticrpg.storage.PlayerData;
//import eu.xaru.mysticrpg.storage.PlayerDataCache;
//import eu.xaru.mysticrpg.storage.SaveModule;
//import net.md_5.bungee.api.chat.*;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//
//import java.util.*;
//
//public class Dialogue {
//
//    private final String id;
//    private final int levelRequirement;
//    private final String insufficientLevelMessage;
//    private final List<String> messages;
//    private final String question;
//    private final String yesResponse;
//    private final String noResponse;
//    private final String questId;
//    private final String completionMessage;
//    private final QuestModule questModule;
//    private final QuestManager questManager;
//    private final PlayerDataCache playerDataCache;
//    private final NPC npc;
//
//    public Dialogue(YamlConfiguration config, NPC npc) {
//        this.id = config.getString("id");
//        this.npc = npc;
//        this.levelRequirement = config.getInt("levelRequirement", 1);
//        this.insufficientLevelMessage = config.getString("insufficientLevelMessage", "You are not experienced enough.");
//        this.messages = config.getStringList("messages");
//        this.question = config.getString("question");
//        this.yesResponse = config.getString("yesResponse", "Great! Here is your quest.");
//        this.noResponse = config.getString("noResponse", "Okay, let me know if you change your mind.");
//        this.questId = config.getString("questId");
//        this.completionMessage = config.getString("completionMessage", "Thank you for completing the quest!");
//
//        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
//        this.questManager = questModule.getQuestManager();
//        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
//        this.playerDataCache = saveModule.getPlayerDataCache();
//    }
//
//    public void start(Player player) {
//        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
//        if (playerData.getCompletedDialogues().contains(id)) {
//            // Dialogue already completed
//            player.sendMessage(Utils.getInstance().$(completionMessage);
//            return;
//        }
//
//        // Check if quest is completed
//        if (questId != null && playerData.getCompletedQuests().contains(questId)) {
//            // Quest is completed, show completion message
//            player.sendMessage(Utils.getInstance().$(completionMessage);
//            markCompleted(player);
//            return;
//        }
//
//        // If the quest is active but not completed, inform the player
//        if (questId != null && playerData.getActiveQuests().contains(questId)) {
//            player.sendMessage(Utils.getInstance().$("Have you completed the quest?");
//            return;
//        }
//
//        sendMessagesWithDelay(player, 0, messages.iterator());
//    }
//
//    private void sendMessagesWithDelay(Player player, int delay, Iterator<String> iterator) {
//        if (iterator.hasNext()) {
//            String message = iterator.next();
//            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class), () -> {
//                player.sendMessage(Utils.getInstance().$(message);
//                sendMessagesWithDelay(player, 40, iterator); // 2 seconds delay (40 ticks)
//            }, delay);
//        } else {
//            // All messages sent, now ask the question
//            if (question != null) {
//                sendQuestion(player);
//            } else {
//                completeDialogue(player);
//            }
//        }
//    }
//
//    private void sendQuestion(Player player) {
//        TextComponent questionComponent = new TextComponent(+ question + " ");
//        TextComponent yesComponent = new TextComponent(+ "[Yes]");
//        yesComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/npcdialogue yes " + npc.getName() + " " + id));
//        TextComponent noComponent = new TextComponent(+ "[No]");
//        noComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/npcdialogue no " + npc.getName() + " " + id));
//
//        questionComponent.addExtra(yesComponent);
//        questionComponent.addExtra(" ");
//        questionComponent.addExtra(noComponent);
//
//        player.spigot().sendMessage(Utils.getInstance().$(questionComponent);
//    }
//
//    public void handleResponse(Player player, String response) {
//        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
//        if ("yes".equalsIgnoreCase(response)) {
//            player.sendMessage(Utils.getInstance().$(yesResponse);
//            if (questId != null) {
//                Quest quest = questManager.getQuest(questId);
//                if (quest != null) {
//                    if (!data.getActiveQuests().contains(questId) && !data.getCompletedQuests().contains(questId)) {
//                        data.getActiveQuests().add(questId);
//                        data.getQuestProgress().put(questId, new HashMap<>());
//                        player.sendMessage(Utils.getInstance().$("You have received a new quest: " + quest.getName());
//                    } else {
//                        player.sendMessage(Utils.getInstance().$("You have already received this quest.");
//                    }
//                }
//            }
//            // Do not mark dialogue as completed yet, wait until quest is completed
//        } else {
//            player.sendMessage(Utils.getInstance().$(noResponse);
//            // Optionally restart the dialogue
//            start(player);
//        }
//    }
//
//    public void completeDialogue(Player player) {
//        player.sendMessage(Utils.getInstance().$(completionMessage);
//        markCompleted(player);
//    }
//
//    private void markCompleted(Player player) {
//        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
//        if (data != null) {
//            data.getCompletedDialogues().add(id);
//        }
//    }
//
//    public boolean isCompletedByPlayer(Player player) {
//        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
//        if (data != null) {
//            // Dialogue is considered completed if the quest is completed
//            if (questId != null && data.getCompletedQuests().contains(questId)) {
//                return true;
//            }
//            return data.getCompletedDialogues().contains(id);
//        }
//        return false;
//    }
//
//    public int getLevelRequirement() {
//        return levelRequirement;
//    }
//
//    public String getInsufficientLevelMessage() {
//        return insufficientLevelMessage;
//    }
//
//    public String getId() {
//        return id;
//    }
//}
