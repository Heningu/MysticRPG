package eu.xaru.mysticrpg.player.equipment;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class EquipmentManager implements Listener {
    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final PlayerDataCache playerDataCache;
    private final NamespacedKey customItemKey;

    public EquipmentManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Get the ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule != null) {
            this.itemManager = customItemModule.getItemManager();
        } else {
            this.itemManager = null;
        }

        this.playerDataCache = PlayerDataCache.getInstance();
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Handle moving armor items into or out of armor slots via normal inventory
        // If player places or removes armor from their armor slot, we update PlayerData.
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            // Let the action happen, then update PlayerData after the event is processed
            Bukkit.getScheduler().runTask(plugin, () -> updatePlayerDataFromInventory(player));
        }

        // If they shift-click or number key into armor slots, it's now allowed.
        // We rely on the update after the action to keep PlayerData in sync.
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // If drag involves armor slots, allow it and then update after the event
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if armor slots are affected
        boolean affectsArmor = event.getRawSlots().stream().anyMatch(slot -> slot >= 5 && slot <= 8);
        // Armor slots in Player Inventory: Helmet: slot 5, Chest:6, Leg:7, Boots:8 in the raw inventory indexing
        // We'll just update after drag if affectsArmor
        if (affectsArmor) {
            Bukkit.getScheduler().runTask(plugin, () -> updatePlayerDataFromInventory(player));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        // If player right-clicks while holding armor, we now allow vanilla equip by placing in armor slot too.
        // So we remove the previous restriction that forced GUI usage only.
        // We no longer cancel the event.
        // If vanilla tries to equip armor, after the equip we do updatePlayerDataFromInventory to sync.

        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && isArmorItem(item)) {
            // After a short delay, update PlayerData if armor got equipped by vanilla
            Bukkit.getScheduler().runTask(plugin, () -> updatePlayerDataFromInventory(player));
        }
    }

    /**
     * Updates the PlayerData equipment map based on what the player currently has equipped in their armor slots.
     * This ensures no duplication between inventory and equipment GUI states.
     */
    private void updatePlayerDataFromInventory(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;
        Map<String, String> equipment = data.getEquipment();
        if (equipment == null) return;

        // Helmet: player.getInventory().getHelmet()
        setEquipmentSlot(player, equipment, "Helmet", player.getInventory().getHelmet());
        setEquipmentSlot(player, equipment, "Chestplate", player.getInventory().getChestplate());
        setEquipmentSlot(player, equipment, "Leggings", player.getInventory().getLeggings());
        setEquipmentSlot(player, equipment, "Boots", player.getInventory().getBoots());
        // The other slots (Amulet, Cloak, Gloves, Core) are only managed by GUI and not part of vanilla armor
        // If you want them managed from inventory too, you'd need custom logic and probably a different approach.

        // No direct event call here, presumably stats update from other triggers
    }

    private void setEquipmentSlot(Player player, Map<String, String> equipment, String slotName, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            equipment.remove(slotName);
        } else {
            String serialized = serializeItemStack(item);
            if (serialized != null) {
                equipment.put(slotName, serialized);
            } else {
                equipment.remove(slotName);
            }
        }
    }

    private boolean isArmorItem(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE")
                || typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS")
                || isCustomArmor(item);
    }

    private boolean isCustomArmor(ItemStack item) {
        if (itemManager == null) return false;
        if (item == null || item.getType() == Material.AIR) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String customItemId = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);

        if (customItemId != null) {
            CustomItem customItem = itemManager.getCustomItem(customItemId);
            return customItem != null && customItem.getArmorType() != null;
        }

        return false;
    }

    private String serializeItemStack(ItemStack item) {
        if (item == null) return null;
        try {
            return Utils.getInstance().itemStackToBase64(item);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
