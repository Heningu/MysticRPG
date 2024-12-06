package eu.xaru.mysticrpg.auctionhouse;

import eu.xaru.mysticrpg.customs.items.Category;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.utils.PaginationHelper;
import eu.xaru.mysticrpg.utils.Utils;
import eu.xaru.mysticrpg.auctionhouse.guis.BuyGUI;
import eu.xaru.mysticrpg.auctionhouse.guis.SellGUI;
import eu.xaru.mysticrpg.auctionhouse.guis.YourAuctionsGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the Auction House GUI, including displaying available auctions,
 * allowing players to buy, sell, and manage their own auctions with pagination support.
 */
public class AuctionsGUI {

    private final AuctionHouseHelper auctionHouseHelper;
    private final EconomyHelper economyHelper;
    private final JavaPlugin plugin;
    private static final Logger logger = Logger.getLogger(AuctionsGUI.class.getName());

    private final Map<UUID, Category> buyGuiSelectedCategoryMap = new HashMap<>();

    // Constants for GUI slot indices
    private static final int MAIN_GUI_BUY_SLOT = 20;
    private static final int MAIN_GUI_SELL_SLOT = 22;
    private static final int MAIN_GUI_MY_AUCTIONS_SLOT = 24;

    // Temporary storage for price and duration settings per player
    private final Map<UUID, Double> priceMap = new HashMap<>();
    private final Map<UUID, Long> durationMap = new HashMap<>();
    private final Map<UUID, Boolean> bidMap = new HashMap<>();

    // Maps to store pending actions for players
    private final Map<UUID, UUID> pendingBids = new HashMap<>();
    private final Set<UUID> pendingPriceInput = new HashSet<>();

    // Map to store the item the player is selling
    private final Map<UUID, ItemStack> sellingItems = new HashMap<>();

    // Maps to store PaginationHelpers per player
    private final Map<UUID, Map<Category, PaginationHelper<ItemStack>>> buyPaginationMap = new ConcurrentHashMap<>();
    private final Map<UUID, PaginationHelper<ItemStack>> yourAuctionsPaginationMap = new HashMap<>();

    // Instances of sub-GUI classes
    private final BuyGUI buyGUI;
    private final SellGUI sellGUI;
    private final YourAuctionsGUI yourAuctionsGUI;

    /**
     * Constructs the AuctionsGUI with necessary dependencies.
     *
     * @param auctionHouseHelper The AuctionHouseHelper instance.
     * @param economyHelper      The EconomyHelper instance.
     * @param plugin             The main plugin instance.
     */
    public AuctionsGUI(AuctionHouseHelper auctionHouseHelper,
                       EconomyHelper economyHelper,
                       JavaPlugin plugin) {
        this.auctionHouseHelper = auctionHouseHelper;
        this.economyHelper = economyHelper;
        this.plugin = plugin;

        // Initialize sub-GUI classes
        this.buyGUI = new BuyGUI(this);
        this.sellGUI = new SellGUI(this);
        this.yourAuctionsGUI = new YourAuctionsGUI(this);
    }

    /**
     * Fills the inventory with placeholders only on the outer border.
     * This method is now public to be accessible by sub-GUI classes.
     *
     * @param inventory    The inventory to fill.
     * @param excludeSlots Set of slot indices to exclude from being filled with placeholders.
     */
    public void fillWithPlaceholdersBorderOnly(Inventory inventory, Set<Integer> excludeSlots) {
        ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Define border slots
        Set<Integer> borderSlots = new HashSet<>();

        // Top and Bottom rows
        for (int i = 0; i < 9; i++) {
            borderSlots.add(i); // Top row
            borderSlots.add(45 + i); // Bottom row
        }

        // Left and Right columns for middle rows
        for (int row = 1; row <= 4; row++) {
            borderSlots.add(row * 9); // Left column
            borderSlots.add(row * 9 + 8); // Right column
        }

        for (int i : borderSlots) {
            if (!excludeSlots.contains(i) && (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir())) {
                inventory.setItem(i, placeholder);
            }
        }
    }


