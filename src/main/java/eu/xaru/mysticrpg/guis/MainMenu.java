package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

/**
 * Represents the Main Menu GUI for the MysticRPG plugin.
 */
public class MainMenu {

    private static final String INVENTORY_TITLE = "Main Menu";
    private static final int INVENTORY_SIZE = 54;

    /**
     * Creates and returns the main menu inventory for the specified player.
     *
     * @param player The player who is opening the GUI.
     * @return The Inventory instance representing the main menu.
     */
    public Inventory getInventory(Player player) {
        Inventory mainMenu = CustomInventoryManager.createInventory(INVENTORY_SIZE, INVENTORY_TITLE);

        // Create the Auctions item
        ItemStack auctionsItem = CustomInventoryManager.createPlaceholder(Material.CHEST, ChatColor.GREEN + "Auctions");
        CustomInventoryManager.setItemLore(auctionsItem, Arrays.asList(
                ChatColor.GRAY + "Click to access the Auctions",
                ChatColor.GRAY + "house and manage your listings."
        ));

        // Add the Auctions item to slot 11 (center-left)
        CustomInventoryManager.addItemToSlot(mainMenu, 11, auctionsItem);

        // Create the Equipment item
        ItemStack equipmentItem = CustomInventoryManager.createPlaceholder(Material.DIAMOND_CHESTPLATE, ChatColor.AQUA + "Equipment");
        CustomInventoryManager.setItemLore(equipmentItem, Arrays.asList(
                ChatColor.GRAY + "Click to manage your",
                ChatColor.GRAY + "equipment and gear."
        ));

        // Add the Equipment item to slot 15 (center-right)
        CustomInventoryManager.addItemToSlot(mainMenu, 15, equipmentItem);

        // Create the Leveling item
        ItemStack levelingItem = CustomInventoryManager.createPlaceholder(Material.EXPERIENCE_BOTTLE, ChatColor.LIGHT_PURPLE + "Leveling");
        CustomInventoryManager.setItemLore(levelingItem, Arrays.asList(
                ChatColor.GRAY + "Click to view and",
                ChatColor.GRAY + "manage your leveling progress."
        ));

        // Add the Leveling item to slot 13 (center)
        CustomInventoryManager.addItemToSlot(mainMenu, 13, levelingItem);

        // Create the Stats item
        ItemStack statsItem = CustomInventoryManager.createPlaceholder(Material.BOOK, ChatColor.BLUE + "Stats");
        CustomInventoryManager.setItemLore(statsItem, Arrays.asList(
                ChatColor.GRAY + "Click to view and",
                ChatColor.GRAY + "enhance your attributes."
        ));

        // Add the Stats item to slot 22 (bottom-left)
        CustomInventoryManager.addItemToSlot(mainMenu, 22, statsItem);

        // **Create the Quests item**
        ItemStack questsItem = CustomInventoryManager.createPlaceholder(Material.WRITABLE_BOOK, ChatColor.GOLD + "Quests");
        CustomInventoryManager.setItemLore(questsItem, Arrays.asList(
                ChatColor.GRAY + "Click to view and",
                ChatColor.GRAY + "manage your quests."
        ));

        // **Add the Quests item to slot 16 (bottom-right)**
        CustomInventoryManager.addItemToSlot(mainMenu, 16, questsItem);

        // **Create the Friends item (Player Head)**
        ItemStack friendsItem = createPlayerHead(player, "Friends");
        CustomInventoryManager.setItemLore(friendsItem, Arrays.asList(
                ChatColor.GRAY + "Click to view and",
                ChatColor.GRAY + "manage your friends."
        ));

        // **Add the Friends item to slot 19 (adjust as needed)**
        CustomInventoryManager.addItemToSlot(mainMenu, 19, friendsItem);

        // **Create the Party item (Cake)**
        ItemStack partyItem = createPartyCake("Party");
        CustomInventoryManager.setItemLore(partyItem, Arrays.asList(
                ChatColor.GRAY + "Click to view and",
                ChatColor.GRAY + "manage your parties."
        ));

        // **Add the Party item to slot 25 (adjust as needed)**
        CustomInventoryManager.addItemToSlot(mainMenu, 25, partyItem);

        // You can add more items to the main menu here following the same pattern
        // Example: Leaderboards, Settings, etc.

        // Fill remaining empty slots with placeholder items (optional)
        ItemStack placeholder = CustomInventoryManager.createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, " ");
        CustomInventoryManager.fillEmptySlots(mainMenu, placeholder);

        return mainMenu;
    }

    /**
     * Creates a player head for the specified player with a given display name.
     *
     * @param player      The player whose head will be displayed.
     * @param displayName The display name for the player head.
     * @return The player head ItemStack.
     */
    private ItemStack createPlayerHead(Player player, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(Utils.getInstance().$(displayName));
            // Embed UUID in lore for identification (optional, can be useful for event handling)
            meta.setLore(Collections.singletonList(player.getUniqueId().toString()));
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Creates a Cake item for the "Party" button with a specified display name.
     *
     * @param displayName The display name for the cake.
     * @return The Cake ItemStack.
     */
    private ItemStack createPartyCake(String displayName) {
        ItemStack cake = new ItemStack(Material.CAKE);
        ItemMeta meta = cake.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.getInstance().$(displayName));
            // Optionally, add lore or other meta data
            cake.setItemMeta(meta);
        }
        return cake;
    }
}
