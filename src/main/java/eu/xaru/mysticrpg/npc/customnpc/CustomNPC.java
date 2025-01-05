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
 * Updated to:
 *  - Store prefix, suffix
 *  - Use one space after prefix and one space before suffix
 *  - Convert underscores -> spaces in the name at creation time
 *  - Display prefix + " " + name + " " + suffix in the greet text
 *  - Also store them in the .yml
 */
public class CustomNPC {

    private final String id;

    /**
     * Example usage:
     *   prefix = "&7["
     *   name   = "Bob the Builder"
     *   suffix = "&7]"
     * The final display => "&7[ Bob the Builder &7]"
     */
    private String prefix;
    private String name;
    private String suffix;

    private Location location;
    private String modelId;
    private final List<String> dialoguesOrder;

    private File dataFile;
    private org.bukkit.configuration.file.YamlConfiguration yml;

    // modules
    private final DialogueManager dialogueManager;
    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final JavaPlugin plugin;

    /**
     * Note: underscores -> spaces typically done in the create command:
     *   npcName = npcName.replace('_',' ');
     */
    public CustomNPC(String id, String name, Location location, String modelId) {
        this.id = id;

        // We'll assume name is already underscore->space replaced by the create command
        this.name = name;

        // Default prefix/suffix to empty string if you want no prefix/suffix
        this.prefix = "";
        this.suffix = "";

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

            // Load prefix, suffix from config if present
            this.prefix = yml.getString("prefix", this.prefix);
            this.suffix = yml.getString("suffix", this.suffix);

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

            // store prefix & suffix
            yml.set("prefix", prefix);
            yml.set("suffix", suffix);

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
     * When a player right-clicks the NPC stands.
     */
    public void interact(Player player) {
        String npcUniqueId = "NPC_" + id;

        if (!dialoguesOrder.isEmpty()) {
            String firstDialogueId = dialoguesOrder.get(0);
            Dialogue dialogue = dialogueManager.getDialogue(firstDialogueId);
            if (dialogue != null) {
                dialogueManager.startDialogue(firstDialogueId, player, npcUniqueId);
                return;
            }
        }

        // If "merchant", open shop, else greet
        String behavior = yml.getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            player.sendMessage(ChatColor.GREEN + "[Opening merchant shop!] (example)");
            return;
        }

        // Combine prefix + " " + name + " " + suffix
        // then translate color codes
        String combinedName = ChatColor.translateAlternateColorCodes('&',
                prefix + " " + name + " " + suffix);

        // Default greet
        String greetMsg = yml.getString("interaction.message", "Hello, %player%!");
        greetMsg = greetMsg.replace("%player%", player.getName());

        player.sendMessage(combinedName + ChatColor.RESET + ": " +
                ChatColor.translateAlternateColorCodes('&', greetMsg));
    }

    public void addDialogue(String dialogueId) {
        if (!dialoguesOrder.contains(dialogueId)) {
            dialoguesOrder.add(dialogueId);
        }
        saveYML();
    }

    // If you want the stands themselves to have prefix + " " + name + " " + suffix,
    // do that in your spawn logic. For example:
    // armorStand.setCustomName( ChatColor.translateAlternateColorCodes('&', prefix+" "+name+" "+suffix) );
    // or do it in EntityHandler

    // getters
    public String getId()         { return id; }
    public String getName()       { return name; }
    public String getPrefix()     { return prefix; }
    public String getSuffix()     { return suffix; }
    public Location getLocation() { return location; }
    public String getModelId()    { return modelId; }
    public List<String> getDialoguesOrder() { return dialoguesOrder; }

    // setters
    public void setName(String newName) {
        this.name = newName;
        saveYML();
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
        saveYML();
    }

    public void setSuffix(String newSuffix) {
        this.suffix = newSuffix;
        saveYML();
    }

    public void setLocation(Location newLoc) {
        this.location = newLoc;
        saveYML();
    }

    public void setModelId(String newModelId) {
        this.modelId = newModelId;
        saveYML();
    }
}
