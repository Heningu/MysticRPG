package eu.xaru.mysticrpg.guis.player.social;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.social.friends.FriendsHelper;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public class FriendRequestsGUI {

    private final PlayerDataCache playerDataCache;

    private FriendsHelper friendsHelper;


    public FriendRequestsGUI() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.friendsHelper = new FriendsHelper(playerDataCache);

    }

    public void openFriendRequests(Player player) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("An error occurred while accessing your friend requests."));
            return;
        }

        List<Item> friendRequestItems = new ArrayList<>();

        // Create items for each friend request
        for (String requestUUIDString : playerData.getFriendRequests()) {
            try {
                UUID requestUUID = UUID.fromString(requestUUIDString);
                OfflinePlayer requester = player.getServer().getOfflinePlayer(requestUUID);
                String requesterName = requester.getName(); // Extract player name



                ItemStack requesterHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) requesterHead.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(requester);
                    meta.setDisplayName(ChatColor.YELLOW + requester.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Left-click to accept");
                    lore.add(ChatColor.RED + "Right-click to deny");
                    meta.setLore(lore);
                    requesterHead.setItemMeta(meta);
                }

                // Create SimpleItem with click handling
                SimpleItem requestItem = new SimpleItem(requesterHead) {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                        if (clickType == ClickType.LEFT) {
                            // Accept friend request
                            friendsHelper.acceptFriendRequest(player, requesterName);
                        } else if (clickType == ClickType.RIGHT) {
                            // Decline friend request
                            friendsHelper.denyFriendRequest(player, requesterName);
                        }
                        // Refresh the GUI
                        openFriendRequests(player);
                    }
                };

                friendRequestItems.add(requestItem);
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }

        // Add a default "no requests" item if the list is empty
        if (friendRequestItems.isEmpty()) {
            friendRequestItems.add(new SimpleItem(
                    new ItemBuilder(Material.BARRIER)
                            .setDisplayName(ChatColor.RED + "No Friend Requests")
            ));
        }

        // Create GUI structure
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to go back to the Friends GUI.", "")
                .addAllItemFlags()
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                new FriendsGUI().openFriendsGUI(player); // Navigate back to FriendsGUI
            }
        };

        Item controler = new ChangePageItem();

        // Build the GUI
        Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "X > # # # # # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', border)
                .addIngredient('X', back)
                .addIngredient('>', controler)
                .setContent(friendRequestItems)
                .build();

        // Open the GUI
        Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(Utils.getInstance().$("Friend Requests"))
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
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " of " + gui.getPageAmount(),
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addAllItemFlags();
        }
    }
}
