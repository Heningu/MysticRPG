package eu.xaru.mysticrpg.admin;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * Utility methods for saving/loading player inventories using YamlConfiguration.
 */
public class InventoryUtils {

    /**
     * Saves a player's inventory (main + armor) to the specified file using YamlConfiguration.
     *
     * @param player The player whose inventory to save.
     * @param file   The target .yml file (e.g., /plugins/MysticRPG/admin/player_inventories/SomePlayer.yml).
     */
    public static void saveInventoryToFile(Player player, File file) {
        try {
            // Ensure parent folders exist
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Failed to prepare inventory file: " + file.getName(), e);
            return;
        }

        // Create and load the configuration
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            // If loading fails, we'll just overwrite it
            DebugLogger.getInstance().log(Level.WARNING,
                    "Could not load existing inventory data from " + file.getName() + ", will overwrite.", e);
        }

        // Grab the inventory contents
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // Store them under "inventory.contents" and "inventory.armor"
        config.set("inventory.contents", contents);
        config.set("inventory.armor", armor);

        // Save to disk
        try {
            config.save(file);
            DebugLogger.getInstance().log(Level.INFO,
                    "Saved inventory for player " + player.getName() + " to " + file.getName());
        } catch (IOException e) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Failed to save inventory data to " + file.getName(), e);
        }
    }

    /**
     * Loads a player's inventory from the specified file using YamlConfiguration.
     *
     * @param player The player whose inventory to load.
     * @param file   The .yml file containing the saved inventory.
     */
    @SuppressWarnings("unchecked")
    public static void loadInventoryFromFile(Player player, File file) {
        if (!file.exists()) {
            DebugLogger.getInstance().log(Level.INFO,
                    "Inventory file not found: " + file.getAbsolutePath());
            return;
        }

        // Create and load the config
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            DebugLogger.getInstance().log(Level.SEVERE,
                    "Could not load inventory data from " + file.getName(), e);
            return;
        }

        // Retrieve lists from the config
        List<ItemStack> contentList = (List<ItemStack>) config.getList("inventory.contents");
        List<ItemStack> armorList   = (List<ItemStack>) config.getList("inventory.armor");

        // If they're not null/empty, apply to player's inventory
        if (contentList != null && !contentList.isEmpty()) {
            player.getInventory().setContents(contentList.toArray(new ItemStack[0]));
        }
        if (armorList != null && !armorList.isEmpty()) {
            player.getInventory().setArmorContents(armorList.toArray(new ItemStack[0]));
        }

        player.updateInventory();
        DebugLogger.getInstance().log(Level.INFO,
                "Loaded inventory for player " + player.getName() + " from " + file.getName());
    }

    /**
     * (Optional) If you need a path key generator, adapt or remove this method.
     */
    private static String getRelativeOrName(File file) {
        String parent = file.getParentFile() != null ? file.getParentFile().getName() : "";
        return parent + "/" + file.getName();
    }
}
