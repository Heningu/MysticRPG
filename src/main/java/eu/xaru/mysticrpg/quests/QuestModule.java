package eu.xaru.mysticrpg.quests;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.CustomMobModule;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.enums.EModulePriority;
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
    private NPCManager npcManager; // We have this from before

    public QuestModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized.");
        }
        playerDataCache = saveModule.getPlayerDataCache();

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

        questManager = new QuestManager();
        eventManager = new EventManager(plugin);

        npcManager = new NPCManager(); // Initialize NPCManager

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
                    PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
                    if (data == null) {
                        sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
                        return;
                    }

                    sender.sendMessage(Utils.getInstance().$(target.getName() + "'s Quest Progress:"));
                    for (String questId : data.getActiveQuests()) {
                        Quest quest = questManager.getQuest(questId);
                        if (quest == null) continue;
                        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
                        if (phaseIndex >= quest.getPhases().size()) {
                            sender.sendMessage(Utils.getInstance().$(quest.getName() + " - Completed all phases?"));
                            continue;
                        }
                        sender.sendMessage(Utils.getInstance().$("Quest: " + quest.getName()));
                        sender.sendMessage(Utils.getInstance().$("Phase: " + quest.getPhases().get(phaseIndex).getName()));
                        sender.sendMessage(Utils.getInstance().$("Objectives:"));
                        Map<String,Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());
                        for (String obj : quest.getPhases().get(phaseIndex).getObjectives()) {
                            int required = 1;
                            String[] parts = obj.split(":");
                            if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                                required = Integer.parseInt(parts[2]);
                            }
                            int current = progress.getOrDefault(obj,0);
                            sender.sendMessage(Utils.getInstance().$(" - " + obj + " [" + current + "/" + required + "]"));
                        }
                    }
                })
                .register();


        new CommandAPICommand("quests")
                .withSubcommand(new CommandAPICommand("list")
                        .withPermission("mysticrpg.questadmin")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.getInstance().$("Available Quests:"));
                            for (Quest quest : questManager.getAllQuests()) {
                                player.sendMessage(Utils.getInstance().$("- " + quest.getId() + ": " + quest.getName()));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("gui")
                        .withPermission("mysticrpg.questadmin")
                        .executesPlayer((player, args) -> {

                            QuestGUI quest = new QuestGUI(player, questManager, playerDataCache,true);
                            quest.open();
                        }))
                .withSubcommand(new CommandAPICommand("check")
                        .withPermission("mysticrpg.questadmin")
                        .withArguments(new PlayerArgument("player"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("player");
                            PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());

                            if (data == null) {
                                sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
                                return;
                            }

                            sender.sendMessage(Utils.getInstance().$("Active Quests:"));
                            for (String questId : data.getActiveQuests()) {
                                Quest quest = questManager.getQuest(questId);
                                if (quest != null) {
                                    sender.sendMessage(Utils.getInstance().$("- " + quest.getName()));
                                }
                            }

                            sender.sendMessage(Utils.getInstance().$("Completed Quests:"));
                            for (String questId : data.getCompletedQuests()) {
                                Quest quest = questManager.getQuest(questId);
                                if (quest != null) {
                                    sender.sendMessage(Utils.getInstance().$("- " + quest.getName()));
                                }
                            }

                            String pinnedQuestId = data.getPinnedQuest();
                            if (pinnedQuestId != null) {
                                Quest pinnedQuest = questManager.getQuest(pinnedQuestId);
                                if (pinnedQuest != null) {
                                    sender.sendMessage(Utils.getInstance().$("Pinned Quest: " + pinnedQuest.getName()));
                                }
                            }
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

                            Quest quest = questManager.getQuest(questId);
                            if (quest == null) {
                                sender.sendMessage(Utils.getInstance().$("Quest not found: " + questId));
                                return;
                            }

                            PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
                            if (data == null) {
                                sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
                                return;
                            }

                            if (data.getActiveQuests().contains(questId) || data.getCompletedQuests().contains(questId)) {
                                sender.sendMessage(Utils.getInstance().$(target.getName() + " already has or completed this quest."));
                                return;
                            }

                            startQuestForPlayer(data, questId);
                            sender.sendMessage(Utils.getInstance().$("Quest " + quest.getName() + " given to " + target.getName()));
                            target.sendMessage(Utils.getInstance().$("You have received a new quest: " + quest.getName()));
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

                            Quest quest = questManager.getQuest(questId);
                            if (quest == null) {
                                sender.sendMessage(Utils.getInstance().$("Quest not found: " + questId));
                                return;
                            }

                            PlayerData data = playerDataCache.getCachedPlayerData(target.getUniqueId());
                            if (data == null) {
                                sender.sendMessage(Utils.getInstance().$("No data found for player " + target.getName()));
                                return;
                            }

                            resetQuestForPlayer(data, questId);

                            sender.sendMessage(Utils.getInstance().$("Quest " + quest.getName() + " has been reset for " + target.getName()));
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

    private void startQuestForPlayer(PlayerData data, String questId) {
        data.getActiveQuests().add(questId);
        data.getQuestProgress().put(questId, new HashMap<>());
        data.getQuestPhaseIndex().put(questId, 0);
        data.getQuestStartTime().put(questId, System.currentTimeMillis());
    }

    private void resetQuestForPlayer(PlayerData data, String questId) {
        data.getActiveQuests().remove(questId);
        data.getCompletedQuests().remove(questId);
        data.getQuestProgress().remove(questId);
        data.getQuestPhaseIndex().remove(questId);
        data.getQuestStartTime().remove(questId);
        if (questId.equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
        }
    }

    private void registerEventHandlers() {
        eventManager.registerEvent(PlayerPickupItemEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (data == null) return;
            ItemStack pickedUp = event.getItem().getItemStack();
            Material mat = pickedUp.getType();

            updateObjectiveProgress(data, "collect_item:" + mat.name() + ":", pickedUp.getAmount());
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
                    updateObjectiveProgress(data, "kill_mob:" + customMob.getId() + ":", 1);
                }
            }
        });

        // Location-based objective: check on PlayerMoveEvent
        eventManager.registerEvent(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (data == null) return;

            Location loc = player.getLocation();
            // Check if any active quest requires going to location
            for (String questId : new ArrayList<>(data.getActiveQuests())) {
                Quest quest = questManager.getQuest(questId);
                if (quest == null) continue;
                int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
                if (phaseIndex >= quest.getPhases().size()) continue; // Completed all phases?

                QuestPhase phase = quest.getPhases().get(phaseIndex);
                for (String obj : phase.getObjectives()) {
                    if (obj.startsWith("go_to_location:")) {
                        String[] parts = obj.split(":");
                        if (parts.length == 5) {
                            String worldName = parts[1];
                            double x = Double.parseDouble(parts[2]);
                            double y = Double.parseDouble(parts[3]);
                            double z = Double.parseDouble(parts[4]);
                            World w = Bukkit.getWorld(worldName);
                            if (w != null) {
                                Location requiredLoc = new Location(w,x,y,z);
                                if (loc.getWorld().equals(w) && loc.distance(requiredLoc) < 3) {
                                    // Mark objective complete
                                    updateObjectiveProgress(data, obj, 1);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public void updateObjectiveProgress(PlayerData data, String objectivePrefix, int amount) {
        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId, 0);
            if (phaseIndex >= quest.getPhases().size()) continue;
            QuestPhase phase = quest.getPhases().get(phaseIndex);

            Map<String,Integer> progressMap = data.getQuestProgress().getOrDefault(questId, new HashMap<>());
            boolean updated = false;
            for (String obj : phase.getObjectives()) {
                if (obj.startsWith("collect_item:") && objectivePrefix.startsWith("collect_item:")) {
                    String requiredMat = obj.split(":")[1];
                    if (objectivePrefix.contains(requiredMat)) {
                        int current = progressMap.getOrDefault(obj,0);
                        int required = Integer.parseInt(obj.split(":")[2]);
                        int newVal = Math.min(current+amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if (obj.startsWith("kill_mob:") && objectivePrefix.startsWith("kill_mob:")) {
                    String requiredMob = obj.split(":")[1];
                    if (objectivePrefix.contains(requiredMob)) {
                        int current = progressMap.getOrDefault(obj,0);
                        int required = Integer.parseInt(obj.split(":")[2]);
                        int newVal = Math.min(current+amount, required);
                        progressMap.put(obj, newVal);
                        updated = true;
                    }
                } else if ((obj.startsWith("talk_to_npc:") && objectivePrefix.startsWith("talk_to_npc:")) ||
                        (obj.startsWith("go_to_location:") && objectivePrefix.startsWith("go_to_location:"))) {
                    if (obj.equals(objectivePrefix)) {
                        // Binary objective
                        progressMap.put(obj,1);
                        updated = true;
                    }
                }
            }
            if (updated) {
                data.getQuestProgress().put(questId, progressMap);
                checkPhaseCompletion(data, quest, phaseIndex);
            }
        }
    }

    private void checkPhaseCompletion(PlayerData data, Quest quest, int phaseIndex) {
        QuestPhase phase = quest.getPhases().get(phaseIndex);
        Map<String,Integer> progress = data.getQuestProgress().get(quest.getId());

        long start = data.getQuestStartTime().getOrDefault(quest.getId(),0L);
        if (phase.getTimeLimit() > 0 && System.currentTimeMillis()-start > phase.getTimeLimit()) {
            resetQuestForPlayer(data, quest.getId());
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player!=null) player.sendMessage(Utils.getInstance().$("You ran out of time to complete the phase! Quest failed."));
            return;
        }

        if (QuestObjectivesHelper.areAllObjectivesComplete(phase, progress)) {
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player!=null && !phase.getDialogueEnd().isEmpty()) {
                player.sendMessage(Utils.getInstance().$(phase.getDialogueEnd()));
            }

            // Delay the title display by 1 tick to ensure it's visible
            if (player != null) {
                String nextStepMessage = "Check quest log";
                if (phase.getNextPhase() != null) {
                    nextStepMessage = "Next step: " + phase.getNextPhase();
                }
                final String finalNextStepMessage = nextStepMessage;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendTitle(
                            Utils.getInstance().$("Phase Completed!"),
                            Utils.getInstance().$(finalNextStepMessage),
                            10, 70, 20
                    );
                });
            }

            // Handle branching or next phase
            if (phase.isShowChoices() && phase.getBranches() != null && !phase.getBranches().isEmpty()) {
                if (player!=null) {
                    player.sendMessage(Utils.getInstance().$("Please choose your path:"));
                    for (Map.Entry<String,String> e : phase.getBranches().entrySet()) {
                        player.sendMessage(Utils.getInstance().$("[Choice: "+e.getKey()+"] /questchoose "+quest.getId()+" "+e.getKey()));
                    }
                }
            } else if (phase.getNextPhase() == null && (phase.getBranches() == null || phase.getBranches().isEmpty())) {
                completeQuest(player, data, quest);
            } else if (phase.getNextPhase()!=null) {
                int nextIndex = getPhaseIndexByName(quest, phase.getNextPhase());
                data.getQuestPhaseIndex().put(quest.getId(), nextIndex);
                data.getQuestStartTime().put(quest.getId(), System.currentTimeMillis());
                if (player!=null && !quest.getPhases().get(nextIndex).getDialogueStart().isEmpty()) {
                    player.sendMessage(Utils.getInstance().$(quest.getPhases().get(nextIndex).getDialogueStart()));
                }
            }
        }
    }


    private int getPhaseIndexByName(Quest quest, String name) {
        for (int i=0;i<quest.getPhases().size();i++) {
            if (quest.getPhases().get(i).getName().equalsIgnoreCase(name)) return i;
        }
        return 0; // fallback
    }

    public void completeQuest(Player player, PlayerData data, Quest quest) {
        data.getActiveQuests().remove(quest.getId());
        data.getCompletedQuests().add(quest.getId());
        data.getQuestProgress().remove(quest.getId());
        data.getQuestPhaseIndex().remove(quest.getId());
        data.getQuestStartTime().remove(quest.getId());
        if (quest.getId().equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
        }

        Map<String, Object> rewards = quest.getRewards();
        if (rewards != null) {
            if (rewards.containsKey("currency")) {
                int amount = ((Number) rewards.get("currency")).intValue();
                data.setBalance(data.getBalance() + amount);
            }
            if (rewards.containsKey("experience")) {
                int xp = ((Number) rewards.get("experience")).intValue();
                data.setXp(data.getXp() + xp);
            }
            if (rewards.containsKey("items")) {
                List<String> items = (List<String>) rewards.get("items");
                if (player != null) {
                    for (String itemId : items) {
                        CustomItem customItem = itemManager.getCustomItem(itemId);
                        if (customItem != null) {
                            player.getInventory().addItem(customItem.toItemStack());
                            player.sendMessage(Utils.getInstance().$("You have received: " + customItem.getName()));
                        }
                    }
                }
            }
        }

        if (player!=null) {
            player.sendMessage(Utils.getInstance().$("You have completed the quest: " + quest.getName()));
            player.sendTitle(Utils.getInstance().$("Quest Completed!"), Utils.getInstance().$(quest.getName()), 10, 70, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public void openQuestGUI(Player player) {
        QuestGUI questGUI = new QuestGUI(player, questManager, playerDataCache, true);
        questGUI.open();
    }

    public void chooseQuestBranch(Player player, String questId, String choice) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        Quest quest = questManager.getQuest(questId);
        if (quest==null||data==null) return;
        int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
        QuestPhase phase = quest.getPhases().get(phaseIndex);
        if (phase.getBranches().containsKey(choice)) {
            String nextPhaseName = phase.getBranches().get(choice);
            int idx = getPhaseIndexByName(quest,nextPhaseName);
            data.getQuestPhaseIndex().put(questId, idx);
            data.getQuestStartTime().put(questId, System.currentTimeMillis());
            Player playerObj = Bukkit.getPlayer(data.getUuid());
            if (playerObj!=null && !quest.getPhases().get(idx).getDialogueStart().isEmpty()) {
                playerObj.sendMessage(Utils.getInstance().$(quest.getPhases().get(idx).getDialogueStart()));
            }
        }
    }
}
