package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class QuestGUI {

    private static final Map<UUID, Boolean> playerGUIState = new HashMap<>();
    private static final Map<UUID, Long> pinCooldowns = new HashMap<>();

    private final Player player;
    private final QuestManager questManager;
    private final PlayerDataCache playerDataCache;
    private Inventory inventory;
    private boolean showingActiveQuests;

    // Define the middle slots where quests will be displayed
    private static final int[] MIDDLE_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    public QuestGUI(Player player, QuestManager questManager, PlayerDataCache playerDataCache, boolean showingActiveQuests) {
        this.player = player;
        this.questManager = questManager;
        this.playerDataCache = playerDataCache;
        this.showingActiveQuests = showingActiveQuests;

        // Store the player's GUI state
        playerGUIState.put(player.getUniqueId(), showingActiveQuests);
    }

    public static boolean isShowingActiveQuests(Player player) {
        return playerGUIState.getOrDefault(player.getUniqueId(), true);
    }

    public void open() {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("No player data found."));
            return;
        }

        // Create an inventory of size 54 (double chest)
        inventory = Bukkit.createInventory(null, 54, "Quests");

        // Fill the inventory with placeholders, excluding quest slots
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isQuestSlot(i)) {
                inventory.setItem(i, filler);
            }
        }

        // Place header item
        ItemStack questBook = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta questBookMeta = questBook.getItemMeta();
        questBookMeta.setDisplayName(Utils.getInstance().$( "Quests"));
        questBookMeta.setLore(Arrays.asList("Here you can find all your completed and ongoing quests."));
        questBook.setItemMeta(questBookMeta);
        inventory.setItem(4, questBook); // Slot 5 (index 4)

        // Update the dye color if all quests are completed
        int completed = data.getCompletedQuests().size();
        int totalQuests = questManager.getAllQuests().size();

        Material dyeMaterial = (completed == totalQuests) ? Material.LIME_DYE : Material.GRAY_DYE;

        // Slot 3 (index 47): Dye named "Quest Progress" with lore
        ItemStack completedQuestsItem = new ItemStack(dyeMaterial);
        ItemMeta completedQuestsMeta = completedQuestsItem.getItemMeta();
        completedQuestsMeta.setDisplayName(Utils.getInstance().$( "Quest Progress"));

        completedQuestsMeta.setLore(Arrays.asList(
                 "You have completed "  + completed + " / " + totalQuests + " quests."
        ));
        completedQuestsItem.setItemMeta(completedQuestsMeta);
        inventory.setItem(47, completedQuestsItem); // Slot 3 in row 6 (index 47)

        // Slot 4 (index 48): Paper named "Useful Tips" with lore
        ItemStack usefulTipsItem = new ItemStack(Material.PAPER);
        ItemMeta usefulTipsMeta = usefulTipsItem.getItemMeta();
        usefulTipsMeta.setDisplayName(Utils.getInstance().$( "Useful Tips"));
        usefulTipsMeta.setLore(Arrays.asList(
                "Click on a quest to pin the progress to the scoreboard."
        ));
        usefulTipsItem.setItemMeta(usefulTipsMeta);
        inventory.setItem(48, usefulTipsItem); // Slot 4 in row 6 (index 48)

        // Slot 5 (index 49): Barrier named "Close" with lore
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(Utils.getInstance().$( "Close"));
        closeMeta.setLore(Arrays.asList(
                "Click to close the menu"
        ));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem); // Slot 5 in row 6 (index 49)

        // Slot 6 (index 50): Hopper Minecart named "Active Quests" with lore
        ItemStack activeQuestsItem = new ItemStack(Material.HOPPER_MINECART);
        ItemMeta activeQuestsMeta = activeQuestsItem.getItemMeta();
        activeQuestsMeta.setDisplayName(Utils.getInstance().$( "Active Quests"));
        activeQuestsMeta.setLore(Arrays.asList(
                "Click to see all your ongoing quests"
        ));
        activeQuestsItem.setItemMeta(activeQuestsMeta);
        inventory.setItem(50, activeQuestsItem); // Slot 6 in row 6 (index 50)

        // Slot 7 (index 51): Chest Minecart named "Completed Quests" with lore
        ItemStack completedQuestsMenuItem = new ItemStack(Material.CHEST_MINECART);
        ItemMeta completedQuestsMenuMeta = completedQuestsMenuItem.getItemMeta();
        completedQuestsMenuMeta.setDisplayName(Utils.getInstance().$( "Completed Quests"));
        completedQuestsMenuMeta.setLore(Arrays.asList(
                "Click to see all your completed quests"
        ));
        completedQuestsMenuItem.setItemMeta(completedQuestsMenuMeta);
        inventory.setItem(51, completedQuestsMenuItem); // Slot 7 in row 6 (index 51)

        // Place quests in the middle slots
        List<String> questsToDisplay = showingActiveQuests ? data.getActiveQuests() : data.getCompletedQuests();

        int index = 0;
        for (String questId : questsToDisplay) {
            if (index >= MIDDLE_SLOTS.length) break; // No more space

            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;

            ItemStack questItem = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = questItem.getItemMeta();
            meta.setDisplayName(Utils.getInstance().$( quest.getName()));

            List<String> lore = new ArrayList<>();
            lore.add(Utils.getInstance().$(quest.getDetails()));
            lore.add("");

            if (showingActiveQuests) {
                Map<String, Integer> objectives = quest.getObjectives();
                Map<String, Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());

                for (Map.Entry<String, Integer> entry : objectives.entrySet()) {
                    String objective = entry.getKey();
                    int required = entry.getValue();
                    int current = progress.getOrDefault(objective, 0);

                    // Cap current at required
                    current = Math.min(current, required);

                    // Format the objective
                    String formattedObjective = formatObjectiveKey(objective);

                    double percent = (double) current / required * 100;
                    if (percent > 100) percent = 100;

                    String progressBar = getProgressBar(percent);

                    lore.add(Utils.getInstance().$(formattedObjective + ": " + current + "/" + required));
                    lore.add(progressBar);
                }

                // Indicate if this quest is pinned
                if (questId.equals(data.getPinnedQuest())) {
                    lore.add("");
                    lore.add(Utils.getInstance().$("This quest is pinned to your scoreboard."));
                } else {
                    lore.add("");
                    lore.add(Utils.getInstance().$("Click to pin this quest to your scoreboard."));
                }
            } else {
                lore.add(Utils.getInstance().$("Quest Completed!"));
            }

            meta.setLore(lore);
            questItem.setItemMeta(meta);

            inventory.setItem(MIDDLE_SLOTS[index], questItem);
            index++;
        }

        player.openInventory(inventory);
    }

    private boolean isQuestSlot(int index) {
        for (int slot : MIDDLE_SLOTS) {
            if (slot == index) return true;
        }
        return false;
    }

    private String getProgressBar(double percent) {
        int totalBars = 20;
        int progressBars = (int) (percent / (100.0 / totalBars));

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < progressBars; i++) {
            sb.append("|");
        }
        sb.append(ChatColor.RED);
        for (int i = progressBars; i < totalBars; i++) {
            sb.append("|");
        }
        sb.append(" " + String.format("%.1f", percent) + "%");
        return sb.toString();
    }

    // Event handler for clicks in the GUI
    public void onInventoryClick(InventoryClickEvent event) {
        // Do not cancel the event here; it will be canceled in the main event handler after this method

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        if (clickedItem.getType() == Material.BARRIER && displayName.equalsIgnoreCase("Close")) {
            player.closeInventory();
        } else if (clickedItem.getType() == Material.CHEST_MINECART && displayName.equalsIgnoreCase("Completed Quests")) {
            // Open the GUI showing completed quests
            QuestGUI newGui = new QuestGUI(player, questManager, playerDataCache, false);
            newGui.open();
        } else if (clickedItem.getType() == Material.HOPPER_MINECART && displayName.equalsIgnoreCase("Active Quests")) {
            // Open the GUI showing active quests
            QuestGUI newGui = new QuestGUI(player, questManager, playerDataCache, true);
            newGui.open();
        } else if (clickedItem.getType() == Material.WRITTEN_BOOK) {
            // Handle clicking on a quest item to pin/unpin
            String questName = displayName;
            Quest quest = questManager.getQuestByName(questName);
            if (quest == null) return;

            // Check cooldown
            long currentTime = System.currentTimeMillis();
            long lastPinTime = pinCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (currentTime - lastPinTime < 2000) { // 2 seconds cooldown
                player.sendMessage(Utils.getInstance().$("Please wait before pinning or unpinning another quest."));
                return;
            }
            pinCooldowns.put(player.getUniqueId(), currentTime);

            if (quest.getId().equals(data.getPinnedQuest())) {
                // Unpin the quest
                data.setPinnedQuest(null);
                player.sendMessage(Utils.getInstance().$("You have unpinned the quest: " + quest.getName()));
            } else {
                // Pin the quest
                data.setPinnedQuest(quest.getId());
                player.sendMessage(Utils.getInstance().$("You have pinned the quest: " + quest.getName()));
            }

            // Refresh the GUI to update lore
            open();

            // Play a sound
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private String formatObjectiveKey(String objectiveKey) {
        if (objectiveKey.startsWith("collect_")) {
            String itemName = objectiveKey.substring("collect_".length());
            itemName = itemName.replace("_", " ");
            return "Collect " + capitalizeWords(itemName);
        } else if (objectiveKey.startsWith("kill_")) {
            String mobName = objectiveKey.substring("kill_".length());
            mobName = mobName.replace("_", " ");
            return "Kill " + capitalizeWords(mobName);
        }
        // Add other cases as needed
        return capitalizeWords(objectiveKey.replace("_", " "));
    }

    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
