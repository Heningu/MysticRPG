package eu.xaru.mysticrpg.customs.crafting;

import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles custom crafting logic and opening a custom crafting GUI.
 */
public class CraftingHelper implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Map<String, CustomRecipe> customRecipes = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public CraftingHelper(JavaPlugin plugin) {
        this.plugin = plugin;

        // Retrieve ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule != null) {
            this.itemManager = customItemModule.getItemManager();
        } else {
            this.itemManager = null;
            DebugLogger.getInstance().log(Level.WARNING,
                    "CustomItemModule not found. Custom items in recipes won't be available.", 0);
        }

        // Load recipe files (now using YamlConfiguration)
        loadCustomRecipes();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Loads all custom recipe .yml files from /custom/recipes using YamlConfiguration.
     */
    public void loadCustomRecipes() {
        File recipesFolder = new File(plugin.getDataFolder(), "custom/recipes");
        if (!recipesFolder.exists() && !recipesFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create recipes folder.");
            return;
        }

        File[] files = recipesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                // Create a YamlConfiguration and load this file
                YamlConfiguration ycfg = new YamlConfiguration();
                ycfg.load(file);

                // Extract data from ycfg
                String id = ycfg.getString("id", "");
                if (id.isEmpty()) {
                    DebugLogger.getInstance().error("Recipe ID is missing in file: " + file.getName());
                    continue;
                }

                // shape must be exactly 3 rows
                List<String> shape = ycfg.getStringList("shape");
                if (shape.size() != 3) {
                    DebugLogger.getInstance().error("Invalid shape in recipe " + id + ". Must have 3 rows.");
                    continue;
                }

                Map<Character, RecipeIngredient> ingredients = new HashMap<>();
                // "ingredients" is a sub-map of key->value (like 'A': 'custom:something')
                Object ingrObj = ycfg.get("ingredients");
                if (ingrObj instanceof Map<?, ?> ingrMap) {
                    for (Map.Entry<?, ?> e : ingrMap.entrySet()) {
                        String keyStr = String.valueOf(e.getKey());
                        if (keyStr.length() != 1) {
                            DebugLogger.getInstance().error("Ingredient key '" + keyStr + "' must be a single character.");
                            continue;
                        }
                        char c = keyStr.charAt(0);
                        String value = String.valueOf(e.getValue());
                        RecipeIngredient ingredient = parseIngredient(value);
                        if (ingredient == null) {
                            DebugLogger.getInstance().error("Invalid ingredient for key '" + keyStr + "' in recipe " + id);
                        } else {
                            ingredients.put(c, ingredient);
                        }
                    }
                }

                String resultValue = ycfg.getString("result", "");
                RecipeIngredient resultIngredient = parseIngredient(resultValue);
                if (resultIngredient == null) {
                    DebugLogger.getInstance().error("Invalid result in recipe " + id);
                    continue;
                }

                // Build and store the recipe
                CustomRecipe customRecipe = new CustomRecipe(id, shape, ingredients, resultIngredient);
                customRecipes.put(id, customRecipe);

                DebugLogger.getInstance().log(Level.INFO, "Loaded custom recipe: " + id, 0);

            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load recipe from file " + file.getName() + ":", e);
            }
        }
    }

    /**
     * Convert a string reference into a RecipeIngredient (custom item or material).
     */
    private RecipeIngredient parseIngredient(String value) {
        if (value == null || value.isEmpty()) return null;

        // If it begins with "custom:", we look for a custom item
        if (value.startsWith("custom:")) {
            String customItemId = value.substring(7).trim();
            if (itemManager != null) {
                CustomItem customItem = itemManager.getCustomItem(customItemId);
                if (customItem != null) {
                    return new RecipeIngredient(customItem.toItemStack());
                }
            }
            DebugLogger.getInstance().error("Custom item not found: " + customItemId);
            return null;
        } else {
            // Otherwise treat it as a material
            Material material = Material.matchMaterial(value.toUpperCase());
            if (material != null) {
                return new RecipeIngredient(new ItemStack(material));
            }
            DebugLogger.getInstance().error("Invalid material: " + value);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Handling custom crafting UI & logic
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.CRAFTING_TABLE) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    openCraftingGUI(event.getPlayer());
                } else {
                    event.setCancelled(false);
                }
            }
        }
    }

    void openCraftingGUI(Player player) {
        Inventory gui = CustomInventoryManager.createInventory(54, ChatColor.DARK_PURPLE + "Custom Crafting");

        // Create filler item
        ItemStack filler = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill entire inventory with placeholders
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // Some decorative items in top row
        ItemStack tableIcon = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta tableMeta = tableIcon.getItemMeta();
        if (tableMeta != null) {
            tableMeta.setDisplayName(ChatColor.GOLD + "Crafting Land");
            tableMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Craft your unlocked recipes here!"));
            tableIcon.setItemMeta(tableMeta);
        }
        gui.setItem(4, tableIcon);

        // Nether stars in slots 2 & 6 for decoration
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = netherStar.getItemMeta();
        if (starMeta != null) {
            starMeta.setDisplayName(ChatColor.AQUA + "✯");
            netherStar.setItemMeta(starMeta);
        }
        gui.setItem(2, netherStar);
        gui.setItem(6, netherStar);

        // Define a 3x3 "crafting grid" in a nice arrangement
        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
        for (int slot : craftingGridSlots) {
            gui.setItem(slot, null); // Clear
        }

        // Define a "result slot"
        int resultSlot = 33;
        gui.setItem(resultSlot, null);

        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    private boolean isCraftingGUI(InventoryView view) {
        return view.getTitle().equals(ChatColor.DARK_PURPLE + "Custom Crafting");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isCraftingGUI(event.getView())) return;

        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        int slot = event.getRawSlot();

        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
        int resultSlot = 33;

        if (slot >= 0 && slot < gui.getSize()) {
            if (isInArray(slot, craftingGridSlots)) {
                // Let them place/pick items from the crafting area
                event.setCancelled(false);
                // Update the result after a tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
            } else if (slot == resultSlot) {
                // Attempt to craft
                event.setCancelled(true);
                ItemStack resultItem = gui.getItem(resultSlot);
                if (resultItem != null && resultItem.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(resultItem.clone());
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), resultItem);
                    }
                    consumeIngredients(gui, player);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
                }
            } else {
                // Clicked outside crafting or result => block it
                event.setCancelled(true);
            }
        } else {
            // Clicked in player's own inventory
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isCraftingGUI(event.getView())) return;

        Inventory gui = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};

        boolean updateNeeded = false;
        for (int slot : event.getRawSlots()) {
            if (isInArray(slot, craftingGridSlots)) {
                updateNeeded = true;
                break;
            }
        }

        if (updateNeeded) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isCraftingGUI(event.getView())) return;
        HumanEntity player = event.getPlayer();
        Inventory gui = event.getInventory();

        // Return any items in the 3x3 grid to the player's inventory
        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
        for (int slot : craftingGridSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
        openInventories.remove(player.getUniqueId());
    }

    private CustomRecipe matchRecipe(ItemStack[] matrix, Player player) {
        // Check if player has unlocked recipes
        PlayerData pData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (pData == null) return null;

        Map<String, Boolean> unlocked = pData.getUnlockedRecipes();
        if (unlocked == null) unlocked = new HashMap<>();

        for (CustomRecipe recipe : customRecipes.values()) {
            if (recipe.matches(matrix)) {
                if (unlocked.getOrDefault(recipe.getId(), false)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private void updateCraftingResult(Inventory gui, Player player) {
        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < craftingGridSlots.length; i++) {
            ItemStack slotItem = gui.getItem(craftingGridSlots[i]);
            matrix[i] = (slotItem != null) ? slotItem.clone() : null;
        }

        CustomRecipe recipe = matchRecipe(matrix, player);
        int resultSlot = 33;

        if (recipe != null) {
            gui.setItem(resultSlot, recipe.getResult().getItemStack().clone());
        } else {
            gui.setItem(resultSlot, null);
        }
    }

    private void consumeIngredients(Inventory gui, Player player) {
        int[] craftingGridSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
        for (int slot : craftingGridSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                int newAmt = item.getAmount() - 1;
                if (newAmt <= 0) {
                    gui.setItem(slot, null);
                } else {
                    item.setAmount(newAmt);
                    gui.setItem(slot, item);
                }
            }
        }
        player.updateInventory();
    }

    private boolean isInArray(int val, int[] array) {
        for (int i : array) {
            if (i == val) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Additional Methods for unlocking/locking recipes
    // -------------------------------------------------------------------------

    public Set<String> getRecipeIds() {
        return customRecipes.keySet();
    }

    public boolean unlockRecipe(Player player, String recipeId) {
        CustomRecipe recipe = customRecipes.get(recipeId);
        if (recipe == null) return false;

        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return false;

        Map<String, Boolean> unlocked = data.getUnlockedRecipes();
        if (unlocked == null) {
            unlocked = new HashMap<>();
            data.setUnlockedRecipes(unlocked);
        }
        unlocked.put(recipeId, true);

        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                DebugLogger.getInstance().log(Level.INFO, "Recipe " + recipeId
                        + " unlocked and saved for " + player.getName(), 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().error("Failed to save unlocked recipe "
                        + recipeId + " for " + player.getName() + ": ", throwable);
            }
        });
        return true;
    }

    public boolean lockRecipe(Player player, String recipeId) {
        CustomRecipe recipe = customRecipes.get(recipeId);
        if (recipe == null) return false;

        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return false;

        Map<String, Boolean> unlocked = data.getUnlockedRecipes();
        if (unlocked != null) {
            unlocked.remove(recipeId);

            PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "Recipe " + recipeId
                            + " locked and saved for " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().error("Failed to save locked recipe "
                            + recipeId + " for " + player.getName() + ": ", throwable);
                }
            });
            return true;
        }
        return false;
    }
}
