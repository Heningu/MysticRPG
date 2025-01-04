package eu.xaru.mysticrpg.npc.customnpc;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.npc.customnpc.dialogues.Dialogue;
import eu.xaru.mysticrpg.npc.customnpc.dialogues.DialogueManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple data container for a single NPC: id, name, location, modelId, dialogues, etc.
 * Actual entity spawn/remove is done by EntityHandler, not here.
 */
public class CustomNPC {

    private final String id;
    private String name;
    private Location location;
    private String modelId;  // e.g. "miner" or null

    // dialogue data
    private final List<String> dialoguesOrder;

    // YML
    private File dataFile;
    private org.bukkit.configuration.file.YamlConfiguration yml;

    // modules
    private final DialogueManager dialogueManager;
    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final JavaPlugin plugin;

    public CustomNPC(String id, String name, Location location, String modelId) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.modelId = (modelId != null && !modelId.isEmpty()) ? modelId : null;

        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);

        this.playerDataCache = PlayerDataCache.getInstance();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.questManager = questModule.getQuestManager();
        this.dialogueManager = DialogueManager.getInstance();

        this.dialoguesOrder = new ArrayList<>();

        // data file
        File folder = new File(plugin.getDataFolder(), "customnpcs");
        if (!folder.exists()) folder.mkdirs();
        this.dataFile = new File(folder, id + ".yml");
        this.yml = new org.bukkit.configuration.file.YamlConfiguration();

        loadYML();
    }

    private void loadYML() {
        if (dataFile.exists()) {
            try {
                yml.load(dataFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load NPC file: " + dataFile.getName());
                e.printStackTrace();
            }
            this.name = yml.getString("name", this.name);

            String worldName = yml.getString("location.world", "world");
            double x = yml.getDouble("location.x", 0.0);
            double y = yml.getDouble("location.y", 64.0);
            double z = yml.getDouble("location.z", 0.0);

            org.bukkit.World w = Bukkit.getWorld(worldName);
            if (w == null && !Bukkit.getWorlds().isEmpty()) {
                w = Bukkit.getWorlds().get(0);
            }
            this.location = new Location(w, x, y, z);

            String savedModelId = yml.getString("modelId", "");
            if (!savedModelId.isEmpty()) {
                this.modelId = savedModelId;
            }

            List<String> loaded = yml.getStringList("dialogues");
            if (!loaded.isEmpty()) {
                dialoguesOrder.addAll(loaded);
            }
        }
    }

    public void saveYML() {
        try {
            yml.set("id", id);
            yml.set("name", name);
            if (location != null && location.getWorld() != null) {
                yml.set("location.world", location.getWorld().getName());
                yml.set("location.x", location.getX());
                yml.set("location.y", location.getY());
                yml.set("location.z", location.getZ());
            }
            yml.set("modelId", modelId);
            yml.set("dialogues", dialoguesOrder);
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save NPC file: " + dataFile.getName());
            e.printStackTrace();
        }
    }

    public void save() {
        saveYML();
    }

    /**
     * Called when a player right-clicks (interacts) one of the NPC's stands (via scoreboard tag).
     */
    public void interact(Player player) {
        // If the NPC has dialogues
        if (!dialoguesOrder.isEmpty()) {
            String firstDialogueId = dialoguesOrder.get(0);
            Dialogue dialogue = dialogueManager.getDialogue(firstDialogueId);
            if (dialogue != null) {
                dialogue.start(player);
                return;
            }
        }

        // If "merchant", open a shop or do something else
        String behavior = yml.getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            player.sendMessage(ChatColor.GREEN + "[Opening merchant shop!]");
            return;
        }

        // Default greet
        String greetMsg = yml.getString("interaction.message", "Hello, %player%!");
        greetMsg = greetMsg.replace("%player%", player.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                name + ": " + greetMsg));
    }

    public void addDialogue(String dialogueId) {
        if (!dialoguesOrder.contains(dialogueId)) {
            dialoguesOrder.add(dialogueId);
        }
        saveYML();
    }

    // getters
    public String getId()        { return id; }
    public String getName()      { return name; }
    public Location getLocation() { return location; }
    public String getModelId()   { return modelId; }

    public List<String> getDialoguesOrder() {
        return dialoguesOrder;
    }

    // setters
    public void setName(String newName) {
        this.name = newName;
        saveYML(); // persist change
    }

    public void setLocation(Location newLoc) {
        this.location = newLoc;
        saveYML(); // persist change
    }

    public void setModelId(String newModelId) {
        this.modelId = newModelId;
        saveYML();
    }
}
