package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class DialogueManager {

    private final NPC npc;
    private final Map<String, Dialogue> dialogues = new LinkedHashMap<>();
    private final List<String> dialogueOrder = new ArrayList<>();

    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final File dialoguesFolder;

    public DialogueManager(NPC npc) {
        this.npc = npc;

        // Retrieve SaveModule and LevelModule from ModuleManager
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);

        // Prepare the "npcs/dialogues" folder
        File npcsFolder = new File(JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class).getDataFolder(), "npcs");
        this.dialoguesFolder = new File(npcsFolder, "dialogues");
        if (!dialoguesFolder.exists()) {
            dialoguesFolder.mkdirs();
        }
    }

    /**
     * Loads dialog configurations for this NPC based on the "dialogues.order" list in the NPC config.
     */
    public void loadDialogues() {
        // Clear any existing data
        dialogues.clear();
        dialogueOrder.clear();

        // Read the dialogue order from the NPC's config (presumably using your new system, or existing)
        List<String> order = npc.getConfig().getStringList("dialogues.order", Collections.emptyList());
        if (order == null || order.isEmpty()) {
            return; // No dialogues to load
        }
        dialogueOrder.addAll(order);

        // For each dialogueId, load the corresponding .yml from "npcs/dialogues"
        for (String dialogueId : dialogueOrder) {
            File dialogueFile = new File(dialoguesFolder, dialogueId + ".yml");
            if (!dialogueFile.exists()) {
                npc.getPlugin().getLogger().warning("Dialogue file not found: " + dialogueFile.getName());
                continue;
            }

            // 1) Construct a path in DynamicConfigManager that matches the folder structure
            String userFileName = "npcs/dialogues/" + dialogueFile.getName();
            // For instance "npcs/dialogues/example.yml"

            // 2) Load the config if not already loaded
            DynamicConfigManager.loadConfig(userFileName);

            // 3) Retrieve the DynamicConfig
            DynamicConfig dialogueConfig = DynamicConfigManager.getConfig(userFileName);
            if (dialogueConfig == null) {
                npc.getPlugin().getLogger().warning("Failed to retrieve DynamicConfig for " + userFileName);
                continue;
            }

            // 4) Create the Dialogue object with your new Dialogue constructor
            Dialogue dialogue = new Dialogue(dialogueConfig, npc);
            dialogues.put(dialogueId, dialogue);
        }
    }

    /**
     * Returns true if we actually have loaded dialogues.
     */
    public boolean hasDialogues() {
        return !dialogues.isEmpty();
    }

    /**
     * Attempts to start the next dialogue for the player, if their level is high enough,
     * and the file has not been completed yet.
     */
    public void startDialogue(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        // Which dialogue is next for this player?
        String currentDialogueId = getNextDialogueIdForPlayer(player);
        if (currentDialogueId == null) {
            String message = npc.getConfig().getString("interaction.allDialoguesCompletedMessage",
                    "I have nothing more to say.");
            player.sendMessage(Utils.getInstance().$(npc.getName() + ": " + message));
            return;
        }

        Dialogue dialogue = dialogues.get(currentDialogueId);
        if (dialogue == null) {
            player.sendMessage(Utils.getInstance().$("Dialogue not found for ID: " + currentDialogueId));
            return;
        }

        int playerLevel = data.getLevel();
        if (playerLevel < dialogue.getLevelRequirement()) {
            player.sendMessage(Utils.getInstance().$(npc.getName() + ": " + dialogue.getInsufficientLevelMessage()));
            return;
        }

        // Begin the dialogue
        dialogue.start(player);
    }

    /**
     * Finds the next dialogue ID that the player has not completed.
     */
    private String getNextDialogueIdForPlayer(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        List<String> completedDialogues = data.getCompletedDialogues();

        for (String dialogueId : dialogueOrder) {
            if (!completedDialogues.contains(dialogueId)) {
                return dialogueId;
            }
        }
        return null;
    }

    /**
     * Retrieves an already loaded Dialogue by ID.
     */
    public Dialogue getDialogueById(String dialogueId) {
        return dialogues.get(dialogueId);
    }
}
