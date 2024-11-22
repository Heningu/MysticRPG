package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.configuration.file.YamlConfiguration;
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
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);

        // Initialize dialogues folder
        File npcsFolder = new File(JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class).getDataFolder(), "npcs");
        this.dialoguesFolder = new File(npcsFolder, "dialogues");
        if (!dialoguesFolder.exists()) {
            dialoguesFolder.mkdirs();
        }
    }

    public void loadDialogues() {
        dialogues.clear();
        dialogueOrder.clear();

        // Load dialogue order from NPC's YAML file
        List<String> order = npc.getConfig().getStringList("dialogues.order");
        if (order == null || order.isEmpty()) {
            return;
        }
        dialogueOrder.addAll(order);

        // Load dialogues from files
        for (String dialogueId : dialogueOrder) {
            File dialogueFile = new File(dialoguesFolder, dialogueId + ".yml");
            if (!dialogueFile.exists()) {
                npc.getPlugin().getLogger().warning("Dialogue file not found: " + dialogueFile.getName());
                continue;
            }
            YamlConfiguration dialogueConfig = YamlConfiguration.loadConfiguration(dialogueFile);
            Dialogue dialogue = new Dialogue(dialogueConfig, npc);
            dialogues.put(dialogueId, dialogue);
        }
    }

    public boolean hasDialogues() {
        return !dialogues.isEmpty();
    }

    public void startDialogue(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        String currentDialogueId = getNextDialogueIdForPlayer(player);
        if (currentDialogueId == null) {
            // All dialogues exhausted
            String message = npc.getConfig().getString("interaction.allDialoguesCompletedMessage", "I have nothing more to say.");
            player.sendMessage(Utils.getInstance().$(npc.getName() + ": " + message));
            return;
        }

        Dialogue dialogue = dialogues.get(currentDialogueId);
        if (dialogue == null) {
            player.sendMessage(Utils.getInstance().$("Dialogue not found."));
            return;
        }

        // Check level requirement
        int playerLevel = data.getLevel();
        if (playerLevel < dialogue.getLevelRequirement()) {
            player.sendMessage(Utils.getInstance().$(npc.getName() + ": " + dialogue.getInsufficientLevelMessage()));
            return;
        }

        dialogue.start(player);
    }

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

    public Dialogue getDialogueById(String dialogueId) {
        return dialogues.get(dialogueId);
    }
}
