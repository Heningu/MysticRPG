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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

public class CraftingHelper implements Listener {

    private final JavaPlugin plugin;
    
    private final ItemManager itemManager;
    private final Map<String, CustomRecipe> customRecipes = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public CraftingHelper(JavaPlugin plugin) {
        this.plugin = plugin;
 

        // Get ItemManager from CustomItemModule
        CustomItemModule customItemModule = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        if (customItemModule != null) {
            this.itemManager = customItemModule.getItemManager();
        } else {
            this.itemManager = null;
            DebugLogger.getInstance().log(Level.WARNING, "CustomItemModule not found. Custom items in recipes won't be available.", 0);
        }

        loadCustomRecipes();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadCustomRecipes() {
        File recipesFolder = new File(plugin.getDataFolder(), "custom/recipes");
        if (!recipesFolder.exists() && !recipesFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create recipes folder.");
            return;
        }

        File[] files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    String id = config.getString("id");
                    if (id == null || id.isEmpty()) {
                        DebugLogger.getInstance().error("Recipe ID is missing in file: " + file.getName());
                        continue;
                    }

                    List<String> shape = config.getStringList("shape");
                    if (shape.size() != 3) {
                        DebugLogger.getInstance().error("Invalid shape in recipe " + id + ". Must have 3 rows.");
                        continue;
                    }

                    Map<Character, RecipeIngredient> ingredients = new HashMap<>();
                    ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
                    if (ingredientsSection != null) {
                        for (String key : ingredientsSection.getKeys(false)) {
                            String value = ingredientsSection.getString(key);
                            RecipeIngredient ingredient = parseIngredient(value);
                            if (ingredient != null) {
                                ingredients.put(key.charAt(0), ingredient);
                            } else {
                                DebugLogger.getInstance().error("Invalid ingredient for key '" + key + "' in recipe " + id);
                            }
                        }
                    }

                    String resultValue = config.getString("result");
                    RecipeIngredient resultIngredient = parseIngredient(resultValue);
                    if (resultIngredient == null) {
                        DebugLogger.getInstance().error("Invalid result in recipe " + id);
                        continue;
                    }

                    CustomRecipe customRecipe = new CustomRecipe(id, shape, ingredients, resultIngredient);
                    customRecipes.put(id, customRecipe);

                    DebugLogger.getInstance().log(Level.INFO, "Loaded custom recipe: " + id, 0);

                } catch (Exception e) {
                    DebugLogger.getInstance().error("Failed to load recipe from file " + file.getName() + ":", e);
                }
            }
        }
    }

    private RecipeIngredient parseIngredient(String value) {
        if (value == null || value.isEmpty()) return null;

        if (value.startsWith("custom:")) {
            String customItemId = value.substring(7);
            if (itemManager != null) {
                CustomItem customItem = itemManager.getCustomItem(customItemId);
                if (customItem != null) {
                    return new RecipeIngredient(customItem.toItemStack());
                }
            }
            DebugLogger.getInstance().error("Custom item not found: " + customItemId);
            return null;
        } else {
            Material material = Material.matchMaterial(value.toUpperCase());
            if (material != null) {
                return new RecipeIngredient(new ItemStack(material));
            }
            DebugLogger.getInstance().error("Invalid material: " + value);
            return null;
        }
    }

    // Handle player interacting with crafting table
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.CRAFTING_TABLE) {
                // Check if the action is a right-click
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    openCraftingGUI(event.getPlayer());
                } else {
                    // Allow breaking the crafting table
                    event.setCancelled(false);
                }
            }
        }
    }

    public Set<String> getRecipeIds() {
        return customRecipes.keySet();
    }

    void openCraftingGUI(Player player) {
        Inventory gui = CustomInventoryManager.createInventory(54, ChatColor.DARK_PURPLE + "Custom Crafting");
        // Create filler item
        ItemStack filler = CustomInventoryManager.createPlaceholder(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill the entire inventory with the filler
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Decorative items in the first row
        // Middle slot (slot 4)
        ItemStack craftingTableItem = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta craftingTableMeta = craftingTableItem.getItemMeta();
        if (craftingTableMeta != null) {
            craftingTableMeta.setDisplayName(ChatColor.GOLD + "Crafting land");
            craftingTableMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Here you can craft all your unlocked recipes and more"));
            craftingTableItem.setItemMeta(craftingTableMeta);
        }
        gui.setItem(4, craftingTableItem);

        // Nether stars in slots 2 and 6
        ItemStack netherStarItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta netherStarMeta = netherStarItem.getItemMeta();
        if (netherStarMeta != null) {
            netherStarMeta.setDisplayName(ChatColor.AQUA + "✯");
            netherStarItem.setItemMeta(netherStarMeta);
        }
        gui.setItem(2, netherStarItem);
        gui.setItem(6, netherStarItem);

        // Define crafting grid slots (we'll use slots 20-22, 29-31, 38-40 for a centered 3x3 grid)
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };

        // Clear the crafting grid slots
        for (int slot : craftingGridSlots) {
            gui.setItem(slot, null); // Empty slot
        }

        // Move the result slot one row down to center it with the crafting grid
        // New result slot at position 33
        int resultSlot = 33;
        gui.setItem(resultSlot, null); // No placeholder

        // Store the open inventory
        openInventories.put(player.getUniqueId(), gui);

        // Open the GUI for the player
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isCraftingGUI(event.getView())) return;

        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        int slot = event.getRawSlot();

        // Define crafting grid slots and result slot
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };
        int resultSlot = 33; // Updated result slot position

        if (slot >= 0 && slot < gui.getSize()) {
            // Clicked inside the GUI
            if (isInArray(slot, craftingGridSlots)) {
                // Clicked in the crafting grid
                event.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
            } else if (slot == resultSlot) {
                // Clicked on the result slot
                event.setCancelled(true); // Prevent placing items in the result slot

                ItemStack resultItem = gui.getItem(resultSlot);
                if (resultItem != null && resultItem.getType() != Material.AIR) {
                    // Give the item to the player
                    HashMap<Integer, ItemStack> excess = player.getInventory().addItem(resultItem.clone());
                    if (!excess.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), resultItem);
                    }
                    // Consume the ingredients
                    consumeIngredients(gui, player);
                    // Update the crafting result
                    Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
                }
            } else {
                // Clicked elsewhere in the GUI, do nothing
                event.setCancelled(true);
            }
        } else {
            // Clicked in the player inventory, allow default behavior
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isCraftingGUI(event.getView())) return;

        Inventory gui = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        // Define crafting grid slots
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };

        boolean updateNeeded = false;

        // Check if any of the slots being dragged into are in the crafting grid
        for (int slot : event.getRawSlots()) {
            if (isInArray(slot, craftingGridSlots)) {
                updateNeeded = true;
                break;
            }
        }

        if (updateNeeded) {
            // Allow the drag event to proceed
            event.setCancelled(false);

            // Schedule an update to the crafting result after the drag completes
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateCraftingResult(gui, player), 1);
        } else {
            // Prevent dragging into other slots in the GUI
            event.setCancelled(true);
        }
    }

    private void consumeIngredients(Inventory gui, Player player) {
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };
        for (int slot : craftingGridSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    gui.setItem(slot, null);
                } else {
                    gui.setItem(slot, item);
                }
            }
        }
        player.updateInventory();
    }

    private void updateCraftingResult(Inventory gui, Player player) {
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < craftingGridSlots.length; i++) {
            ItemStack item = gui.getItem(craftingGridSlots[i]);
            matrix[i] = item != null ? item.clone() : null;
        }

        CustomRecipe matchedRecipe = matchRecipe(matrix, player);

        int resultSlot = 33; // Updated result slot position

        if (matchedRecipe != null) {
            ItemStack resultItem = matchedRecipe.getResult().getItemStack().clone();
            gui.setItem(resultSlot, resultItem);
        } else {
            gui.setItem(resultSlot, null); // Set to null when no result
        }
    }

    private boolean isInArray(int value, int[] array) {
        for (int v : array) {
            if (v == value) return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isCraftingGUI(event.getView())) return;

        HumanEntity player = event.getPlayer();
        Inventory gui = event.getInventory();

        // Return items in crafting grid to player
        int[] craftingGridSlots = {
                20, 21, 22,
                29, 30, 31,
                38, 39, 40
        };
        for (int slot : craftingGridSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }

        openInventories.remove(player.getUniqueId());
    }

    private boolean isCraftingGUI(InventoryView view) {
        return view.getTitle().equals(ChatColor.DARK_PURPLE + "Custom Crafting");
    }

    private CustomRecipe matchRecipe(ItemStack[] matrix, Player player) {
        PlayerData playerData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return null;

        Map<String, Boolean> unlockedRecipes = playerData.getUnlockedRecipes();

        for (CustomRecipe recipe : customRecipes.values()) {
            if (recipe.matches(matrix)) {
                // Check if player has unlocked the recipe
                if (unlockedRecipes != null && unlockedRecipes.getOrDefault(recipe.getId(), false)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    public boolean unlockRecipe(Player player, String recipeId) {
        CustomRecipe recipe = customRecipes.get(recipeId);
        if (recipe == null) return false;

        PlayerData playerData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return false;

        Map<String, Boolean> unlockedRecipes = playerData.getUnlockedRecipes();
        if (unlockedRecipes == null) {
            unlockedRecipes = new HashMap<>();
            playerData.setUnlockedRecipes(unlockedRecipes);
        }

        unlockedRecipes.put(recipeId, true);

        // Save the updated player data to the database
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Optionally log success
                DebugLogger.getInstance().log(Level.INFO, "Recipe " + recipeId + " unlocked and saved for player " + player.getName(), 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // Log failure
                DebugLogger.getInstance().error("Failed to save unlocked recipe " + recipeId + " for player " + player.getName() + ": ", throwable);
            }
        });

        return true;
    }


    public boolean lockRecipe(Player player, String recipeId) {
        CustomRecipe recipe = customRecipes.get(recipeId);
        if (recipe == null) return false;

        PlayerData playerData = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return false;

        Map<String, Boolean> unlockedRecipes = playerData.getUnlockedRecipes();
        if (unlockedRecipes != null) {
            unlockedRecipes.remove(recipeId);

            // Save the updated player data to the database
            PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Optionally log success
                    DebugLogger.getInstance().log(Level.INFO, "Recipe " + recipeId + " locked and saved for player " + player.getName(), 0);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    // Log failure
                    DebugLogger.getInstance().error("Failed to save locked recipe " + recipeId + " for player " + player.getName() + ": ", throwable);
                }
            });

            return true;
        }

        return false;
    }

}
