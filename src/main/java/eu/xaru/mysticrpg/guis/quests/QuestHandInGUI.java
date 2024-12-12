package eu.xaru.mysticrpg.guis.quests;

import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.HashMap;
import java.util.Map;

public class QuestHandInGUI {

    private final Player player;
    private final String questId;
    private final Map<Material, Integer> requiredItems;
    private final PlayerDataCache playerDataCache;
    private final QuestModule questModule;
    private final String objectiveKey;

    private Gui gui;
    private Window window;

    public QuestHandInGUI(Player player, String questId, Map<Material, Integer> requiredItems,
                          PlayerDataCache playerDataCache, QuestModule questModule, String objectiveKey) {
        this.player = player;
        this.questId = questId;
        this.requiredItems = requiredItems;
        this.playerDataCache = playerDataCache;
        this.questModule = questModule;
        this.objectiveKey = objectiveKey;
    }

    public void open() {
        // Define a 3x9 structure:
        //  - '#' represents placeholders for empty slots
        //  - 'C' represents the confirm button at the last slot (2,8)
        String[] layout = {
                "# # # # # # # # #",
                "# # # # # # # # #",
                "# # # # # # # # C"
        };

        // We'll first add filler for '#' and the confirm item for 'C', then after build, replace '#' with clickable empty slots.
        ItemProvider filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").addAllItemFlags();
        Item confirmButton = createConfirmButton();

        gui = Gui.normal()
                .setStructure(layout)
                .addIngredient('#', filler)
                .addIngredient('C', confirmButton)
                .build();

        // After building, iterate over all slots except the confirm slot (which is at index 26)
        // Structure is 3 rows of 9 = 27 slots total
        // Confirm is last slot: index 2*9+8=26
        for (int i = 0; i < 27; i++) {
            if (i == 26) continue; // confirm button slot
            // Replace filler '#' with an empty slot item allowing item placement
            gui.setItem(i, createEmptySlot(i));
        }

        window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.DARK_GREEN + "Hand in Items")
                .setGui(gui)
                .build();
        window.open();
    }

    private Item createEmptySlot(int slotIndex) {
        return new SimpleItem(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .setDisplayName(ChatColor.GRAY + "Empty Slot")
                .addAllItemFlags()) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                ItemStack cursorItem = clickPlayer.getItemOnCursor();
                ItemStack currentItem = event.getCurrentItem();

                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    // Placing item into this slot
                    ItemStack toPlace = cursorItem.clone();
                    clickPlayer.setItemOnCursor(null);
                    gui.setItem(slotIndex, new SimpleItem(toPlace) {
                        @Override
                        public void handleClick(@NotNull ClickType ct, @NotNull Player cp, @NotNull InventoryClickEvent e) {
                            handleSlotItemClick(slotIndex, cp, e);
                        }
                    });
                } else {
                    // Removing item from this slot
                    if (currentItem != null && currentItem.getType() != Material.WHITE_STAINED_GLASS_PANE) {
                        // There is some item here, put it on cursor
                        clickPlayer.setItemOnCursor(currentItem.clone());
                        gui.setItem(slotIndex, createEmptySlot(slotIndex));
                    } else {
                        // Empty slot, do nothing
                    }
                }
            }
        };
    }

    private void handleSlotItemClick(int slotIndex, Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack cursorItem = player.getItemOnCursor();
        ItemStack currentItem = event.getCurrentItem();

        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Replace current item with cursor item, move current item to cursor
            player.setItemOnCursor(currentItem);
            gui.setItem(slotIndex, new SimpleItem(cursorItem) {
                @Override
                public void handleClick(@NotNull ClickType ct, @NotNull Player cp, @NotNull InventoryClickEvent e) {
                    handleSlotItemClick(slotIndex, cp, e);
                }
            });
        } else {
            // Take item out
            player.setItemOnCursor(currentItem);
            gui.setItem(slotIndex, createEmptySlot(slotIndex));
        }
    }

    private Item createConfirmButton() {
        return new SimpleItem(new ItemBuilder(Material.GREEN_WOOL)
                .setDisplayName(ChatColor.GREEN + "Confirm Hand-In")
                .addLoreLines("Click to confirm and submit items.")
                .addAllItemFlags())
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);

                Map<Material,Integer> collected = new HashMap<>();

                // Collect items from slots 0-25
                for (int i = 0; i < 26; i++) {
                    Item slotItem = gui.getItem(i);
                    ItemStack stack = (slotItem != null) ? slotItem.getItemProvider().get(null) : null;
                    if (stack != null && stack.getType() != Material.AIR) {
                        collected.put(stack.getType(), collected.getOrDefault(stack.getType(), 0) + stack.getAmount());
                    }
                }

                boolean allMet = true;
                for (Map.Entry<Material,Integer> req : requiredItems.entrySet()) {
                    int have = collected.getOrDefault(req.getKey(),0);
                    if (have < req.getValue()) {
                        allMet = false;
                        break;
                    }
                }

                if (!allMet) {
                    returnItemsToPlayer(clickPlayer);
                    clickPlayer.sendMessage(Utils.getInstance().$("You didn't provide enough items!"));
                    clickPlayer.closeInventory();
                    return;
                }

                removeRequiredItemsAndReturnLeftovers(clickPlayer);

                PlayerData data = playerDataCache.getCachedPlayerData(clickPlayer.getUniqueId());
                if (data != null) {
                    questModule.updateObjectiveProgress(data, objectiveKey, 1);
                }

                clickPlayer.sendMessage(Utils.getInstance().$("Quest phase completed!"));
                clickPlayer.closeInventory();
            }
        };
    }

    private void returnItemsToPlayer(Player player) {
        // Return all items from slots 0-25
        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem != null) {
                ItemStack stack = slotItem.getItemProvider().get(null);
                if (stack != null && stack.getType() != Material.AIR) {
                    player.getInventory().addItem(stack.clone());
                }
            }
        }
    }

    private void removeRequiredItemsAndReturnLeftovers(Player player) {
        Map<Material,Integer> toRemove = new HashMap<>(requiredItems);

        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem == null) continue;
            ItemStack stack = slotItem.getItemProvider().get(null);
            if (stack == null || stack.getType() == Material.AIR) continue;

            Material mat = stack.getType();
            if (toRemove.containsKey(mat)) {
                int needed = toRemove.get(mat);
                int remove = Math.min(stack.getAmount(), needed);
                stack.setAmount(stack.getAmount() - remove);
                needed -= remove;
                toRemove.put(mat, needed);
                if (needed <= 0) {
                    toRemove.remove(mat);
                }
            }

            if (stack.getAmount() <= 0) {
                gui.setItem(i, createEmptySlot(i));
            } else {
                int finalI = i;
                gui.setItem(i, new SimpleItem(stack) {
                    @Override
                    public void handleClick(@NotNull ClickType ct, @NotNull Player cp, @NotNull InventoryClickEvent e) {
                        handleSlotItemClick(finalI, cp, e);
                    }
                });
            }
        }

        // Return all leftovers
        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem != null) {
                ItemStack stack = slotItem.getItemProvider().get(null);
                if (stack != null && stack.getType() != Material.AIR) {
                    player.getInventory().addItem(stack.clone());
                }
            }
        }
    }
}
