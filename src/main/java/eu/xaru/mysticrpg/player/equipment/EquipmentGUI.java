package eu.xaru.mysticrpg.player.equipment;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class EquipmentGUI implements Listener {

    private final JavaPlugin plugin = JavaPlugin.getPlugin(MysticCore.class);
    private final String GUI_TITLE = ChatColor.DARK_PURPLE + "Equipment";
    private final int GUI_SIZE = 54; // 6 rows of 9 (double chest)
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    // Define equipment slots and their GUI positions (template and item slots)
    private final Map<String, Integer> equipmentTemplateSlots = Map.of(
            "Helmet", 10,
            "Chestplate", 19,
            "Leggings", 28,
            "Boots", 37,
            "Amulet", 12,
            "Cloak", 21,
            "Gloves", 30
    );

    private final Map<String, Integer> equipmentItemSlots = Map.of(
            "Helmet", 11,
            "Chestplate", 20,
            "Leggings", 29,
            "Boots", 38,
            "Amulet", 13,
            "Cloak", 22,
            "Gloves", 31
    );

    private final ItemManager itemManager;

    public EquipmentGUI() {
        // Get the ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule != null) {
            this.itemManager = customItemModule.getItemManager();
        } else {
            this.itemManager = null;
        }
    }

    public void openEquipmentGUI(Player player) {
        Inventory gui = CustomInventoryManager.createInventory(GUI_SIZE, GUI_TITLE);

        // Create filler item
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        fillerMeta.setLocalizedName("filler");
        filler.setItemMeta(fillerMeta);

        // Fill the entire inventory with the filler
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, filler);
        }

        // Add equipment slot icons and placeholders
        for (String slotName : equipmentTemplateSlots.keySet()) {
            int templatePosition = equipmentTemplateSlots.get(slotName);
            int itemPosition = equipmentItemSlots.get(slotName);

            // Icon representing the equipment slot (template)
            ItemStack icon = createSlotIcon(slotName);
            gui.setItem(templatePosition, icon); // Place icon

            // Placeholder for the equipment item
            ItemStack placeholder = createPlaceholderItem(slotName);
            gui.setItem(itemPosition, placeholder); // Place placeholder
        }

        // Load player's equipment items
        loadPlayerEquipment(player, gui);

        // Store the open inventory
        openInventories.put(player.getUniqueId(), gui);

        // Open the GUI for the player
        player.openInventory(gui);
    }

    private ItemStack createSlotIcon(String slotName) {
        Material material;
        switch (slotName) {
            case "Helmet":
                material = Material.DIAMOND_HELMET;
                break;
            case "Chestplate":
                material = Material.DIAMOND_CHESTPLATE;
                break;
            case "Leggings":
                material = Material.DIAMOND_LEGGINGS;
                break;
            case "Boots":
                material = Material.DIAMOND_BOOTS;
                break;
            case "Amulet":
                material = Material.HEART_OF_THE_SEA;
                break;
            case "Cloak":
                material = Material.ELYTRA;
                break;
            case "Gloves":
                material = Material.LEATHER;
                break;
            default:
                material = Material.BARRIER;
        }

        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(Utils.getInstance().$(ChatColor.GOLD + slotName));
        meta.setLocalizedName("icon");
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createPlaceholderItem(String slotName) {
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(Utils.getInstance().$(ChatColor.GRAY + "Empty " + slotName + " Slot"));
        meta.setLocalizedName("placeholder");
        placeholder.setItemMeta(meta);
        return placeholder;
    }

    private void loadPlayerEquipment(Player player, Inventory gui) {
        PlayerData playerData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Map<String, String> equipment = playerData.getEquipment();
        if (equipment == null) {
            // Initialize equipment map if null
            equipment = new HashMap<>();
            playerData.setEquipment(equipment);
        }

        for (String slotName : equipmentItemSlots.keySet()) {
            int itemPosition = equipmentItemSlots.get(slotName);

            String serializedItem = equipment.get(slotName);
            if (serializedItem != null) {
                ItemStack item = deserializeItemStack(serializedItem);
                gui.setItem(itemPosition, item);

                // Equip armor pieces in the player's inventory
                equipArmorPiece(player, slotName, item);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEquipmentGUI(event.getView())) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getRawSlot();

        if (clickedInventory != null && clickedInventory.getType() == InventoryType.CHEST) {
            // Clicked inside the GUI
            String slotName = getSlotNameByItemPosition(slot);

            if (slotName != null) {
                // Clicked on an equipment item slot
                ItemStack cursorItem = event.getCursor();
                ItemStack currentItem = event.getCurrentItem();

                if (event.isLeftClick() || event.isRightClick()) {
                    if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                        // Placing item into equipment slot
                        if (isValidItemForSlot(slotName, cursorItem)) {
                            event.setCancelled(true);

                            // Update the GUI
                            clickedInventory.setItem(slot, cursorItem);
                            player.setItemOnCursor(null);

                            // Update player data
                            saveEquipmentItem(player, slotName, cursorItem);

                            // Equip armor pieces
                            equipArmorPiece(player, slotName, cursorItem);
                        } else {
                            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You cannot place that item in the " + slotName + " slot."));
                            event.setCancelled(true);
                        }
                    } else {
                        // Picking up item from equipment slot
                        event.setCancelled(true);

                        // Update player data
                        saveEquipmentItem(player, slotName, null);

                        // Unequip armor pieces
                        equipArmorPiece(player, slotName, null);

                        // Give the item to the player's cursor
                        player.setItemOnCursor(currentItem);

                        // Replace the slot with a placeholder
                        clickedInventory.setItem(slot, createPlaceholderItem(slotName));
                    }
                } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    // Prevent shift-clicking in the GUI
                    event.setCancelled(true);
                } else {
                    event.setCancelled(true);
                }
            } else {
                // Clicked on a non-equipment slot (e.g., filler pane or template icon)
                event.setCancelled(true);
            }
        } else if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
            // Clicked in player inventory
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Shift-clicking from player inventory into equipment GUI
                ItemStack clickedItem = event.getCurrentItem();
                String slotName = getSlotNameForItem(clickedItem);
                if (slotName != null) {
                    event.setCancelled(true);

                    // Place the item into the equipment slot if possible
                    int equipmentSlot = equipmentItemSlots.get(slotName);
                    Inventory guiInventory = event.getView().getTopInventory();

                    // Check if equipment slot is empty
                    ItemStack equipmentSlotItem = guiInventory.getItem(equipmentSlot);
                    if (isPlaceholderItem(equipmentSlotItem)) {
                        // Update the GUI
                        guiInventory.setItem(equipmentSlot, clickedItem);
                        event.getClickedInventory().setItem(event.getSlot(), null);

                        // Update player data
                        saveEquipmentItem(player, slotName, clickedItem);

                        // Equip armor pieces
                        equipArmorPiece(player, slotName, clickedItem);
                    } else {
                        player.sendMessage(Utils.getInstance().$(ChatColor.RED + slotName + " slot is already occupied."));
                    }
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    private String getSlotNameForItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        if (isHelmet(item)) return "Helmet";
        if (isChestplate(item)) return "Chestplate";
        if (isLeggings(item)) return "Leggings";
        if (isBoots(item)) return "Boots";
        if (isAmulet(item)) return "Amulet";
        if (isCloak(item)) return "Cloak";
        if (isGloves(item)) return "Gloves";
        return null;
    }

    private boolean isPlaceholderItem(ItemStack item) {
        if (item == null || item.getType() != Material.GRAY_STAINED_GLASS_PANE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && "placeholder".equals(meta.getLocalizedName());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isEquipmentGUI(event.getView())) return;

        Player player = (Player) event.getWhoClicked();

        // Get the slots in the GUI that are being dragged into
        Set<Integer> guiSlots = event.getRawSlots();
        for (int slot : guiSlots) {
            if (slot < event.getView().getTopInventory().getSize()) {
                // Slot is in the GUI
                String slotName = getSlotNameByItemPosition(slot);
                if (slotName == null) {
                    // Dragging over non-equipment slots (e.g., filler panes)
                    event.setCancelled(true);
                    return;
                } else {
                    // Dragging into an equipment item slot
                    ItemStack draggedItem = event.getOldCursor();
                    if (!isValidItemForSlot(slotName, draggedItem)) {
                        player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You cannot place that item in the " + slotName + " slot."));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Cancel the event and manually handle the drag
        event.setCancelled(true);

        Map<Integer, ItemStack> newItems = event.getNewItems();
        for (Map.Entry<Integer, ItemStack> entry : newItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            String slotName = getSlotNameByItemPosition(slot);
            if (slotName != null && isValidItemForSlot(slotName, item)) {
                event.getInventory().setItem(slot, item);

                // Update player data
                saveEquipmentItem(player, slotName, item);

                // Equip armor pieces
                equipArmorPiece(player, slotName, item);

                // Remove item from cursor
                int amount = event.getOldCursor().getAmount() - item.getAmount();
                if (amount > 0) {
                    event.setCursor(new ItemStack(event.getOldCursor().getType(), amount));
                } else {
                    event.setCursor(null);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isEquipmentGUI(event.getView())) return;

        HumanEntity player = event.getPlayer();
        openInventories.remove(player.getUniqueId());
    }

    private boolean isEquipmentGUI(InventoryView view) {
        return view.getTitle().equals(GUI_TITLE);
    }

    private String getSlotNameByItemPosition(int position) {
        for (Map.Entry<String, Integer> entry : equipmentItemSlots.entrySet()) {
            if (entry.getValue() == position) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isValidItemForSlot(String slotName, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true; // Allow removing items

        switch (slotName) {
            case "Helmet":
                return isHelmet(item);
            case "Chestplate":
                return isChestplate(item);
            case "Leggings":
                return isLeggings(item);
            case "Boots":
                return isBoots(item);
            case "Amulet":
                return isAmulet(item);
            case "Cloak":
                return isCloak(item);
            case "Gloves":
                return isGloves(item);
            default:
                return false;
        }
    }

    private boolean isHelmet(ItemStack item) {
        if (item.getType().name().endsWith("_HELMET")) return true;
        return isCustomArmorPiece(item, "HELMET");
    }

    private boolean isChestplate(ItemStack item) {
        if (item.getType().name().endsWith("_CHESTPLATE")) return true;
        return isCustomArmorPiece(item, "CHESTPLATE");
    }

    private boolean isLeggings(ItemStack item) {
        if (item.getType().name().endsWith("_LEGGINGS")) return true;
        return isCustomArmorPiece(item, "LEGGINGS");
    }

    private boolean isBoots(ItemStack item) {
        if (item.getType().name().endsWith("_BOOTS")) return true;
        return isCustomArmorPiece(item, "BOOTS");
    }

    private boolean isAmulet(ItemStack item) {
        // Add logic for custom amulets if needed
        return item.getType() == Material.HEART_OF_THE_SEA || item.getType() == Material.AMETHYST_SHARD;
    }

    private boolean isCloak(ItemStack item) {
        // Add logic for custom cloaks if needed
        return item.getType() == Material.ELYTRA;
    }

    private boolean isGloves(ItemStack item) {
        // Add logic for custom gloves if needed
        return item.getType() == Material.LEATHER || item.getType() == Material.RABBIT_HIDE;
    }

    private boolean isCustomArmorPiece(ItemStack item, String armorType) {
        if (itemManager == null) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey key = new NamespacedKey(plugin, "custom_item_id");
        String customItemId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (customItemId != null) {
            // Retrieve the custom item from your ItemManager
            CustomItem customItem = itemManager.getCustomItem(customItemId);
            if (customItem != null && customItem.getArmorType() != null && customItem.getArmorType().equalsIgnoreCase(armorType)) {
                return true;
            }
        }

        return false;
    }

    private void saveEquipmentItem(Player player, String slotName, ItemStack item) {
        PlayerData playerData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Map<String, String> equipment = playerData.getEquipment();
        if (item == null || item.getType() == Material.AIR) {
            equipment.remove(slotName);
        } else {
            String serializedItem = serializeItemStack(item);
            equipment.put(slotName, serializedItem);
        }
    }

    private void equipArmorPiece(Player player, String slotName, ItemStack item) {
        switch (slotName) {
            case "Helmet":
                player.getInventory().setHelmet(item);
                break;
            case "Chestplate":
                player.getInventory().setChestplate(item);
                break;
            case "Leggings":
                player.getInventory().setLeggings(item);
                break;
            case "Boots":
                player.getInventory().setBoots(item);
                break;
            // Accessories can have custom effects applied here
            default:
                break;
        }
    }

    // Serialization and deserialization methods
    private String serializeItemStack(ItemStack item) {
        if (item == null) return null;
        try {
            return Utils.getInstance().itemStackToBase64(item);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ItemStack deserializeItemStack(String data) {
        if (data == null) return null;
        try {
            return Utils.getInstance().itemStackFromBase64(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
