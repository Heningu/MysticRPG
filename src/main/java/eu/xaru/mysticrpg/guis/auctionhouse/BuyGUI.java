package eu.xaru.mysticrpg.guis.auctionhouse;

import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.auctionhouse.AuctionsGUI;
import eu.xaru.mysticrpg.customs.items.Category;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.guis.globalbuttons.CategoryTabItem;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the Buy GUI, including displaying available auctions and category filtering using TabGui.
 */
public class BuyGUI {

    private final AuctionsGUI mainGUI;
    private AuctionHouseModule auctionHouse;
    private EquipmentModule equipmentModule;
    private LevelModule levelingModule;
    private PlayerStatModule playerStat;
    private QuestModule questModule;
    private FriendsModule friendsModule;
    private PartyModule partyModule;

    /**
     * Constructs the BuyGUI with references to various modules.
     */
    public BuyGUI(){
        this.mainGUI = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class).getAuctionsGUI();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    /**
     * Opens the Buy GUI for the specified player.
     *
     * @param player The player to open the GUI for.
     */
    public void openAuctionHouseBuyGUI(Player player){

        if (!mainGUI.getAuctionHouseHelper().areAuctionsLoaded()) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "Please wait, auctions are still loading."));
            return;
        }

        UUID playerId = player.getUniqueId();
        Category selectedCategory = mainGUI.getBuyGuiSelectedCategoryMap().get(playerId);
        if (selectedCategory == null) {
            selectedCategory = Category.EVERYTHING; // Default to EVERYTHING
            mainGUI.getBuyGuiSelectedCategoryMap().put(playerId, selectedCategory);
        }

        // Create the border item
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags()
        );

        // BACK BUTTON
        Item back = new SimpleItem(new ItemBuilder(Material.ARROW)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines(
                        "",
                        "Click to get back to the main menu.",
                        ""
                )
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true)
        )
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Main Menu
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(player);
            }
        };

        // Retrieve all active auctions
        List<Auction> allAuctions = mainGUI.getAuctionHouseHelper().getActiveAuctions();

        // Create PagedGuis for each category without pagination
        // Assuming tab indices: 0 - Everything, 1 - Weapons, 2 - Armor, 3 - Magic
        PagedGui<Item> gui1 = createPagedGuiForCategory(Category.EVERYTHING, allAuctions);
        PagedGui<Item> gui2 = createPagedGuiForCategory(Category.WEAPON, allAuctions);
        PagedGui<Item> gui3 = createPagedGuiForCategory(Category.ARMOR, allAuctions);
        PagedGui<Item> gui4 = createPagedGuiForCategory(Category.MAGIC, allAuctions);
        PagedGui<Item> gui5 = createPagedGuiForCategory(Category.CONSUMABLE, allAuctions);
        PagedGui<Item> gui6 = createPagedGuiForCategory(Category.ACCESSORY, allAuctions);
        PagedGui<Item> gui7 = createPagedGuiForCategory(Category.TOOL, allAuctions);
        PagedGui<Item> gui8 = createPagedGuiForCategory(Category.ARTIFACT, allAuctions);

        // Build the TabGui
        Gui buyGUI = TabGui.normal()
                .setStructure(
                        "0 1 2 3 4 5 6 7 8",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B # # # # # # # #")
                .addIngredient('#', border)
                .addIngredient('0', new CategoryTabItem(0,"Everything", Material.STICK))
                .addIngredient('1', new CategoryTabItem(1,"Weapons", Material.DIAMOND_SWORD))
                .addIngredient('2', new CategoryTabItem(2,"Armor", Material.DIAMOND_HELMET))
                .addIngredient('3', new CategoryTabItem(3,"Magic", Material.BLAZE_ROD))
                .addIngredient('4', new CategoryTabItem(3,"CONSUMABLE", Material.GOLDEN_APPLE))
                .addIngredient('5', new CategoryTabItem(3,"ACCESSORY", Material.HEART_OF_THE_SEA))
                .addIngredient('6', new CategoryTabItem(3,"TOOL", Material.STONE_PICKAXE))
                .addIngredient('7', new CategoryTabItem(3,"ARTIFACT", Material.NETHER_STAR))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('B', back)
                .setTabs(Arrays.asList(gui1, gui2, gui3, gui4, gui5, gui6, gui7, gui8))
                .build();

        // Create and open the Window with the TabGui
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.translateAlternateColorCodes('&', "&cAuction House - Buy"))
                .setGui(buyGUI)
                .build();

        window.open();

    }

    /**
     * Creates a PagedGui for a specific auction category.
     *
     * @param category The auction category.
     * @param auctions All active auctions.
     * @return A PagedGui populated with auctions for the category.
     */
    private PagedGui<Item> createPagedGuiForCategory(Category category, List<Auction> auctions){
        // Filter auctions based on the category
        List<Auction> filteredAuctions;
        if(category == Category.EVERYTHING){
            filteredAuctions = auctions.stream()
                    .filter(auction -> CustomItemUtils.isCustomItem(auction.getItem()))
                    .collect(Collectors.toList());
        }
        else{
            filteredAuctions = auctions.stream()
                    .filter(auction -> CustomItemUtils.getCategory(auction.getItem()) == category)
                    .collect(Collectors.toList());
        }

        // Convert auctions to Items
        List<Item> auctionItems = filteredAuctions.stream().map(auction -> {
            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            if (auction.isBidItem()) {
                lore.add(ChatColor.GRAY + "Current Bid: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getCurrentBid()));
                lore.add(Utils.getInstance().$("Right-click to place a bid"));
            } else {
                lore.add(Utils.getInstance().$("Price: $" +
                        mainGUI.getEconomyHelper().formatBalance(auction.getStartingPrice())));
                lore.add(Utils.getInstance().$("Left-click to buy now"));
            }
            lore.add(Utils.getInstance().$("Time Left: " +
                    formatTimeLeft(auction.getEndTime() - System.currentTimeMillis())));
            lore.add(Utils.getInstance().$("Auction ID: " +
                    auction.getAuctionId()));

            meta.setLore(lore);
            item.setItemMeta(meta);

            return new SimpleItem(new ItemBuilder(item)
                    .addAllItemFlags()) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                    // Handle auction item clicks
                    if (auction.isBidItem()) {
                        if (clickType.isRightClick()) {
                            player.closeInventory();
                            promptBidAmount(player, auction.getAuctionId());
                        }
                    } else {
                        if (clickType.isLeftClick()) {
                            mainGUI.getAuctionHouseHelper().buyAuction(player, auction.getAuctionId());
                            // Refresh the Buy GUI
                            Bukkit.getScheduler().runTask(mainGUI.getPlugin(), () -> openAuctionHouseBuyGUI(player));
                        }
                    }
                }
            };
        }).collect(Collectors.toList());

        // If no auctions, add a placeholder
        if(auctionItems.isEmpty()){
            auctionItems.add(new SimpleItem(new ItemBuilder(Material.BEDROCK)
                    .setDisplayName(ChatColor.RED + "No Auctions Available")));
        }

        Item controler = new ChangePageItem();



        // Create PagedGui without pagination
        PagedGui<Item> pagedGui = PagedGui.<Item>items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# > # # # # # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createPlaceholder())
                .addIngredient('>', controler)
                .setContent(auctionItems)
                .build();

     //   pagedGui.playAnimation("");

        return pagedGui;
    }

    /**
     * Creates a placeholder item for GUI borders.
     *
     * @return A SimpleItem representing the placeholder.
     */
    private Item createPlaceholder() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());
    }

    /**
     * Prompts the player to enter a bid amount via chat.
     *
     * @param player    The player placing the bid.
     * @param auctionId The auction ID to bid on.
     */
    private void promptBidAmount(Player player, UUID auctionId){
        // Add the player to a pending bids map before closing inventory
        mainGUI.getPendingBids().put(player.getUniqueId(), auctionId);

        // Prompt the player to enter a bid amount via chat
        player.sendMessage(Utils.getInstance().$(ChatColor.GREEN + "Enter your bid amount in chat:"));
    }

    /**
     * Formats the remaining time into a human-readable string.
     *
     * @param millis The time in milliseconds.
     * @return A formatted string representing the time left.
     */
    private String formatTimeLeft(long millis){
        if(millis <0){
            return ChatColor.RED + "Expired";
        }
        long seconds = millis / 1000 %60;
        long minutes = millis / (1000*60)%60;
        long hours = millis / (1000*60*60)%24;
        long days = millis / (1000*60*60*24);

        StringBuilder sb = new StringBuilder();
        if(days >0) sb.append(days).append("d ");
        if(hours >0 || days >0) sb.append(hours).append("h ");
        if(minutes >0 || hours >0 || days >0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    /**
     * Handles pagination controls - Left-click to go back, Right-click to go forward.
     * Currently, pagination isn't implemented, so these actions will notify the player.
     */
    public class ChangePageItem extends ControlItem<PagedGui<?>> {

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.LEFT) {
                getGui().goBack();
            } else if (clickType == ClickType.RIGHT) {
                getGui().goForward();
            }
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch pages")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " from " + (gui.getPageAmount()) + " pages",
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addEnchantment(Enchantment.UNBREAKING,1,true)
                    .addAllItemFlags();
        }
    }
}
