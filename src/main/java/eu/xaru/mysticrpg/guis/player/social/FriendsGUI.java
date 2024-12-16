package eu.xaru.mysticrpg.guis.player.social;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;





/**
 * Utility class for creating and managing the Friends GUI using a PagedGui.
 */
public class FriendsGUI {

    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final StatsModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;
    private final PlayerDataCache playerDataCache;



    public FriendsGUI(){
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }



    /**
     * Opens the Friends GUI for the specified player.
     *
     * @param player          The player who will see the GUI.
     */
    public void openFriendsGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("An error occurred while accessing your friend data."));
            return;
        }


        Item controler = new ChangePageItem();

        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };


        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags()
        );


        Item fq = new SimpleItem(new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(ChatColor.GREEN + "Friend Requests")
                .addLoreLines(ChatColor.GRAY + "Click to see friend requests.")
                .addAllItemFlags()
        )        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
                FriendRequestsGUI friendRequestsGUI = new FriendRequestsGUI();
                friendRequestsGUI.openFriendRequests(player);
            }
        };

        List<Item> friendItems = new ArrayList<>();

        // Create items for each friend
        for (String friendUUIDString : playerData.getFriends()) {
            try {
                UUID friendUUID = UUID.fromString(friendUUIDString);
                OfflinePlayer friend = player.getServer().getOfflinePlayer(friendUUID);

                ItemStack friendHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) friendHead.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(friend);
                    meta.setDisplayName(friend.isOnline()
                            ? ChatColor.GREEN + friend.getName()
                            : ChatColor.RED + "[Offline] " + (friend.getName() != null ? friend.getName() : "Unknown"));
                    friendHead.setItemMeta(meta);
                }

                SimpleItem friendItem = new SimpleItem(friendHead) {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                        OfflinePlayer target = friend; // Pass the friend object
                        new FriendOptionsGUI(target).openFriendOptionsGUI(clickPlayer); // Open FriendOptionsGUI with the target player
                    }
                };
                friendItems.add(friendItem);
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }

        // Add a default "no friends" item if the list is empty
        if (friendItems.isEmpty()) {
            friendItems.add(new SimpleItem(
                    new ItemBuilder(Material.BARRIER)
                            .setDisplayName("No Friends")
            ));
        }

        // Create the paged GUI
        Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "X F > # # # # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', border)
                .addIngredient('X', back)
                .addIngredient('>', controler)
                .addIngredient('F', fq)
                .setContent(friendItems)
                .build();

        // Open the GUI in a window
        Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(Utils.getInstance().$("Friends"))
                .open(player);
    }

    public static class ChangePageItem extends ControlItem<PagedGui<?>> {
        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.RIGHT) {
                getGui().goForward();
            } else if (clickType == ClickType.LEFT) {
                getGui().goBack();
            }
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch Pages")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " of " + gui.getPageAmount(),
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addEnchantment(Enchantment.UNBREAKING, 1, true)
                    .addAllItemFlags();
        }
    }
}
