package eu.xaru.mysticrpg.guis.quests;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.Quest;
import eu.xaru.mysticrpg.quests.QuestManager;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.quests.QuestPhase;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class QuestGUI {

    private final PlayerDataCache playerDataCache;
    private final QuestManager questManager;
    private Player player;
    private boolean showingActiveQuests = true;
    private static final Map<UUID, Long> pinCooldownMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_DURATION_MS = TimeUnit.SECONDS.toMillis(5); // 5 seconds


    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final PlayerStatModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;

    public QuestGUI(Player player, QuestManager questManager, PlayerDataCache playerDataCache) {
        this.player = player;
        this.questManager = questManager;
        this.playerDataCache = playerDataCache;
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openQuestGUI(Player player) {
        this.player = player;
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }


        Item hint = new SimpleItem(new ItemBuilder(Material.PAPER)
                .setDisplayName("Useful Hint")
                .addLoreLines(
                        "",
                        "You can pin a quest to your sideboard to keep",
                        "track of your quest progress. To pin a quest,",
                        "simply click on one.",
                        ""
                ));



        // Static items
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(org.bukkit.ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };



        List<Item> questItems = createQuestItems(data, showingActiveQuests);

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        Item info = new SimpleItem(new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(ChatColor.GOLD + "Quests")
                .addAllItemFlags()
                .addLoreLines(
                        "",
                        ChatColor.DARK_PURPLE + "Here you can find all your completed",
                        ChatColor.DARK_PURPLE + "and ongoing quests.",
                        ""
                )
        );

        Item toggleItem = new SimpleItem(new ItemBuilder(Material.HOPPER_MINECART)
                .setDisplayName(showingActiveQuests ? ChatColor.GREEN + "Active Quests" : ChatColor.BLUE + "Completed Quests")
                .addLoreLines("", ChatColor.GRAY + "Click to toggle view")) {
            @Override
            public void handleClick(@org.jetbrains.annotations.NotNull ClickType clickType, @org.jetbrains.annotations.NotNull Player clickPlayer, @org.jetbrains.annotations.NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                showingActiveQuests = !showingActiveQuests;
                openQuestGUI(clickPlayer);
            }
        };

        PagedGui<Item> pagedGui = PagedGui.items()
                .setStructure(
                        "# # # # I # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B T H # # # # # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', border)
                .addIngredient('I', info)
                .addIngredient('T', toggleItem)
                .addIngredient('B', back)
                .addIngredient('H', hint)
                .setContent(questItems)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + (showingActiveQuests ? "Active Quests" : "Completed Quests"))
                .setGui(pagedGui)
                .build();
        window.open();
    }

    private List<Item> createQuestItems(PlayerData data, boolean showingActive) {
        List<Item> items = new ArrayList<>();
        List<String> questIds = showingActive ? data.getActiveQuests() : data.getCompletedQuests();

        for (String questId : questIds) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;

            ItemStackMeta metaData = getQuestItemMeta(data, quest, showingActive);

            // Use addLoreLines(...) instead of setLore(...)
            ItemProvider provider = new ItemBuilder(Material.WRITTEN_BOOK)
                    .setDisplayName(metaData.displayName)
                    .addLoreLines(metaData.lore.toArray(new String[0]))
                    .addAllItemFlags();

            Item questItem = new SimpleItem(provider) {
                @Override
                public void handleClick(@org.jetbrains.annotations.NotNull ClickType clickType, @org.jetbrains.annotations.NotNull Player clickPlayer, @org.jetbrains.annotations.NotNull InventoryClickEvent event) {
                    event.setCancelled(true);
                    pinOrUnpinQuest(data, quest, clickPlayer);
                    openQuestGUI(clickPlayer); // refresh GUI
                    clickPlayer.playSound(clickPlayer.getLocation(), Sound.UI_BUTTON_CLICK,1.0f,1.0f);
                }
            };

            items.add(questItem);
        }

        return items;
    }

    private void pinOrUnpinQuest(PlayerData data, Quest quest, Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check for cooldown
        if (pinCooldownMap.containsKey(playerUUID)) {
            long lastActionTime = pinCooldownMap.get(playerUUID);
            if (currentTime - lastActionTime < COOLDOWN_DURATION_MS) {
                long timeLeft = (COOLDOWN_DURATION_MS - (currentTime - lastActionTime)) / 1000;
                player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Please wait " + timeLeft + " seconds before pinning/unpinning another quest."));
                return;
            }
        }

        // If the quest is not active and the player is trying to pin it, deny
        if (!quest.getId().equals(data.getPinnedQuest()) && !data.getActiveQuests().contains(quest.getId())) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You can only pin active quests."));
            return;
        }

        if (quest.getId().equals(data.getPinnedQuest())) {
            data.setPinnedQuest(null);
            player.sendMessage(Utils.getInstance().$("You have unpinned the quest: " + quest.getName()));
        } else {
            // Ensure only one quest is pinned at a time
            data.setPinnedQuest(quest.getId());
            player.sendMessage(Utils.getInstance().$("You have pinned the quest: " + quest.getName()));
        }

        // Add player to cooldown
        pinCooldownMap.put(playerUUID, currentTime);
        // Schedule removal of cooldown after 5 seconds

        Plugin plugin = MysticCore.getPlugin(MysticCore.class);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> pinCooldownMap.remove(playerUUID), 100L); // 100 ticks = 5 seconds
    }

    private ItemStackMeta getQuestItemMeta(PlayerData data, Quest quest, boolean showingActive) {
        String displayName = Utils.getInstance().$(quest.getName());
        List<String> lore = new ArrayList<>();
        lore.add(Utils.getInstance().$(quest.getDetails()));
        lore.add("");

        if (showingActive) {
            Map<String,Integer> progress = data.getQuestProgress().getOrDefault(quest.getId(), new HashMap<>());
            int phaseIndex = data.getQuestPhaseIndex().getOrDefault(quest.getId(),0);
            if (phaseIndex < quest.getPhases().size()) {
                QuestPhase phase = quest.getPhases().get(phaseIndex);
                for (String obj : phase.getObjectives()) {
                    lore.addAll(formatObjectiveProgress(obj, progress));
                }
                if (quest.getId().equals(data.getPinnedQuest())) {
                    lore.add("");
                    lore.add(Utils.getInstance().$("This quest is pinned to your scoreboard."));
                } else {
                    lore.add("");
                    lore.add(Utils.getInstance().$("Click to pin this quest to your scoreboard."));
                }
            }
        } else {
            lore.add(Utils.getInstance().$("Quest Completed!"));
        }

        return new ItemStackMeta(displayName, lore);
    }

    private List<String> formatObjectiveProgress(String objective, Map<String,Integer> progress) {
        List<String> lore = new ArrayList<>();
        String[] parts = objective.split(":");
        String formatted = formatObjectiveKey(objective);

        int required = 1;
        if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
            required = Integer.parseInt(parts[2]);
        }

        int current = progress.getOrDefault(objective,0);
        if (current>required) current=required;
        double percent = ((double)current/required)*100;
        String progressBar = getProgressBar(percent);

        lore.add(Utils.getInstance().$(formatted + ": " + current + "/" + required));
        lore.add(progressBar);
        return lore;
    }

    private String getProgressBar(double percent) {
        int totalBars = 20;
        int progressBars = (int) (percent/(100.0/totalBars));
        StringBuilder sb = new StringBuilder();
        sb.append(org.bukkit.ChatColor.GREEN);
        for (int i = 0; i < progressBars; i++) {
            sb.append("|");
        }
        sb.append(org.bukkit.ChatColor.RED);
        for (int i = progressBars; i < totalBars; i++) {
            sb.append("|");
        }
        sb.append(" ").append(String.format("%.1f", percent)).append("%");
        return sb.toString();
    }

    private String formatObjectiveKey(String objectiveKey) {
        String[] parts = objectiveKey.split(":");
        switch(parts[0]) {
            case "collect_item":
                return "Collect " + capitalizeWords(parts[1].toLowerCase());
            case "kill_mob":
                return "Kill " + capitalizeWords(parts[1].toLowerCase());
            case "talk_to_npc":
                return "Talk to " + capitalizeWords(parts[1].replace("_"," "));
            case "go_to_location":
                return "Go to location";
        }
        return capitalizeWords(objectiveKey.replace("_"," "));
    }

    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w: words) {
            if (w.length()>0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    static class ItemStackMeta {
        String displayName;
        List<String> lore;
        ItemStackMeta(String displayName, List<String> lore) {
            this.displayName = displayName;
            this.lore = lore;
        }
    }
}
