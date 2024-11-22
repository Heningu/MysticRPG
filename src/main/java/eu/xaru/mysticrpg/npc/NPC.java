package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.FollowTrait;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NPC {

    private String name;
    private Location location;
    private String behavior;
    private String skin;
    private YamlConfiguration config;
    private File configFile;


    private final QuestModule questModule;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;

    private final DialogueManager dialogueManager;

    private final JavaPlugin plugin;
    net.citizensnpcs.api.npc.NPC npcEntity;

    public NPC(String name, Location location) {
        this.name = name;
        this.location = location;
        this.behavior = "default";
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.dialogueManager = new DialogueManager(this);

        this.plugin = JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class);

        loadConfig();
        spawn();
    }

    private void loadConfig() {
        File npcsFolder = new File(plugin.getDataFolder(), "npcs");
        if (!npcsFolder.exists()) {
            npcsFolder.mkdirs();
        }
        this.configFile = new File(npcsFolder, name + ".yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.skin = config.getString("skin");
        this.behavior = config.getString("behavior", "default");
        dialogueManager.loadDialogues();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void spawn() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        npcEntity = registry.createNPC(EntityType.PLAYER, name);

        npcEntity.setName(name);

        npcEntity.addTrait(new NPCInteractTrait(this));
        npcEntity.addTrait(LookClose.class);
        npcEntity.addTrait(FollowTrait.class);

        if (skin != null && !skin.isEmpty()) {
            setSkin(skin);
        }
        npcEntity.spawn(location);
    }

    public void despawn() {
        if (npcEntity != null && npcEntity.isSpawned()) {
            npcEntity.despawn();
        }
    }

    public void update() {
        // Update any additional settings here if needed
    }

    public void interact(Player player) {
        if (dialogueManager.hasDialogues()) {
            dialogueManager.startDialogue(player);
        } else {
            String message = config.getString("interaction.message", "Hello, %player%!");
            message = message.replace("%player%", player.getName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', name + ": " + message));
            // move npc to player pos + random
            npcEntity.setMoveDestination(player.getLocation().add(Utils.getInstance().getRandomNumberInRange(-5, 5), 0, Utils.getInstance().getRandomNumberInRange(-5, 5)));
            npcEntity.getNavigator().setTarget(player, true);
            npcEntity.getNavigator().setStraightLineTarget(player.getLocation().add(Utils.getInstance().getRandomNumberInRange(-5, 5), 0, Utils.getInstance().getRandomNumberInRange(-5, 5)));
        }
    }

    public net.citizensnpcs.api.npc.NPC getNpc() {
        return npcEntity;
    }

    public void setSkin(String skinData) {
        this.skin = skinData;
        config.set("skin", skinData);
        saveConfig();

        if (npcEntity != null) {
            npcEntity.removeTrait(SkinTrait.class);
            SkinTrait skinTrait = npcEntity.getOrAddTrait(SkinTrait.class);
            if (skinData.contains(":")) {
                // Skin data is in the form of value:signature
                String[] skinParts = skinData.split(":");
                String value = skinParts[0];
                String signature = skinParts[1];
                skinTrait.setSkinPersistent(UUID.randomUUID().toString(), signature, value);
            } else {
                // Skin data is the name of a player
                skinTrait.setSkinName(skinData);
            }
            if (npcEntity.isSpawned()) {
                npcEntity.despawn();
                npcEntity.spawn(location);
            }
        }
    }

    public String getSkin() {
        return skin;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
        config.set("behavior", behavior);
        saveConfig();
        update();
    }

    public String getBehavior() {
        return behavior;
    }
}
