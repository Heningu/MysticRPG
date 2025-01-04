/*
package eu.xaru.mysticrpg.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.guis.quests.QuestHandInGUI;
import eu.xaru.mysticrpg.guis.quests.ShopGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.quests.QuestPhase;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;  // IMPORTANT: import the SkinTrait
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
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
*/
/*
    private final DialogueManager dialogueManager;
*//*

    private final JavaPlugin plugin;

    // The Citizens NPC reference
    public net.citizensnpcs.api.npc.NPC npcEntity;

    // Whether to use ModelEngine + which model
    private boolean modeled;
    private String modelId;

    public XaruNPC(String id, String name, Location location,
                   boolean modeled, String modelId) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.modeled = modeled;
        this.modelId = modelId;

        this.plugin = JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.questManager = questModule.getQuestManager();

        loadConfig();

*/
/*        this.dialogueManager = new DialogueManager(this);
        dialogueManager.loadDialogues();*//*

        // We don't spawn right here; spawnIfMissing() is called later
    }

    // Overload for normal creation
    public XaruNPC(String id, String name, Location location) {
        this(id, name, location, false, null);
    }

    private void loadConfig() {
        File npcsFolder = new File(plugin.getDataFolder(), "npcs");
        if (!npcsFolder.exists()) {
            npcsFolder.mkdirs();
        }
        this.config = DynamicConfigManager.loadConfig("npcs/" + id + ".yml");
    }

    */
/**
     * If Citizens hasn’t reloaded this NPC from its saves, create/spawn it now.
     * We forcibly remove any default skin references so we don't see a random player skin.
     *//*

    public void spawnIfMissing() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        net.citizensnpcs.api.npc.NPC existing = registry.getByUniqueIdGlobal(
                UUID.nameUUIDFromBytes(id.getBytes())
        );

        if (existing != null) {
            // Already in Citizens registry
            this.npcEntity = existing;

            // (Optionally despawn first to ensure we apply the blank skin properly)
            if (npcEntity.isSpawned()) {
                npcEntity.despawn();
            }

            // Remove the default SkinTrait if present
            if (npcEntity.hasTrait(SkinTrait.class)) {
                npcEntity.removeTrait(SkinTrait.class);
            }

            // Clear all references to a real skin
            npcEntity.data().setPersistent("player-skin-use-latest", false);
            npcEntity.data().setPersistent("player-skin-name", "none");
            npcEntity.data().remove("cached-skin-uuid");
            npcEntity.data().remove("cached-skin-uuid-name");
            npcEntity.setProtected(true);

            // Re-add your standard traits
            npcEntity.addTrait(NPCInteractTrait.class);
            npcEntity.getOrAddTrait(LookClose.class).lookClose(true);

            // Now spawn if needed
            if (!npcEntity.isSpawned() && location != null) {
                npcEntity.spawn(location);
            }
        } else {
            // Otherwise create a brand-new NPC as a PLAYER
            npcEntity = registry.createNPC(EntityType.PLAYER, name);
            npcEntity.setName(name);

            // Remove or disable any default Citizen skins
            if (npcEntity.hasTrait(SkinTrait.class)) {
                npcEntity.removeTrait(SkinTrait.class);
            }
            npcEntity.data().setPersistent("player-skin-use-latest", false);
            npcEntity.data().setPersistent("player-skin-name", "none");
            npcEntity.data().remove("cached-skin-uuid");
            npcEntity.data().remove("cached-skin-uuid-name");
            // Add your traits
            npcEntity.addTrait(NPCInteractTrait.class);
            npcEntity.getOrAddTrait(LookClose.class).lookClose(true);

            // Spawn
            if (location != null) {
                npcEntity.spawn(location);
            }

            // Link trait => XaruNPC
            NPCInteractTrait trait = npcEntity.getOrAddTrait(NPCInteractTrait.class);
            trait.setXaruNPC(this);
        }

        // Attach ModelEngine if needed
        if (modeled && modelId != null && !modelId.isEmpty()) {
            try {
                org.bukkit.entity.Entity bukkitEnt = npcEntity.getEntity();
                if (bukkitEnt == null) return;

                ModeledEntity me = ModelEngineAPI.createModeledEntity(bukkitEnt);
                if (me == null) return;

                ActiveModel am = ModelEngineAPI.createActiveModel(modelId);
                if (am == null) return;

                me.addModel(am, true);

                // Hide the underlying player
                me.setBaseEntityVisible(false);

                // e.g. am.setState("idle", true);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to attach ModelEngine model " + modelId + " to NPC " + id);
                e.printStackTrace();
            }
        }
    }

    */
/**
     * Re-apply the model after a server restart if Citizens re-spawns the NPC.
     *//*

    public void reapplyModelIfNeeded() {
        if (!modeled || modelId == null || modelId.isEmpty()) {
            return;
        }
        if (npcEntity == null || !npcEntity.isSpawned()) {
            return;
        }

        org.bukkit.entity.Entity bukkitEnt = npcEntity.getEntity();
        if (bukkitEnt == null) return;

        ModeledEntity me = ModelEngineAPI.getModeledEntity(bukkitEnt);
        if (me == null) {
            me = ModelEngineAPI.createModeledEntity(bukkitEnt);
            if (me == null) {
                plugin.getLogger().warning("Failed to (re)create ModeledEntity for NPC " + id);
                return;
            }
        }

        Optional<ActiveModel> maybeExisting = me.getModel(modelId);
        if (maybeExisting.isEmpty()) {
            ActiveModel newModel = ModelEngineAPI.createActiveModel(modelId);
            if (newModel == null) {
                plugin.getLogger().warning("Invalid modelId '" + modelId + "' for NPC " + id);
                return;
            }
            me.addModel(newModel, true);
        }

        me.setBaseEntityVisible(false);
        // maybeExisting.ifPresent(am -> am.setState("idle", true));
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

*/
/*        // Dialogue approach
        if (dialogueManager.hasDialogues()) {
            dialogueManager.startDialogue(player);
            return;
        }*//*


        // If NPC is a merchant => open shop
        String behavior = getConfig().getString("behavior", "default");
        if ("merchant".equalsIgnoreCase(behavior)) {
            Map<String, Integer> shopItems = Map.of("iron_sword", 100, "iron_pickaxe", 200);
            new ShopGUI(player, shopItems).open();
            return;
        }

        // else just greet
        String message = config.getString("interaction.message", "Hello, %player%!");
        message = message.replace("%player%", player.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', name + ": " + message));
    }

    */
/*public void sendQuestion(Player player, Dialogue dialogue) {
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
    }*//*


    public DynamicConfig getConfig() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

*/
/*    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }*//*


    public String getId() {
        return id;
    }

    public net.citizensnpcs.api.npc.NPC getNpcEntity() {
        return npcEntity;
    }

    public boolean isModeled() {
        return modeled;
    }

    public String getModelId() {
        return modelId;
    }
}
*/
