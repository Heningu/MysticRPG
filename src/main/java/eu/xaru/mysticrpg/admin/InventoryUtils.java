package eu.xaru.mysticrpg.admin;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * Utility methods for saving/loading player inventories using the DynamicConfig system.
 */
public class InventoryUtils {

    /**
     * Saves a player's inventory (main + armor) to the specified file using DynamicConfig.
     *
     * @param player The player whose inventory to save.
     * @param file   The target .yml file (e.g., /plugins/MysticRPG/admin/player_inventories/SomePlayer.yml).
     */
    public static void saveInventoryToFile(Player player, File file) {
        try {
            // Step 1: Create parent folders if they do not exist
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            // If the file doesn't exist yet, create it so DynamicConfig has something to load
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            DebugLogger.getInstance().log(Level.SEVERE, "Failed to prepare inventory file: " + file.getName(), e);
            return;
        }

        // Build a unique path key for DynamicConfig
        // For example: "admin/player_inventories/<filename>.yml"
        String pathKey = getRelativeOrName(file);

        // Step 2: Load (or reload) this config into the manager
        DynamicConfigManager.loadConfig(pathKey, pathKey);
        DynamicConfig config = DynamicConfigManager.getConfig(pathKey);
        if (config == null) {
            DebugLogger.getInstance().log(Level.SEVERE, "Could not load DynamicConfig for " + pathKey);
            return;
        }

        // Step 3: Retrieve and store inventory contents
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // We store them as a List<ItemStack> so DynamicConfig can handle them easily
        config.set("inventory.contents", contents);
        config.set("inventory.armor", armor);

        // Step 4: Save changes if needed
        config.saveIfNeeded();

        DebugLogger.getInstance().log(Level.INFO,
                "Saved inventory for player " + player.getName() + " to " + file.getName());
    }

    /**
     * Loads a player's inventory from the specified file using DynamicConfig.
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

        // Build the same path key used when saving
        String pathKey = getRelativeOrName(file);

        // Load from DynamicConfig
        DynamicConfigManager.loadConfig(pathKey, pathKey);
        DynamicConfig config = DynamicConfigManager.getConfig(pathKey);
        if (config == null) {
            DebugLogger.getInstance().log(Level.WARNING,
                    "Could not load DynamicConfig for " + pathKey);
            return;
        }

        // Retrieve the lists from config
        List<ItemStack> contentList = (List<ItemStack>) config.getList("inventory.contents", null);
        List<ItemStack> armorList   = (List<ItemStack>) config.getList("inventory.armor", null);

        // If they're null or empty, there's nothing to load
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
     * A helper method that decides how to build the path key for DynamicConfig based on a file.
     * Adjust if you want a different mapping scheme.
     */
    private static String getRelativeOrName(File file) {
        // Option A: Use the absolute path as the load key
        // return file.getAbsolutePath().replace("\\", "/");

        // Option B: Use some relative path under your plugin's data folder
        // e.g. "admin/player_inventories/SomePlayer.yml"
        // For simplicity, let's just combine the parent folder name + file name:
        String parent = file.getParentFile().getName();
        return parent + "/" + file.getName();
    }
}
