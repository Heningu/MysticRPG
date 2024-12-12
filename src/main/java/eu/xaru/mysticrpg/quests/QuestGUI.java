package eu.xaru.mysticrpg.quests;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
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

        inventory = player.getServer().createInventory(null, 54, "Quests");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isQuestSlot(i)) {
                inventory.setItem(i, filler);
            }
        }

        ItemStack questBook = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta questBookMeta = questBook.getItemMeta();
        questBookMeta.setDisplayName(Utils.getInstance().$("Quests"));
        questBookMeta.setLore(Arrays.asList("Here you can find all your completed and ongoing quests."));
        questBook.setItemMeta(questBookMeta);
        inventory.setItem(4, questBook);

        int completed = data.getCompletedQuests().size();
        int totalQuests = questManager.getAllQuests().size();
        Material dyeMaterial = (completed == totalQuests) ? Material.LIME_DYE : Material.GRAY_DYE;

        ItemStack completedQuestsItem = new ItemStack(dyeMaterial);
        ItemMeta completedQuestsMeta = completedQuestsItem.getItemMeta();
        completedQuestsMeta.setDisplayName(Utils.getInstance().$("Quest Progress"));
        completedQuestsMeta.setLore(Arrays.asList("You have completed " + completed + " / " + totalQuests + " quests."));
        completedQuestsItem.setItemMeta(completedQuestsMeta);
        inventory.setItem(47, completedQuestsItem);

        ItemStack usefulTipsItem = new ItemStack(Material.PAPER);
        ItemMeta usefulTipsMeta = usefulTipsItem.getItemMeta();
        usefulTipsMeta.setDisplayName(Utils.getInstance().$("Useful Tips"));
        usefulTipsMeta.setLore(Arrays.asList("Click on a quest to pin the progress to the scoreboard."));
        usefulTipsItem.setItemMeta(usefulTipsMeta);
        inventory.setItem(48, usefulTipsItem);

        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(Utils.getInstance().$("Close"));
        closeMeta.setLore(Arrays.asList("Click to close the menu"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem);

        ItemStack activeQuestsItem = new ItemStack(Material.HOPPER_MINECART);
        ItemMeta activeQuestsMeta = activeQuestsItem.getItemMeta();
        activeQuestsMeta.setDisplayName(Utils.getInstance().$("Active Quests"));
        activeQuestsMeta.setLore(Arrays.asList("Click to see all your ongoing quests"));
        activeQuestsItem.setItemMeta(activeQuestsMeta);
        inventory.setItem(50, activeQuestsItem);

        ItemStack completedQuestsMenuItem = new ItemStack(Material.CHEST_MINECART);
        ItemMeta completedQuestsMenuMeta = completedQuestsMenuItem.getItemMeta();
        completedQuestsMenuMeta.setDisplayName(Utils.getInstance().$("Completed Quests"));
        completedQuestsMenuMeta.setLore(Arrays.asList("Click to see all your completed quests"));
        completedQuestsMenuItem.setItemMeta(completedQuestsMenuMeta);
        inventory.setItem(51, completedQuestsMenuItem);

        List<String> questsToDisplay = showingActiveQuests ? data.getActiveQuests() : data.getCompletedQuests();

        int index = 0;
        for (String questId : questsToDisplay) {
            if (index >= MIDDLE_SLOTS.length) break;
            Quest quest = questManager.getQuest(questId);
            if (quest == null) continue;

            ItemStack questItem = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = questItem.getItemMeta();
            meta.setDisplayName(Utils.getInstance().$(quest.getName()));

            List<String> lore = new ArrayList<>();
            lore.add(Utils.getInstance().$(quest.getDetails()));
            lore.add("");

            if (showingActiveQuests) {
                Map<String, Integer> progress = data.getQuestProgress().getOrDefault(questId, new HashMap<>());
                // Display objectives
                int phaseIndex = data.getQuestPhaseIndex().getOrDefault(questId,0);
                if (phaseIndex < quest.getPhases().size()) {
                    QuestPhase phase = quest.getPhases().get(phaseIndex);
                    for (String obj : phase.getObjectives()) {
                        int required = 1;
                        String[] parts = obj.split(":");
                        String formattedObjective = formatObjectiveKey(obj);
                        if (parts[0].equals("collect_item") || parts[0].equals("kill_mob")) {
                            required = Integer.parseInt(parts[2]);
                        }
                        int current = progress.getOrDefault(obj,0);
                        if (current>required) current=required;

                        double percent = ((double)current/required)*100;
                        String progressBar = getProgressBar(percent);

                        lore.add(Utils.getInstance().$(formattedObjective + ": " + current + "/" + required));
                        lore.add(progressBar);
                    }
                    if (questId.equals(data.getPinnedQuest())) {
                        lore.add("");
                        lore.add("This quest is pinned to your scoreboard.");
                    } else {
                        lore.add("");
                        lore.add("Click to pin this quest to your scoreboard.");
                    }
                }
            } else {
                lore.add("Quest Completed!");
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
        int progressBars = (int) (percent/(100.0/totalBars));
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

    public void onInventoryClick(InventoryClickEvent event) {
        // As per original code
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        if (clickedItem.getType()==Material.BARRIER && displayName.equalsIgnoreCase("Close")) {
            player.closeInventory();
        } else if (clickedItem.getType()==Material.CHEST_MINECART && displayName.equalsIgnoreCase("Completed Quests")) {
            QuestGUI newGui = new QuestGUI(player, questManager, playerDataCache, false);
            newGui.open();
        } else if (clickedItem.getType()==Material.HOPPER_MINECART && displayName.equalsIgnoreCase("Active Quests")) {
            QuestGUI newGui = new QuestGUI(player, questManager, playerDataCache, true);
            newGui.open();
        } else if (clickedItem.getType()==Material.WRITTEN_BOOK) {
            String questName = displayName;
            Quest quest = questManager.getQuestByName(questName);
            if (quest == null) return;
            long currentTime = System.currentTimeMillis();
            long lastPinTime = pinCooldowns.getOrDefault(player.getUniqueId(),0L);
            if (currentTime - lastPinTime < 2000) {
                player.sendMessage(Utils.getInstance().$("Please wait before pinning/unpinning another quest."));
                return;
            }
            pinCooldowns.put(player.getUniqueId(), currentTime);

            if (quest.getId().equals(data.getPinnedQuest())) {
                data.setPinnedQuest(null);
                player.sendMessage(Utils.getInstance().$("You have unpinned the quest: " + quest.getName()));
            } else {
                data.setPinnedQuest(quest.getId());
                player.sendMessage(Utils.getInstance().$("You have pinned the quest: " + quest.getName()));
            }
            open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK,1.0f,1.0f);
        }
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
}
