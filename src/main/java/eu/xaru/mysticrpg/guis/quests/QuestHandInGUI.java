package eu.xaru.mysticrpg.guis.quests;

import eu.xaru.mysticrpg.quests.QuestManager;
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
    private final QuestManager questManager;
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
        this.questManager = questModule.getQuestManager();
    }

    public void open() {
        // Define a 3x9 structure:
        //  - '#' represents placeholders for empty slots
        //  - 'C' represents the confirm button at the last slot (2,8)
        String[] layout = {
                "# # # # # # # # #",
                "# X # # # # # # #",
                "# # # # # # # # C"
        };

        // Create filler and confirm button items
        ItemProvider filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags();
        Item confirmButton = createConfirmButton();

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        // Build the GUI structure
        gui = Gui.normal()
                .setStructure(layout)
                .addIngredient('X', filler)
                .addIngredient('#', border)
                .addIngredient('C', confirmButton)
                .build();

        // Replace filler '#' with interactive empty slots
        for (int i = 0; i < 27; i++) {
            if (i == 26) continue; // Skip confirm button slot
            gui.setItem(i, createEmptySlot(i));
        }

        // Create and open the window
        window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.DARK_GREEN + "Hand in Items")
                .setGui(gui)
                .build();
        window.open();
    }

    /**
     * Creates an interactive empty slot where players can place items.
     *
     * @param slotIndex The index of the slot in the GUI.
     * @return An interactive Item representing the empty slot.
     */
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
                    }
                }
            }
        };
    }

    /**
     * Handles clicks on items placed in the GUI slots.
     *
     * @param slotIndex The index of the slot being interacted with.
     * @param player    The player interacting with the slot.
     * @param event     The inventory click event.
     */
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

    /**
     * Creates the confirm button with the necessary click handling.
     *
     * @return The confirm button Item.
     */
    private Item createConfirmButton() {
        return new SimpleItem(new ItemBuilder(Material.GREEN_WOOL)
                .setDisplayName(ChatColor.GREEN + "Confirm Hand-In")
                .addLoreLines("Click to confirm and submit items.")
                .addAllItemFlags()) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);

                Map<Material, Integer> collected = new HashMap<>();
                boolean hasUserAddedItems = false;

                // Collect items from slots 0-25, excluding placeholders
                for (int i = 0; i < 26; i++) {
                    Item slotItem = gui.getItem(i);
                    if (slotItem == null) continue;
                    ItemProvider provider = slotItem.getItemProvider();
                    if (provider == null) continue;
                    ItemStack stack = provider.get(null);
                    if (stack == null || stack.getType() == Material.AIR) continue;

                    // Check if the item is a placeholder (Empty Slot)
                    String displayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                            ? stack.getItemMeta().getDisplayName()
                            : "";
                    if (displayName.equals(ChatColor.GRAY + "Empty Slot")) {
                        continue; // Skip placeholder
                    }

                    // This is a user-added item
                    hasUserAddedItems = true;
                    collected.put(stack.getType(), collected.getOrDefault(stack.getType(), 0) + stack.getAmount());
                }

                // If no user-added items were placed, simply close the GUI without doing anything
                if (!hasUserAddedItems) {
                    clickPlayer.closeInventory();
                    return;
                }

                boolean allMet = true;
                for (Map.Entry<Material, Integer> req : requiredItems.entrySet()) {
                    int have = collected.getOrDefault(req.getKey(), 0);
                    if (have < req.getValue()) {
                        allMet = false;
                        break;
                    }
                }

                if (!allMet) {
                    // Only return items if some were placed
                    if (!collected.isEmpty()) {
                        returnItemsToPlayer(clickPlayer);
                    }
                    clickPlayer.sendMessage(Utils.getInstance().$("You didn't provide enough items!"));
                    clickPlayer.closeInventory();
                    return;
                }

                removeRequiredItemsAndReturnLeftovers(clickPlayer);

                PlayerData data = playerDataCache.getCachedPlayerData(clickPlayer.getUniqueId());
                if (data != null) {
                    questManager.updateObjectiveProgress(data, objectiveKey, 1);
                }

                clickPlayer.sendMessage(Utils.getInstance().$("Quest phase completed!"));
                clickPlayer.closeInventory();
            }
        };
    }

    /**
     * Returns all user-added items back to the player's inventory.
     *
     * @param player The player to return items to.
     */
    private void returnItemsToPlayer(Player player) {
        // Return all user-added items from slots 0-25
        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem == null) continue;
            ItemProvider provider = slotItem.getItemProvider();
            if (provider == null) continue;
            ItemStack stack = provider.get(null);
            if (stack == null || stack.getType() == Material.AIR) continue;

            // Check if the item is a placeholder (Empty Slot)
            String displayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                    ? stack.getItemMeta().getDisplayName()
                    : "";
            if (displayName.equals(ChatColor.GRAY + "Empty Slot")
            ) {
                continue; // Skip placeholder
            }

            player.getInventory().addItem(stack.clone());
        }
    }

    /**
     * Removes the required items from the GUI and returns any leftovers to the player's inventory.
     *
     * @param player The player performing the hand-in.
     */
    private void removeRequiredItemsAndReturnLeftovers(Player player) {
        Map<Material, Integer> toRemove = new HashMap<>(requiredItems);

        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem == null) continue;
            ItemProvider provider = slotItem.getItemProvider();
            if (provider == null) continue;
            ItemStack stack = provider.get(null);
            if (stack == null || stack.getType() == Material.AIR) continue;

            // Check if the item is a placeholder (Empty Slot)
            String displayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                    ? stack.getItemMeta().getDisplayName()
                    : "";
            if (displayName.equals(ChatColor.GRAY + "Empty Slot")
            ) {
                continue; // Skip placeholder
            }

            Material mat = stack.getType();
            if (toRemove.containsKey(mat)) {
                int needed = toRemove.get(mat);
                int remove = Math.min(stack.getAmount(), needed);
                stack.setAmount(stack.getAmount() - remove);
                needed -= remove;
                if (needed > 0) {
                    toRemove.put(mat, needed);
                } else {
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

        // Return all leftovers (items that were not required)
        for (int i = 0; i < 26; i++) {
            Item slotItem = gui.getItem(i);
            if (slotItem == null) continue;
            ItemProvider provider = slotItem.getItemProvider();
            if (provider == null) continue;
            ItemStack stack = provider.get(null);
            if (stack == null || stack.getType() == Material.AIR) continue;

            // Check if the item is a placeholder (Empty Slot)
            String displayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                    ? stack.getItemMeta().getDisplayName()
                    : "";
            if (displayName.equals(ChatColor.GRAY + "Empty Slot")
            ) {
                continue; // Skip placeholder
            }

            player.getInventory().addItem(stack.clone());
            gui.setItem(i, createEmptySlot(i));
        }
    }
}
