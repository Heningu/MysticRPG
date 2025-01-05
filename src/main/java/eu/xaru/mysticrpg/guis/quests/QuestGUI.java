package eu.xaru.mysticrpg.guis.quests;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.quests.*;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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

/**
 * A Quest GUI that displays either active or completed quests.
 * Each quest item in the GUI shows:
 *  - Quest name (gold)
 *  - Quest details (gray)
 *  - For each phase:
 *     each objective => show progress + "❌" or "✅"
 *  - "Click to pin or unpin" if active
 *  - "Quest completed" if it's in completed list
 */
public class QuestGUI {

    private final PlayerDataCache playerDataCache;
    private final QuestManager questManager;
    private Player player;
    private boolean showingActiveQuests = true;

    // A pin/unpin cooldown map: <playerUUID -> lastPinActionTime>
    private static final Map<UUID, Long> pinCooldownMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_DURATION_MS = TimeUnit.SECONDS.toMillis(5);

    // Other modules used by the main menu or references
    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final StatsModule playerStat;
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
        this.playerStat = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    /**
     * Opens the main quest GUI for the player, listing either active or completed quests.
     */
    public void openQuestGUI(Player player) {
        this.player = player;

        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        // A "hint" item with instructions
        Item hint = new SimpleItem(new ItemBuilder(Material.PAPER)
                .setDisplayName("Useful Hint")
                .addLoreLines(
                        "",
                        "You can pin a quest to your sideboard to keep",
                        "track of your quest progress. To pin a quest,",
                        "simply click on one.",
                        ""
                ));

        // A "back" item to return to main menu
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(org.bukkit.ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
        ) {
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

        // Create a list of items (quests) for either active or completed
        List<Item> questItems = createQuestItems(data, showingActiveQuests);

        // The "border" item for structure
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        // "info" item at top
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

        // Toggle item to swap between active and completed quests
        Item toggleItem = new SimpleItem(new ItemBuilder(Material.HOPPER_MINECART)
                .setDisplayName(showingActiveQuests ? ChatColor.GREEN + "Active Quests" : ChatColor.BLUE + "Completed Quests")
                .addLoreLines("", ChatColor.GRAY + "Click to toggle view")) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                showingActiveQuests = !showingActiveQuests;
                openQuestGUI(clickPlayer);
            }
        };

        // Construct the PagedGui
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

        // Build the window and open
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + (showingActiveQuests ? "Active Quests" : "Completed Quests"))
                .setGui(pagedGui)
                .build();
        window.open();
    }

    /**
     * Creates the item representation of each quest in the GUI.
     * The user can click to pin/unpin a quest if it's active.
     */
    private List<Item> createQuestItems(PlayerData data, boolean showingActive) {
        List<Item> items = new ArrayList<>();
        List<String> questIds = showingActive ? data.getActiveQuests() : data.getCompletedQuests();

        for (String questId : questIds) {
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue; // Skip if no quest found

            // Build the item name/lore
            ItemStackMeta metaData = getQuestItemMeta(data, quest, showingActive);

            // Create the item
            ItemProvider provider = new ItemBuilder(Material.WRITTEN_BOOK)
                    .setDisplayName(metaData.displayName)
                    .addLoreLines(metaData.lore.toArray(new String[0]))
                    .addAllItemFlags();

            // On click => pin/unpin
            Item questItem = new SimpleItem(provider) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                    event.setCancelled(true);
                    pinOrUnpinQuest(data, quest, clickPlayer);
                    openQuestGUI(clickPlayer); // Refresh
                    clickPlayer.playSound(clickPlayer.getLocation(), Sound.UI_BUTTON_CLICK,1.0f,1.0f);
                }
            };
            items.add(questItem);
        }
        return items;
    }

    /**
     * Pins or unpins the quest, respecting a 5-second cooldown to prevent spam.
     */
    private void pinOrUnpinQuest(PlayerData data, Quest quest, Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        if (pinCooldownMap.containsKey(playerUUID)) {
            long lastTime = pinCooldownMap.get(playerUUID);
            if (currentTime - lastTime < COOLDOWN_DURATION_MS) {
                long left = (COOLDOWN_DURATION_MS - (currentTime - lastTime)) / 1000;
                player.sendMessage(Utils.getInstance().$(ChatColor.YELLOW + "Please wait " + left + " seconds before pinning/unpinning another quest."));
                return;
            }
        }

        // If not active => can't pin
        if (!quest.getId().equals(data.getPinnedQuest()) && !data.getActiveQuests().contains(quest.getId())) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You can only pin active quests."));
            return;
        }

        if (quest.getId().equals(data.getPinnedQuest())) {
            // unpin
            data.setPinnedQuest(null);
            player.sendMessage(Utils.getInstance().$("You have unpinned the quest: " + quest.getName()));
        } else {
            data.setPinnedQuest(quest.getId());
            player.sendMessage(Utils.getInstance().$("You have pinned the quest: " + quest.getName()));
        }

        // Add to cooldown
        pinCooldownMap.put(playerUUID, currentTime);
        Plugin plugin = MysticCore.getPlugin(MysticCore.class);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> pinCooldownMap.remove(playerUUID), 100L);
    }

    /**
     * Builds the final name/lore for a quest item.
     * Format:
     *  1) Gold quest name
     *  2) blank line
     *  3) gray quest details
     *  4) blank line
     *  5) for each phase => each objective with "❌" or "✅"
     *  6) blank line
     *  7) "Click to pin or unpin" or "Quest is completed."
     */
    private ItemStackMeta getQuestItemMeta(PlayerData data, Quest quest, boolean showingActive) {
        // 1) gold quest name
        String displayName = ChatColor.GOLD + quest.getName();

        List<String> lore = new ArrayList<>();
        // 2) blank line
        lore.add("");
        // 3) gray details
        lore.add(ChatColor.GRAY + quest.getDetails());
        // 4) blank line
        lore.add("");

        // Show all phases
        Map<String,Integer> progressMap = data.getQuestProgress().getOrDefault(quest.getId(), new HashMap<>());
        for (QuestPhase phase : quest.getPhases()) {
            // For each objective in the phase, check progress
            for (String obj : phase.getObjectives()) {
                // e.g. "Collect Oak Log 0/16"
                String baseStr = ObjectiveFormatter.formatObjective(obj, progressMap.getOrDefault(obj,0));

                // If that objective is done => "✅" else "❌"
                boolean objComplete = QuestObjectivesHelper.isObjectiveComplete(obj, progressMap);
                String mark = objComplete ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌";

                lore.add(ChatColor.WHITE + baseStr + mark);
            }
        }

        // blank line
        lore.add("");

        // If active => "Click to pin or unpin"
        // If completed => "Quest is completed."
        if (showingActive) {
            lore.add(ChatColor.GREEN + "Click to pin or unpin the quest.");
        } else {
            lore.add(ChatColor.GREEN + "Quest is completed.");
        }

        return new ItemStackMeta(displayName, lore);
    }

    /**
     * Helper class to store item meta data
     */
    static class ItemStackMeta {
        String displayName;
        List<String> lore;
        ItemStackMeta(String displayName, List<String> lore) {
            this.displayName = displayName;
            this.lore = lore;
        }
    }
}
