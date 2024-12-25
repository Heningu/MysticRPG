package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.guis.quests.QuestHandInGUI;
import eu.xaru.mysticrpg.guis.quests.ShopGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.quests.QuestPhase;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPC {

    private String id;
    private String name;
    private Location location;
    private DynamicConfig config;
    private File configFile;

    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;

    private final DialogueManager dialogueManager;
    private final JavaPlugin plugin;
    net.citizensnpcs.api.npc.NPC npcEntity;

    public NPC(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.plugin = JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.questManager = questModule.getQuestManager();

        loadConfig();

        this.dialogueManager = new DialogueManager(this);
        dialogueManager.loadDialogues();
        spawn();
    }

    private void loadConfig() {
        File npcsFolder = new File(plugin.getDataFolder(), "npcs");
        if (!npcsFolder.exists()) {
            npcsFolder.mkdirs();
        }
        DynamicConfigManager.loadConfig("npcs/" + id + ".yml", "npcs/" + id + ".yml");
        this.config = DynamicConfigManager.getConfig("npcs/" + id + ".yml");
    }

    public void spawn() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        npcEntity = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, name);
        npcEntity.setName(name);
        npcEntity.addTrait(new NPCInteractTrait(this));
        npcEntity.getOrAddTrait(LookClose.class).lookClose(true); // Always look at player
        npcEntity.spawn(location);
    }

    public void despawn() {
        if (npcEntity != null && npcEntity.isSpawned()) {
            npcEntity.despawn();
        }
    }

    public void interact(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data==null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        boolean handledQuest = false;

        // Check active quests for submit_items_to_npc or talk_to_npc objectives
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
            if (phaseIndex>=quest.getPhases().size()) continue;
            QuestPhase phase = quest.getPhases().get(phaseIndex);

            for (String obj : phase.getObjectives()) {
                if (obj.startsWith("talk_to_npc:")) {
                    String npcId = obj.split(":")[1];
                    if (npcId.equalsIgnoreCase(this.id)) {
                        // Send the message to the player
                        String message = "Oh, you got the items for me?";
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

                        // Schedule the GUI to open after 1 second (20 ticks)
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Map<Material, Integer> requiredItems = new HashMap<>();
                            requiredItems.put(Material.OAK_LOG, 16);
                            new QuestHandInGUI(player, questId, requiredItems, playerDataCache, questModule, obj).open();
                        }, 20L); // 20 ticks = 1 second

                        handledQuest = true;
                        break;
                    }
                }
                else if (obj.startsWith("submit_items_to_npc:")) {
                    String npcId = obj.split(":")[1];
                    if (npcId.equalsIgnoreCase(this.id)) {
                        // Send the message to the player
                        String message ="Oh, you got items for me?";
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

                        // Schedule the GUI to open after 1 second (20 ticks)
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Map<Material, Integer> requiredItems = new HashMap<>();
                            requiredItems.put(Material.OAK_LOG, 16);
                            new QuestHandInGUI(player, questId, requiredItems, playerDataCache, questModule, obj).open();
                        }, 20L); // 20 ticks = 1 second

                        handledQuest = true;
                        break;
                    }
                }
            }
            if (handledQuest) break;
        }

        if (handledQuest) return;

        // If not handled as an active quest step, check if NPC has dialogues
        if (dialogueManager.hasDialogues()) {
            dialogueManager.startDialogue(player);
            return;
        }

        // If NPC is a merchant (from config) open ShopGUI
        String behavior = getConfig().getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            Map<String,Integer> shopItems = Map.of("iron_sword",100,"iron_pickaxe",200);
            new ShopGUI(player, shopItems).open();
            return;
        }

        // Just greet if nothing else
        String message = config.getString("interaction.message", "Hello, %player%!");
        message = message.replace("%player%", player.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', name + ": " + message));
    }

    public void sendQuestion(Player player, Dialogue dialogue) {
        if (dialogue.getQuestion() == null) {
            player.sendMessage(Utils.getInstance().$(name + ": I have nothing more to ask right now."));
            return;
        }

        player.sendMessage(Utils.getInstance().$(name + ": " + dialogue.getQuestion()));

        TextComponent yesComponent = new TextComponent(ChatColor.GREEN +"[Yes]");
        yesComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/npcdialogue yes " + id + " " + dialogue.getId()));

        TextComponent noComponent = new TextComponent(ChatColor.RED + "[No]");
        noComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/npcdialogue no " + id + " " + dialogue.getId()));

        TextComponent message = new TextComponent();
        message.addExtra(yesComponent);
        message.addExtra(" ");
        message.addExtra(noComponent);

        player.spigot().sendMessage(message);
    }

    public DynamicConfig getConfig() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }
}
