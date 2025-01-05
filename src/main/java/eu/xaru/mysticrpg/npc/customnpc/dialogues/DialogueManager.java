package eu.xaru.mysticrpg.npc.customnpc.dialogues;

import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads dialogues from "plugins/MysticRPG/customnpcs/dialogues/*.yml" and
 * blocks spam by storing "playerUUID_npcId" in a set.
 * If a conversation is ongoing, we do not show the lines again.
 */
public class DialogueManager {

    private static DialogueManager instance;
    public static DialogueManager getInstance() {
        if (instance == null) {
            instance = new DialogueManager();
        }
        return instance;
    }

    private final Map<String, Dialogue> dialogues = new ConcurrentHashMap<>();

    /**
     * A set of "ongoing conversation" => "playerUUID_npcId".
     */
    private final Set<String> ongoingConversations = ConcurrentHashMap.newKeySet();

    private String conversationKey(Player player, String npcId) {
        return player.getUniqueId() + "_" + npcId;
    }

    public void loadAllDialogues(File dialoguesFolder) {
        dialogues.clear();
        if (!dialoguesFolder.exists()) {
            dialoguesFolder.mkdirs();
        }

        File[] files = dialoguesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String dialogueId = file.getName().replace(".yml", "");
            YamlConfiguration yml = new YamlConfiguration();
            try {
                yml.load(file);

                String id = yml.getString("id", dialogueId);
                int lvlReq = yml.getInt("levelRequirement", 1);
                String insufficient = yml.getString("insufficientLevelMessage", "You are not experienced enough.");

                List<String> msgs = yml.getStringList("messages");
                if (msgs == null || msgs.isEmpty()) {
                    msgs = Collections.singletonList("Hello, %player%!");
                }

                String question = yml.getString("question", null);
                String yesResp = yml.getString("yesResponse", "Great!");
                String noResp = yml.getString("noResponse", "Maybe next time.");
                String questId = yml.getString("questId", null);

                Dialogue d = new Dialogue(id, lvlReq, insufficient, msgs, question, yesResp, noResp, questId);
                dialogues.put(dialogueId, d);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasDialogues() {
        return !dialogues.isEmpty();
    }

    public Dialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }

    public String[] getAllDialogueIds() {
        return dialogues.keySet().toArray(new String[0]);
    }

    /**
     * Called by NPC. If no conversation is active, we add to set & start the dialogue.
     */
    public void startDialogue(String dialogueId, Player player, String npcId) {
        String key = conversationKey(player, npcId);
        if (ongoingConversations.contains(key)) {
            player.sendMessage(Utils.getInstance().$("You are already talking to this NPC!"));
            return;
        }

        Dialogue d = dialogues.get(dialogueId);
        if (d == null) {
            player.sendMessage(Utils.getInstance().$("No such dialogue: " + dialogueId));
            return;
        }
        // Mark them in conversation
        ongoingConversations.add(key);

        // Start with npcId
        d.start(player, npcId);
    }

    /**
     * If the dialogue finishes (no question) or the user picks yes/no => remove from set.
     */
    public void finishConversation(Player player, String npcId) {
        if (npcId == null) {
            // fallback => remove all for that player's UUID
            ongoingConversations.removeIf(s -> s.startsWith(player.getUniqueId().toString()));
        } else {
            String key = conversationKey(player, npcId);
            ongoingConversations.remove(key);
        }
    }

    /**
     * askQuestion(..., npcId) => embed <npcId> in the clickable commands
     */
    public void askQuestion(Player player, Dialogue dialogue, String npcId) {
        if (dialogue.getQuestion() == null) return;

        String questionLine = dialogue.getQuestion().replace("%player%", player.getName());
        player.sendMessage(Utils.getInstance().$(questionLine));

        TextComponent yes = new TextComponent(ChatColor.GREEN + "[Yes]");
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/xarudialogue yes " + dialogue.getId() + " " + npcId));

        TextComponent no = new TextComponent(ChatColor.RED + "[No]");
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/xarudialogue no " + dialogue.getId() + " " + npcId));

        TextComponent space = new TextComponent(" ");
        player.spigot().sendMessage(yes, space, no);
    }

    /**
     * The final step: /xarudialogue yes/no <dialogueId> <npcId>
     */
    public void handleResponse(Player player, String response, String dialogueId, String npcId) {
        Dialogue d = dialogues.get(dialogueId);
        if (d == null) {
            player.sendMessage(Utils.getInstance().$("Dialogue not found: " + dialogueId));
            return;
        }
        d.handleResponse(player, response);

        // done => remove from set
        finishConversation(player, npcId);
    }
}
