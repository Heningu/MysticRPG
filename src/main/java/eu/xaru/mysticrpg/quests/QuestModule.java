package eu.xaru.mysticrpg.quests;

import com.github.juliarn.npclib.api.event.AttackNpcEvent;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
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
import eu.xaru.mysticrpg.npc.NPCManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public class QuestModule implements IBaseModule {

    private QuestManager questManager;
    private DebugLoggerModule logger;
    private EventManager eventManager;
    private PlayerDataCache playerDataCache;
    private final JavaPlugin plugin;
    private ItemManager itemManager;
    private CustomMobModule customMobModule;

    public QuestModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized. QuestModule cannot function without it.");
        }
        playerDataCache = saveModule.getPlayerDataCache();

        // Get CustomItemModule and ItemManager
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule == null) {
            throw new IllegalStateException("CustomItemModule not initialized. QuestModule cannot function without it.");
        }
        itemManager = customItemModule.getItemManager();

        // Get CustomMobModule
        customMobModule = ModuleManager.getInstance().getModuleInstance(CustomMobModule.class);
        if (customMobModule == null) {
            throw new IllegalStateException("CustomMobModule not initialized. QuestModule cannot function without it.");
        }

        questManager = new QuestManager();
        eventManager = new EventManager(plugin);

        registerCommands();
        registerEventHandlers();

        logger.log(Level.INFO, "QuestModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "QuestModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "QuestModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "QuestModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, SaveModule.class, CustomItemModule.class, CustomMobModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void registerCommands() {

        new CommandAPICommand("debug")
                .withSubcommand(new CommandAPICommand("quest")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.getInstance().$("&cQuest &#6600ffdebug command"));
                        }))
                .withSubcommand(new CommandAPICommand("spawnnpc")
                        .executesPlayer((player, args) -> {

                            player.sendMessage(Utils.getInstance().$("&7 npc &#6600ffdebug command"));
                            Location npcLocation = player.getLocation();
                            String npcName = "Bob";
                            NPCManager npcManager = new NPCManager(MysticCore.getInstance().getPlatform());

                            npcManager
                                    .createNPC(npcLocation, npcName)
                                    .exceptionally(throwable -> {
                                        // Handle any exceptions during NPC creation
                                        plugin.getLogger().severe("Failed to create NPC: " + throwable.getMessage());
                                        return null;
                                    });
                            var eventManager = MysticCore.getInstance().getPlatform().eventManager();
                            eventManager.registerEventHandler(AttackNpcEvent.class, attackEvent -> {
                                var npc = attackEvent.npc();
                                Player player2 = attackEvent.player();
                                npc.platform().packetFactory().createAnimationPacket(EntityAnimation.TAKE_DAMAGE).schedule(player2, npc);
                                player2.sendMessage("You attacked NPC " + npc.profile().name() + "! That's not nice!");
                            });
                            eventManager.registerEventHandler(InteractNpcEvent.class, interactEvent -> {
                                var npc = interactEvent.npc();
                                Player player3 = interactEvent.player();
                                if (interactEvent.hand() == InteractNpcEvent.Hand.MAIN_HAND) {
                                    player3.sendMessage("You interacted with NPC " + npc.profile().name() + " with your main hand!");
                                } else {
                                    player3.sendMessage("You interacted with NPC " + npc.profile().name() + " with your off hand!");
                                }
                            });
                        }))
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

                            sender.sendMessage(Utils.getInstance().$("Active Quests for " + target.getName() + ":"));
                            for (String questId : data.getActiveQuests()) {
                                Quest quest = questManager.getQuest(questId);
                                if (quest != null) {
                                    sender.sendMessage(Utils.getInstance().$("- " + quest.getName()));
                                }
                            }

                            sender.sendMessage(Utils.getInstance().$("Completed Quests for " + target.getName() + ":"));
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
                .withSubcommand(new CommandAPICommand("gui")
                        .executesPlayer((player, args) -> {
                            QuestGUI questGUI = new QuestGUI(player, questManager, playerDataCache, true);
                            questGUI.open();
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

                            data.getActiveQuests().add(questId);
                            data.getQuestProgress().put(questId, new HashMap<>());

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

                            data.getActiveQuests().remove(questId);
                            data.getCompletedQuests().remove(questId);
                            data.getQuestProgress().remove(questId);

                            // Unpin the quest if it was pinned
                            if (questId.equals(data.getPinnedQuest())) {
                                data.setPinnedQuest(null);
                            }

                            sender.sendMessage(Utils.getInstance().$("Quest " + quest.getName() + " has been reset for " + target.getName()));
                        }))
                .register();
    }


    public void updateQuestProgressOnMobDeath(Player player, CustomMob customMob) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        String mobId = customMob.getId();

        for (String questId : new ArrayList<>(data.getActiveQuests())) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;

            Map<String, Integer> objectives = quest.getObjectives();
            Map<String, Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

            String objectiveKey = "kill_" + mobId;
            if (objectives.containsKey(objectiveKey)) {
                int current = progress.getOrDefault(objectiveKey, 0) + 1;
                int required = objectives.get(objectiveKey);
                int newProgress = Math.min(current, required);
                progress.put(objectiveKey, newProgress);

                data.getQuestProgress().put(questId, progress);

                if (isQuestCompleted(quest, progress)) {
                    completeQuest(player, data, quest);
                }
            }
        }
    }

    private void registerEventHandlers() {
        // Handle PlayerPickupItemEvent for item collection quests
        eventManager.registerEvent(PlayerPickupItemEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
            if (data == null) return;

            for (String questId : new ArrayList<>(data.getActiveQuests())) {
                Quest quest = questManager.getQuest(questId);
                if (quest == null) continue;

                Map<String, Integer> objectives = quest.getObjectives();
                Map<String, Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

                String itemType = event.getItem().getItemStack().getType().name().toLowerCase();
                String objectiveKey = "collect_" + itemType;
                if (objectives.containsKey(objectiveKey)) {
                    int amountPickedUp = event.getItem().getItemStack().getAmount();

                    int current = progress.getOrDefault(objectiveKey, 0);
                    int required = objectives.get(objectiveKey);

                    int newProgress = Math.min(current + amountPickedUp, required);
                    progress.put(objectiveKey, newProgress);

                    data.getQuestProgress().put(questId, progress);

                    if (isQuestCompleted(quest, progress)) {
                        completeQuest(player, data, quest);
                    }
                }
            }
        });

        // Handle EntityDamageEvent for custom mob kill quests
        eventManager.registerEvent(EntityDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

            MobManager mobManager = customMobModule.getMobManager();
            CustomMobInstance mobInstance = mobManager.findMobInstance(livingEntity);
            if (mobInstance == null) return; // Not a custom mob

            CustomMob customMob = mobInstance.getCustomMob();
            double damage = event.getFinalDamage();

            double currentHp = mobInstance.getCurrentHp() - damage;

            // Check if mob will die after this damage
            if (currentHp <= 0) {
                LivingEntity damager = null;
                if (event instanceof EntityDamageByEntityEvent edbe) {
                    if (edbe.getDamager() instanceof LivingEntity attacker) {
                        damager = attacker;
                    }
                }

                if (damager instanceof Player player) {
                    PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
                    if (data == null) return;

                    for (String questId : new ArrayList<>(data.getActiveQuests())) {
                        Quest quest = questManager.getQuest(questId);
                        if (quest == null) continue;

                        Map<String, Integer> objectives = quest.getObjectives();
                        Map<String, Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

                        String objectiveKey = "kill_" + customMob.getId();
                        if (objectives.containsKey(objectiveKey)) {
                            int currentCount = progress.getOrDefault(objectiveKey, 0) + 1;
                            int required = objectives.get(objectiveKey);
                            int newProgress = Math.min(currentCount, required);
                            progress.put(objectiveKey, newProgress);

                            data.getQuestProgress().put(questId, progress);

                            if (isQuestCompleted(quest, progress)) {
                                completeQuest(player, data, quest);
                            }
                        }
                    }
                }
            }
        });

        // Register InventoryClickEvent for QuestGUI
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null) return;

            String inventoryTitle = event.getView().getTitle();
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            // Handle clicks in the Quests menu
            if ("Quests".equals(inventoryTitle)) {
                logger.log("Player " + player.getName() + " clicked in the Quests menu.");

                // Create the QuestGUI with the correct state (active or completed quests)
                boolean showingActiveQuests = QuestGUI.isShowingActiveQuests(player);
                QuestGUI questGUI = new QuestGUI(player, questManager, playerDataCache, showingActiveQuests);

                // Handle the click within the QuestGUI
                questGUI.onInventoryClick(event);

                // Now cancel the event to prevent item movement
                event.setCancelled(true);
            }
        });

        // Register InventoryDragEvent for QuestGUI
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if ("Quests".equals(inventoryTitle)) {
                logger.log("Player is dragging items in the Quests menu.");
                event.setCancelled(true); // Prevent item movement
            }
        });
    }

    private boolean isQuestCompleted(Quest quest, Map<String, Integer> progress) {
        for (Map.Entry<String, Integer> objective : quest.getObjectives().entrySet()) {
            int currentProgress = progress.getOrDefault(objective.getKey(), 0);
            int required = objective.getValue();
            if (currentProgress < required) {
                return false;
            }
        }
        return true;
    }

    private void completeQuest(Player player, PlayerData data, Quest quest) {
        data.getActiveQuests().remove(quest.getId());
        data.getCompletedQuests().add(quest.getId());
        data.getQuestProgress().remove(quest.getId());

        // Unpin the quest if it was pinned
        if (quest.getId().equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
        }

        // Grant rewards
        Map<String, Object> rewards = quest.getRewards();
        if (rewards != null) {
            if (rewards.containsKey("currency")) {
                double amount = ((Number) rewards.get("currency")).doubleValue();
                data.setBalance(data.getBalance() + amount);
            }
            if (rewards.containsKey("experience")) {
                int xp = ((Number) rewards.get("experience")).intValue();
                data.setXp(data.getXp() + xp);
            }
            if (rewards.containsKey("items")) {
                // Get the item IDs and give the custom items to the player
                List<String> items = (List<String>) rewards.get("items");
                for (String itemId : items) {
                    CustomItem customItem = itemManager.getCustomItem(itemId);
                    if (customItem != null) {
                        player.getInventory().addItem(customItem.toItemStack());
                        player.sendMessage(Utils.getInstance().$("You have received: " + customItem.getName()));
                    } else {
                        player.sendMessage(Utils.getInstance().$("Failed to find custom item: " + itemId));
                    }
                }
            }
        }

        // Notify the player
        player.sendMessage(Utils.getInstance().$("You have completed the quest: " + quest.getName()));
        player.sendTitle(Utils.getInstance().$( "Quest Completed!"), Utils.getInstance().$(quest.getName()), 10, 70, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public void assignQuestToPlayerFromNPC(Player player, String questId, String npcId) {
        // Placeholder for future NPC integration
    }

    public void completeQuestThroughNPC(Player player, String questId, String npcId) {
        // Placeholder for future NPC integration
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}
