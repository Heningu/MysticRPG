package eu.xaru.mysticrpg.player.equipment;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
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
import org.bukkit.plugin.java.JavaPlugin;

public class EquipmentManager implements Listener {
    private final JavaPlugin plugin;
    
    private final ItemManager itemManager;

    public EquipmentManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Get the ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule != null) {
            this.itemManager = customItemModule.getItemManager();
        } else {
            this.itemManager = null;
        }

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);}

    /**
     * Provides access to the EquipmentGUI instance.
     *
     * @return The EquipmentGUI instance.
     */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent players from interacting with armor slots in their inventory
        if (event.getWhoClicked() instanceof Player) {
            if (event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.PLAYER) {
                if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                    event.setCancelled(true);
                }

                // Prevent shift-clicking armor into armor slots
                if (event.getClick().isShiftClick()) {
                    ItemStack item = event.getCurrentItem();
                    if (isArmorItem(item)) {
                        event.setCancelled(true);
                    }
                }

                // Prevent swapping armor with number keys
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                    if (isArmorItem(hotbarItem)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        // Check if it's a right-click action and the player is holding an armor item
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && isArmorItem(item)) {
            // Cancel the event to prevent vanilla armor equipping
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must use the equipment GUI to equip armor!");
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

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey key = new NamespacedKey(plugin, "custom_item_id");
        String customItemId = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);

        if (customItemId != null) {
            CustomItem customItem = itemManager.getCustomItem(customItemId);
            if (customItem != null && customItem.getArmorType() != null) {
                return true;
            }
        }

        return false;
    }
}
