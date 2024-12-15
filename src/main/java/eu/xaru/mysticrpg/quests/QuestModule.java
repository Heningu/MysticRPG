package eu.xaru.mysticrpg.quests;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.CustomMobModule;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.quests.QuestGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.npc.Dialogue;
import eu.xaru.mysticrpg.npc.NPC;
import eu.xaru.mysticrpg.npc.NPCManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public class QuestModule implements IBaseModule {

    private QuestManager questManager;
    private EventManager eventManager;
    private PlayerDataCache playerDataCache;
    private final JavaPlugin plugin;
    private ItemManager itemManager;
    private CustomMobModule customMobModule;
    private MobManager mobManager;
    private NPCManager npcManager;

    public QuestModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized.");
        }
        playerDataCache = PlayerDataCache.getInstance();

        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule == null) {
            throw new IllegalStateException("CustomItemModule not initialized.");
        }
        itemManager = customItemModule.getItemManager();

        customMobModule = ModuleManager.getInstance().getModuleInstance(CustomMobModule.class);
        if (customMobModule == null) {
            throw new IllegalStateException("CustomMobModule not initialized.");
        }
        mobManager = customMobModule.getMobManager();

        eventManager = new EventManager(plugin);
        npcManager = new NPCManager();

        questManager = new QuestManager(plugin, playerDataCache, itemManager);

        registerCommands();
        registerEventHandlers();

        DebugLogger.getInstance().log(Level.INFO, "QuestModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "QuestModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "QuestModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "QuestModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, CustomItemModule.class, CustomMobModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void registerCommands() {

        new CommandAPICommand("progress")
                .withPermission("mysticrpg.questadmin")
                .withArguments(new PlayerArgument("player"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    questManager.sendQuestProgress(sender, target);
                })
                .register();

        new CommandAPICommand("quests")
                .withSubcommand(new CommandAPICommand("list")
                        .withPermission("mysticrpg.questadmin")
                        .executes((sender, args) -> {
                            Player player = (Player) sender;
                            questManager.listAllQuests(player);
                        }))
                .withSubcommand(new CommandAPICommand("check")
                        .withPermission("mysticrpg.questadmin")
                        .withArguments(new PlayerArgument("player"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("player");
                            questManager.checkPlayerQuests(sender, target);
                        }))
                .withSubcommand(new CommandAPICommand("give")
                        .withPermission("mysticrpg.questadmin")
                        .withArguments(
                                new PlayerArgument("player"),
                                new StringArgument("questId").replaceSuggestions(ArgumentSuggestions.strings(info ->
                                        questManager.getAllQuests().stream().map(Quest::getId).toArray(String[]::new)))
                        )
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("player");
                            String questId = (String) args.get("questId");
                            questManager.giveQuest(sender, target, questId);
                        }))
                .withSubcommand(new CommandAPICommand("reset")
                        .withPermission("mysticrpg.questadmin")
                        .withArguments(
                                new PlayerArgument("player"),
                                new StringArgument("questId").replaceSuggestions(ArgumentSuggestions.strings(info ->
                                        questManager.getAllQuests().stream().map(Quest::getId).toArray(String[]::new)))
                        )
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("player");
                            String questId = (String) args.get("questId");
                            questManager.resetQuest(sender, target, questId);
                        }))
                .register();

        // NPC commands
        new CommandAPICommand("npc")
                .withPermission("mysticrpg.npcadmin")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("id"), new StringArgument("name"))
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("id");
                            String npcName = (String) args.get("name");
                            Location loc = player.getLocation();
                            npcManager.createNPC(loc, npcId, npcName);
                            player.sendMessage(Utils.getInstance().$("NPC " + npcName + " created with id " + npcId + " at your location."));
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("id");
                            boolean success = npcManager.deleteNPC(npcId);
                            if (success) {
                                player.sendMessage(Utils.getInstance().$("NPC with id " + npcId + " deleted."));
                            } else {
                                player.sendMessage(Utils.getInstance().$("No NPC found with id " + npcId + "."));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            Map<String, NPC> npcs = npcManager.getAllNPCs();
                            if (npcs.isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No NPCs available."));
                            } else {
                                player.sendMessage(Utils.getInstance().$("NPCs:"));
                                for (Map.Entry<String, NPC> entry : npcs.entrySet()) {
                                    player.sendMessage(Utils.getInstance().$(" - " + entry.getKey() + ": " + entry.getValue().getName()));
                                }
                            }
                        }))
                .register();

        // npcdialogue command
        new CommandAPICommand("npcdialogue")
                .withArguments(new StringArgument("response").replaceSuggestions(ArgumentSuggestions.strings("yes","no")))
                .withArguments(new StringArgument("npcId"))
                .withArguments(new StringArgument("dialogueId"))
                .executesPlayer((player, args) -> {
                    String response = (String) args.get("response");
                    String npcId = (String) args.get("npcId");
                    String dialogueId = (String) args.get("dialogueId");

                    NPC npc = npcManager.getNPC(npcId);
                    if (npc == null) {
                        player.sendMessage(Utils.getInstance().$("No NPC found with id " + npcId));
                        return;
                    }
                    Dialogue dialogue = npc.getDialogueManager().getDialogueById(dialogueId);
                    if (dialogue == null) {
                        player.sendMessage(Utils.getInstance().$("No dialogue found with id " + dialogueId));
                        return;
                    }

                    dialogue.handleResponse(player, response);
                })
                .register();
    }

    private void registerEventHandlers() {
        eventManager.registerEvent(PlayerPickupItemEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (data == null) return;
            ItemStack pickedUp = event.getItem().getItemStack();
            Material mat = pickedUp.getType();

            questManager.updateObjectiveProgress(data, "collect_item:" + mat.name() + ":", pickedUp.getAmount());
        });

        eventManager.registerEvent(EntityDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
            CustomMobInstance mobInstance = mobManager.findMobInstance(livingEntity);
            if (mobInstance == null) return;

            CustomMob customMob = mobInstance.getCustomMob();
            double damage = event.getFinalDamage();
            double currentHp = mobInstance.getCurrentHp() - damage;

            if (currentHp <= 0 && event instanceof EntityDamageByEntityEvent edbe) {
                if (edbe.getDamager() instanceof Player player) {
                    PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
                    if (data == null) return;
                    questManager.updateObjectiveProgress(data, "kill_mob:" + customMob.getId() + ":", 1);
                }
            }
        });

        eventManager.registerEvent(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (data == null) return;

            Location loc = player.getLocation();
            questManager.checkLocationObjectives(data, loc);
        });
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public void openQuestGUI(Player player) {
        // Open new Paged QuestGUI
        QuestGUI gui = new QuestGUI(player, questManager, playerDataCache);
        gui.openQuestGUI(player);

    }

    public void chooseQuestBranch(Player player, String questId, String choice) {
        questManager.chooseQuestBranch(player, questId, choice);
    }
}
