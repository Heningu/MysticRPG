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
 * Loads dialogues from "plugins/MysticRPG/customnpcs/dialogues/*.yml".
 * By default, if 'messages' is empty => we set it to ["Hello, %player%!"].
 */
public class DialogueManager {

    private static DialogueManager instance;
    public static DialogueManager getInstance() {
        if (instance == null) {
            instance = new DialogueManager();
        }
        return instance;
    }

    // All dialogues loaded from disk, keyed by dialogueId
    private final Map<String, Dialogue> dialogues = new ConcurrentHashMap<>();

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

    /**
     * This just means "Are there ANY dialogues loaded from the folder at all?"
     */
    public boolean hasDialogues() {
        return !dialogues.isEmpty();
    }

    /**
     * If code calls this, we used to pick the first loaded from the entire manager.
     * But we do NOT want that if the NPC itself has an empty list. => We'll let the NPC decide.
     */
    public void startDialogue(String dialogueId, Player player) {
        Dialogue d = dialogues.get(dialogueId);
        if (d == null) {
            player.sendMessage(Utils.getInstance().$("No such dialogue: " + dialogueId));
            return;
        }
        d.start(player);
    }

    public Dialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }

    public void askQuestion(Player player, Dialogue dialogue) {
        if (dialogue.getQuestion() == null) return;

        String questionLine = dialogue.getQuestion().replace("%player%", player.getName());
        player.sendMessage(Utils.getInstance().$(questionLine));

        TextComponent yes = new TextComponent(ChatColor.GREEN + "[Yes]");
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/xarudialogue yes " + dialogue.getId()));

        TextComponent no = new TextComponent(ChatColor.RED + "[No]");
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/xarudialogue no " + dialogue.getId()));

        TextComponent space = new TextComponent(" ");
        player.spigot().sendMessage(yes, space, no);
    }

    public void handleResponse(Player player, String response, String dialogueId) {
        Dialogue d = dialogues.get(dialogueId);
        if (d == null) {
            player.sendMessage(Utils.getInstance().$("Dialogue not found: " + dialogueId));
            return;
        }
        d.handleResponse(player, response);
    }
}
