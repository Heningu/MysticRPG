package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.guis.quests.QuestHandInGUI;
import eu.xaru.mysticrpg.guis.quests.ShopGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.*;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class XaruNPC {

    private String id;
    private String name;
    private Location location;
    private DynamicConfig config;

    private final QuestModule questModule;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final DialogueManager dialogueManager;
    private final JavaPlugin plugin;

    // The Citizens NPC reference
    public net.citizensnpcs.api.npc.NPC npcEntity;

    public XaruNPC(String id, String name, Location location) {
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
        // No direct spawn call here — let Citizens handle it, or call spawnIfMissing()
    }

    private void loadConfig() {
        File npcsFolder = new File(plugin.getDataFolder(), "npcs");
        if (!npcsFolder.exists()) {
            npcsFolder.mkdirs();
        }
        this.config = DynamicConfigManager.loadConfig("npcs/" + id + ".yml");
    }

    /**
     * If Citizens hasn’t reloaded this NPC from saves, spawn it now.
     */
    public void spawnIfMissing() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        net.citizensnpcs.api.npc.NPC existing = registry.getByUniqueIdGlobal(
                UUID.nameUUIDFromBytes(id.getBytes())
        );

        // If found, just link and spawn if needed
        if (existing != null) {
            this.npcEntity = existing;
            if (!npcEntity.isSpawned() && location != null) {
                npcEntity.spawn(location);
            }
            return;
        }

        // Otherwise create a new NPC
        npcEntity = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, name);
        npcEntity.setName(name);

        // Attach your trait by class
        npcEntity.addTrait(NPCInteractTrait.class);

        // Optionally, add LookClose trait
        npcEntity.getOrAddTrait(LookClose.class).lookClose(true);

        // Actually spawn, if we have a location
        if (location != null) {
            npcEntity.spawn(location);
        }
        NPCInteractTrait trait = npcEntity.getOrAddTrait(NPCInteractTrait.class);
        trait.setXaruNPC(this);
    }

    public void despawn() {
        if (npcEntity != null && npcEntity.isSpawned()) {
            npcEntity.despawn();
        }
    }

    public void interact(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        boolean handledQuest = false;
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
            if (phaseIndex >= quest.getPhases().size()) continue;
            QuestPhase phase = quest.getPhases().get(phaseIndex);

            for (String obj : phase.getObjectives()) {
                if (obj.startsWith("talk_to_npc:")) {
                    String npcId = obj.split(":")[1];
                    if (npcId.equalsIgnoreCase(this.id)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "Oh, you got the items for me?"));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Map<Material, Integer> requiredItems = new HashMap<>();
                            requiredItems.put(Material.OAK_LOG, 16);
                            new QuestHandInGUI(player, questId, requiredItems, playerDataCache, questModule, obj).open();
                        }, 20L);
                        handledQuest = true;
                        break;
                    }
                } else if (obj.startsWith("submit_items_to_npc:")) {
                    String npcId = obj.split(":")[1];
                    if (npcId.equalsIgnoreCase(this.id)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "Oh, you got items for me?"));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Map<Material, Integer> requiredItems = new HashMap<>();
                            requiredItems.put(Material.OAK_LOG, 16);
                            new QuestHandInGUI(player, questId, requiredItems, playerDataCache, questModule, obj).open();
                        }, 20L);
                        handledQuest = true;
                        break;
                    }
                }
            }
            if (handledQuest) break;
        }
        if (handledQuest) return;

        // Dialogue approach
        if (dialogueManager.hasDialogues()) {
            dialogueManager.startDialogue(player);
            return;
        }

        // If NPC is a merchant => open shop
        String behavior = getConfig().getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            Map<String, Integer> shopItems = Map.of("iron_sword", 100, "iron_pickaxe", 200);
            new ShopGUI(player, shopItems).open();
            return;
        }

        // else greet
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

        TextComponent yesComponent = new TextComponent(ChatColor.GREEN + "[Yes]");
        yesComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/npcdialogue yes " + id + " " + dialogue.getId()
        ));

        TextComponent noComponent = new TextComponent(ChatColor.RED + "[No]");
        noComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/npcdialogue no " + id + " " + dialogue.getId()
        ));

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

    public String getId() {
        return id;
    }

    public net.citizensnpcs.api.npc.NPC getNpcEntity() {
        return npcEntity;
    }
}
