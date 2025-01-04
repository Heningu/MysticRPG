package eu.xaru.mysticrpg.npc.customnpc;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.entityhandling.EntityHandler;
import eu.xaru.mysticrpg.entityhandling.LinkedEntity;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.npc.customnpc.dialogues.Dialogue;
import eu.xaru.mysticrpg.npc.customnpc.dialogues.DialogueManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
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
 * CustomNPC holds data for a single NPC (id, name, location, modelId, dialogues, etc.).
 * The spawn() method calls EntityHandler.createLinkedEntity(...), attaching the ModelEngine model
 * if modelId != null. This logic is used for both newly created NPCs and old NPCs on startup.
 */
public class CustomNPC {

    private final String id;
    private String name;
    private Location location;
    private String modelId;  // e.g. "miner" or null

    private LinkedEntity linkedEntity;

    private final DialogueManager dialogueManager;
    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final JavaPlugin plugin;

    // YML
    private File dataFile;
    private org.bukkit.configuration.file.YamlConfiguration yml;

    // dialogues
    private final List<String> dialoguesOrder;

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

        File folder = new File(plugin.getDataFolder(), "customnpcs");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.dataFile = new File(folder, id + ".yml");
        this.yml = new org.bukkit.configuration.file.YamlConfiguration();

        this.dialoguesOrder = new ArrayList<>();
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
     * Spawns the LinkedEntity in the world (2 ArmorStands + ModelEngine if modelId != null).
     * Marked as persistent so it can also be stored in entities.yml if desired.
     */
    public void spawn() {
        despawn();

        if (location == null || location.getWorld() == null) {
            return;
        }

        // "NPC_" + id => scoreboard tag "XaruLinkedEntity_NPC_test_model" or _name
        linkedEntity = EntityHandler.getInstance().createLinkedEntity(
                "NPC_" + id,
                location,
                name,
                modelId,
                true
        );
    }

    public void despawn() {
        if (linkedEntity != null) {
            linkedEntity.despawnEntities();
            linkedEntity = null;
        }
    }

    public void interact(Player player) {
        if (!dialoguesOrder.isEmpty()) {
            String firstDialogueId = dialoguesOrder.get(0);
            Dialogue dialogue = dialogueManager.getDialogue(firstDialogueId);
            if (dialogue != null) {
                dialogue.start(player);
                return;
            }
        }

        String behavior = yml.getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            player.sendMessage(ChatColor.GREEN + "[Opening merchant shop!]");
            return;
        }

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

    public void setModelId(String newModelId) {
        this.modelId = newModelId;
        saveYML();

        if (linkedEntity != null) {
            String entityId = "NPC_" + id;
            EntityHandler.getInstance().updateEntityModel(entityId, newModelId);
        }
    }

    // Getters
    public String getId()        { return id; }
    public String getName()      { return name; }
    public Location getLocation() { return location; }
    public String getModelId()   { return modelId; }

    public void setName(String newName) {
        this.name = newName;
        if (linkedEntity != null) {
            linkedEntity.setDisplayName(newName);
        }
    }

    public void setLocation(Location newLoc) {
        this.location = newLoc;
    }
}
