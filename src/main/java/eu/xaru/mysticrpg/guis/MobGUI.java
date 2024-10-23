package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.player.stats.StatMenu;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI for displaying all registered custom mobs as their default heads.
 */
public class MobGUI {

    private final MobManager mobManager;
    private final String guiTitle = "&cRegistered Mobs"; // Using color codes
    private final JavaPlugin plugin;

    /**
     * Constructor for the MobGUI.
     *
     * @param mobManager The MobManager instance to access mob configurations and spawning.
     */
    public MobGUI(MobManager mobManager) {
        this.mobManager = mobManager;
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Ensure MysticCore is your main class
    }

    /**
     * Opens the Mob GUI for the specified player.
     *
     * @param player The player to open the GUI for.
     */
    public void openMobGUI(Player player) {
        // Retrieve all registered mob configurations
        Map<String, CustomMob> mobConfigurations = mobManager.getMobConfigurations();
        int mobCount = mobConfigurations.size();

        // Calculate the required inventory size (multiple of 9, up to 54)
        int size = 9;
        while (size < mobCount && size < 54) {
            size += 9;
        }

        // Create the custom inventory with the calculated size and title
        Inventory mobInventory = CustomInventoryManager.createInventory(size, guiTitle);

        // Create a placeholder item (e.g., black stained glass pane) for aesthetics
        ItemStack placeholder = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");
        CustomInventoryManager.fillEmptySlots(mobInventory, placeholder);

        // Populate the inventory with mob heads
        int slot = 0;
        for (CustomMob customMob : mobConfigurations.values()) {
            // Determine the appropriate head material based on the entity type
            Material headMaterial = getHeadMaterial(customMob.getEntityType());

            // If the entity has a specific head material, use it; otherwise, fallback to PLAYER_HEAD
            ItemStack mobHead;
            if (headMaterial != null) {
                mobHead = new ItemStack(headMaterial, 1);
            } else {
                mobHead = new ItemStack(Material.PLAYER_HEAD, 1);
            }

            // Set the display name of the head to the mob's name
            CustomInventoryManager.setItemDisplayName(mobHead, ChatColor.YELLOW + customMob.getName());

            // Optionally, add lore or other metadata here
            /*
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Health: " + customMob.getHealth());
            lore.add(ChatColor.GRAY + "Level: " + customMob.getLevel());
            lore.add(ChatColor.GRAY + "Rewards: " + customMob.getExperienceReward() + " XP, " + customMob.getCurrencyReward() + " Coins");
            CustomInventoryManager.setItemLore(mobHead, lore);
            */

            // Place the mob head in the inventory
            if (slot < mobInventory.getSize()) {
                mobInventory.setItem(slot, mobHead);
                slot++;
            }
        }

        // Open the inventory for the player
        CustomInventoryManager.openInventory(player, mobInventory);
    }

    /**
     * Registers event handlers related to the Mob GUI using the provided EventManager.
     *
     * @param eventManager The EventManager instance to register events with.
     */
    public void registerMobGuiEvents(EventManager eventManager) {
        // Handle Inventory Clicks within the Mob GUI
        eventManager.registerEvent(InventoryClickEvent.class, new Consumer<InventoryClickEvent>() {
            @Override
            public void accept(InventoryClickEvent event) {
                Inventory clickedInventory = event.getClickedInventory();
                if (clickedInventory == null) return;

                // Correctly access the inventory title using event.getView().getTitle()
                String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
                if (!inventoryTitle.equalsIgnoreCase(ChatColor.stripColor(guiTitle))) {
                    return; // Not the Mob GUI, ignore
                }

                // Prevent any item movement within the Mob GUI
                event.setCancelled(true);

                // Get the clicked item
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }

                // Get the display name of the clicked item
                String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                // Retrieve the corresponding CustomMob based on the display name
                CustomMob selectedMob = null;
                for (CustomMob customMob : mobManager.getMobConfigurations().values()) {
                    if (customMob.getName().equalsIgnoreCase(displayName)) {
                        selectedMob = customMob;
                        break;
                    }
                }

                if (selectedMob == null) {
                    // Clicked item does not correspond to any registered mob
                    return;
                }

                // Spawn the custom mob at the player's location
                Player player = (Player) event.getWhoClicked();
                mobManager.spawnMobAtLocation(selectedMob, player.getLocation());

                // Provide feedback to the player
                player.sendMessage(ChatColor.GREEN + "Spawned mob: " + selectedMob.getName());

                // Play a sound for feedback
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }, EventPriority.HIGH);

        // Handle Inventory Dragging within the Mob GUI
        eventManager.registerEvent(InventoryDragEvent.class, new Consumer<InventoryDragEvent>() {
            @Override
            public void accept(InventoryDragEvent event) {
                // Check if the inventory being dragged is the Mob GUI
                String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
                if (!inventoryTitle.equalsIgnoreCase(ChatColor.stripColor(guiTitle))) {
                    return; // Not the Mob GUI, ignore
                }

                // Prevent any item dragging within the Mob GUI
                event.setCancelled(true);
            }
        }, EventPriority.HIGH);

        // Handle Item Dropping while the Mob GUI is open
        eventManager.registerEvent(PlayerDropItemEvent.class, new Consumer<PlayerDropItemEvent>() {
            @Override
            public void accept(PlayerDropItemEvent event) {
                Player player = event.getPlayer();
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory().getHolder() instanceof StatMenu) {
                    event.setCancelled(true); // Prevent item dropping
                    player.sendMessage(ChatColor.RED + "You cannot drop items from the Mob GUI.");
                }
            }
        }, EventPriority.HIGH);
    }

    /**
     * Maps an EntityType to its corresponding Minecraft head material.
     *
     * @param entityType The EntityType of the mob.
     * @return The corresponding head Material, or null if no specific head exists.
     */
    private Material getHeadMaterial(org.bukkit.entity.EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case SKELETON:
                return Material.SKELETON_SKULL;
            case CREEPER:
                return Material.CREEPER_HEAD;
            case WITHER_SKELETON:
                return Material.WITHER_SKELETON_SKULL;
            case PLAYER:
                return Material.PLAYER_HEAD;
            // Add more cases as needed for other EntityTypes that have default head items
            default:
                return null; // No specific head material exists for this EntityType
        }
    }
}