    /**
     * Creates and places category items in the first row of the Buy GUI.
     * This method is now public to be accessible by sub-GUI classes.
     *
     * @param gui              The inventory GUI.
     * @param player           The player opening the GUI.
     * @param selectedCategory The currently selected category.
     */
    public void createAndPlaceCategoryItems(Inventory gui, Player player, Category selectedCategory) {
        // Slot 0: "Everything" category
        ItemStack everythingItem = createGuiItem(Material.BARRIER, ChatColor.AQUA + "Everything",
                Arrays.asList(ChatColor.GRAY + "Click to view all items"));
        if (selectedCategory == Category.EVERYTHING) { // "Everything" is selected
            CustomItemUtils.applyEnchantedEffect(everythingItem);
        }
        gui.setItem(0, everythingItem);

        // Get all categories
        Category[] categories = CustomItemUtils.getAllCategories();

        // Place each category item in slots 1-8
        for (int i = 0; i < categories.length && i < 8; i++) {
            Category category = categories[i];
            Material material = getMaterialForCategory(category);
            String displayName = ChatColor.AQUA + category.name().replace("_", " ");
            List<String> lore = Arrays.asList(ChatColor.GRAY + "Click to view " + category.name().replace("_", " ") + " items");

            ItemStack categoryItem = createGuiItem(material, displayName, lore);
            if (category == selectedCategory) {
                // Highlight the selected category
                CustomItemUtils.applyEnchantedEffect(categoryItem);
            }
            gui.setItem(i + 1, categoryItem);
        }

        // Fill remaining category slots with placeholders if necessary
        for (int i = categories.length; i < 8; i++) {
            int slot = i + 1;
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
            }
        }
    }


    /**
     * Maps each Category to a corresponding Material for display.
     *
     * @param category The category to map.
     * @return The Material representing the category.
     */
    private Material getMaterialForCategory(Category category) {
        switch (category) {
            case WEAPON:
                return Material.DIAMOND_SWORD;
            case ARMOR:
                return Material.DIAMOND_CHESTPLATE;
            case MAGIC:
                return Material.BLAZE_ROD;
            case CONSUMABLE:
                return Material.GOLDEN_APPLE;
            case ACCESSORY:
                return Material.NETHER_STAR; // Represents accessories
            case TOOL:
                return Material.IRON_PICKAXE;
            case ARTIFACT:
                return Material.EMERALD;
            case QUEST_ITEM:
                return Material.BOOK;
            case PET:
                return Material.TURTLE_EGG;
            case MOUNT:
                return Material.SADDLE;
            case EVERYTHING:
                return Material.BARRIER; // Represents "Everything"
            default:
                return Material.BARRIER; // Fallback material
        }
    }


    /**
     * Opens the main Auction House GUI for the player.
     *
     * @param player The player to open the GUI for.
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Utils.getInstance().$("Auction House"));

        // Fill the GUI with placeholders on all slots
        fillAllWithPlaceholders(gui);

        // Create the "Buy Items" item
        ItemStack buyItems = createGuiItem(Material.EMERALD, Utils.getInstance().$("Buy Items"), null);

        // Create the "Sell Items" item
        ItemStack sellItems = createGuiItem(Material.CHEST, Utils.getInstance().$("Sell Items"), null);

        // Create the "My Auctions" item
        ItemStack myAuctions = createGuiItem(Material.BOOK, Utils.getInstance().$("My Auctions"), null);

        // Place the items in the GUI
        gui.setItem(MAIN_GUI_BUY_SLOT, buyItems);
        gui.setItem(MAIN_GUI_SELL_SLOT, sellItems);
        gui.setItem(MAIN_GUI_MY_AUCTIONS_SLOT, myAuctions);

        // Remove PaginationHelpers when opening Main GUI
        removePagination(player.getUniqueId());
        logger.log(Level.INFO, "Removed PaginationHelpers for player {0}", player.getName());

        player.openInventory(gui);
    }

    /**
     * Handles clicks within the Auction House GUIs.
     *
     * @param event The InventoryClickEvent.
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        ItemStack clickedItem = event.getCurrentItem();

        switch (inventoryTitle) {
            case "Auction House":
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                handleMainGUIClick(event, player, clickedItem);
                break;
            case "Auction House - Buy":
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                buyGUI.handleBuyGUIClick(event, player, clickedItem);
                break;
            case "Auction House - Sell":
                sellGUI.handleSellGUIClick(event, player);
                break;
            case "Your Auctions":
                event.setCancelled(true);
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }
                yourAuctionsGUI.handleYourAuctionsGUIClick(event, player, clickedItem);
                break;
            default:
                break;
        }
    }

    /**
     * Handles dragging items within the Auction House GUIs.
     *
     * @param event The InventoryDragEvent.
     */
    public void handleInventoryDrag(InventoryDragEvent event) {
        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        if ("Auction House - Sell".equals(inventoryTitle)) {
            // Allow dragging only if the drag involves slot 22
            if (event.getRawSlots().contains(SellGUI.SELL_GUI_ITEM_SLOT)) {
                // Update the sellingItems map after the event has processed
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player player = (Player) event.getWhoClicked();
                    ItemStack item = event.getView().getTopInventory().getItem(SellGUI.SELL_GUI_ITEM_SLOT);
                    if (item != null && item.getType() != Material.AIR) {
                        sellingItems.put(player.getUniqueId(), item.clone());
                    } else {
                        sellingItems.remove(player.getUniqueId());
                    }
                }, 1L); // Delay by 1 tick to ensure the event has processed
            } else {
                event.setCancelled(true);
            }
        } else {
            // Cancel dragging in other Auction House GUIs
            event.setCancelled(true);
        }
    }

    /**
     * Handles player chat input for bidding and custom price setting.
     *
     * @param event The AsyncPlayerChatEvent.
     */
    public void handlePlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (pendingBids.containsKey(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            UUID auctionId = pendingBids.remove(playerId);

            try {
                double bidAmount = Double.parseDouble(message);
                auctionHouseHelper.placeBid(player, auctionId, bidAmount);
                // Reopen Buy GUI
                Bukkit.getScheduler().runTask(plugin, () -> buyGUI.openBuyGUI(player));
            } catch (NumberFormatException e) {
                player.sendMessage(Utils.getInstance().$("Invalid bid amount. Please enter a number."));
            }
        } else if (pendingPriceInput.contains(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            pendingPriceInput.remove(playerId);

            try {
                double customPrice = Double.parseDouble(message);
                if (customPrice < 0) {
                    player.sendMessage(Utils.getInstance().$("Price cannot be negative."));
                    return;
                }
                priceMap.put(playerId, customPrice);
                player.sendMessage(Utils.getInstance().$("Custom price set to $" + economyHelper.formatBalance(customPrice)));
                // Reopen Sell GUI
                Bukkit.getScheduler().runTask(plugin, () -> sellGUI.openSellGUI(player));
            } catch (NumberFormatException e) {
                player.sendMessage(Utils.getInstance().$("Invalid price. Please enter a number."));
            }
        }
    }

    /**
     * Handles clicks on the main Auction House GUI.
     *
     * @param event       The InventoryClickEvent.
     * @param player      The player who clicked.
     * @param clickedItem The item that was clicked.
     */
    private void handleMainGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        if ("Buy Items".equalsIgnoreCase(displayName)) {
            buyGUI.openBuyGUI(player);
        } else if ("Sell Items".equalsIgnoreCase(displayName)) {
            sellGUI.openSellGUI(player);
        } else if ("My Auctions".equalsIgnoreCase(displayName)) {
            yourAuctionsGUI.openPlayerAuctionsGUI(player);
        }
    }

    /**
     * Creates a generic GUI item with a display name and optional lore.
     *
     * @param material    The material of the item.
     * @param displayName The display name of the item.
     * @param lore        The lore of the item (can be null).
     * @return The customized ItemStack.
     */
    public ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        if (displayName != null) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Fills the entire inventory with black stained glass panes as placeholders.
     *
     * @param inventory The inventory to fill.
     */
    private void fillAllWithPlaceholders(Inventory inventory) {
        ItemStack placeholder = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    /**
     * Removes PaginationHelpers associated with a player.
     *
     * @param playerId The UUID of the player.
     */
    public void removePagination(UUID playerId) {
        buyPaginationMap.remove(playerId);
        yourAuctionsPaginationMap.remove(playerId);
        logger.log(Level.INFO, "Removed PaginationHelpers for player {0}", Bukkit.getPlayer(playerId).getName());
    }

    /**
     * Checks if a player is pending a custom price input.
     *
     * @param playerId The UUID of the player.
     * @return True if the player is pending a custom price input, false otherwise.
     */
    public boolean isPendingPriceInput(UUID playerId) {
        return pendingPriceInput.contains(playerId);
    }

    /**
     * Removes the selling item associated with a player.
     *
     * @param playerId The UUID of the player.
     */
    public void removeSellingItem(UUID playerId) {
        sellingItems.remove(playerId);
    }

    /**
     * Retrieves or creates a PaginationHelper for the specified player and category.
     *
     * @param player       The player for whom the PaginationHelper is needed.
     * @param category     The selected category.
     * @param auctionItems The list of auction items to paginate.
     * @return The PaginationHelper instance for the current page.
     */
    public PaginationHelper<ItemStack> getBuyGuiPaginationHelper(Player player, Category category, List<ItemStack> auctionItems) {
        UUID playerUUID = player.getUniqueId();
        Map<Category, PaginationHelper<ItemStack>> playerPaginationMap = buyPaginationMap.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());

        PaginationHelper<ItemStack> paginationHelper = playerPaginationMap.get(category);
        if (paginationHelper == null) {
            paginationHelper = new PaginationHelper<>(auctionItems, 28);
            playerPaginationMap.put(category, paginationHelper);
            logger.log(Level.INFO, "Created new PaginationHelper for player {0} in category {1}. Total pages: {2}",
                    new Object[]{player.getName(), category.name(), paginationHelper.getTotalPages()});
        } else {
            paginationHelper.updateItems(auctionItems);
            // Removed the following line to prevent resetting the page to 1
            // paginationHelper.setCurrentPage(1); // Reset to first page upon update
            logger.log(Level.INFO, "Updated PaginationHelper for player {0} in category {1}.",
                    new Object[]{player.getName(), category.name()});
        }

        logger.log(Level.INFO, "PaginationHelper state for player {0} in category {1}: Current Page {2} / Total Pages {3}",
                new Object[]{player.getName(), category.name(), paginationHelper.getCurrentPage(), paginationHelper.getTotalPages()});

        return paginationHelper;
    }

    /**
     * Retrieves an existing PaginationHelper for the specified player and category without modifying it.
     *
     * @param player   The player for whom the PaginationHelper is needed.
     * @param category The selected category.
     * @return The existing PaginationHelper instance, or null if none exists.
     */
    public PaginationHelper<ItemStack> getExistingBuyGuiPaginationHelper(Player player, Category category) {
        UUID playerUUID = player.getUniqueId();
        Map<Category, PaginationHelper<ItemStack>> playerPaginationMap = buyPaginationMap.get(playerUUID);
        if (playerPaginationMap != null) {
            return playerPaginationMap.get(category);
        }
        return null;
    }

    // Getter methods for accessing maps and helpers from sub-GUI classes
    public AuctionHouseHelper getAuctionHouseHelper() {
        return auctionHouseHelper;
    }

    public EconomyHelper getEconomyHelper() {
        return economyHelper;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Map<UUID, Category> getBuyGuiSelectedCategoryMap() {
        return buyGuiSelectedCategoryMap;
    }

    public Map<UUID, Double> getPriceMap() {
        return priceMap;
    }

    public Map<UUID, Long> getDurationMap() {
        return durationMap;
    }

    public Map<UUID, Boolean> getBidMap() {
        return bidMap;
    }

    public Map<UUID, UUID> getPendingBids() {
        return pendingBids;
    }

    public Set<UUID> getPendingPriceInput() {
        return pendingPriceInput;
    }

    public Map<UUID, ItemStack> getSellingItems() {
        return sellingItems;
    }

    public Map<UUID, Map<Category, PaginationHelper<ItemStack>>> getBuyPaginationMap() {
        return buyPaginationMap;
    }

    public Map<UUID, PaginationHelper<ItemStack>> getYourAuctionsPaginationMap() {
        return yourAuctionsPaginationMap;
    }
}
