package eu.xaru.mysticrpg.guis.player.social;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.social.friends.FriendsHelper;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class FriendOptionsGUI {

    private final PlayerDataCache playerDataCache;
    private final FriendsHelper friendsHelper;
    private final PartyModule partyModule;
    private final OfflinePlayer target; // The target player for the Friend Options GUI

    public FriendOptionsGUI(OfflinePlayer target) {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.friendsHelper = new FriendsHelper(playerDataCache);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        this.target = target; // Assign the target player
    }

    public void openFriendOptionsGUI(Player player) {

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

        Item removeFriend = new SimpleItem(new ItemBuilder(Material.REDSTONE_BLOCK)
                .setDisplayName(ChatColor.RED + "Remove Friend")
                .addLoreLines("", "Click to remove the player as a friend.", "")
                .addAllItemFlags()
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                if (target.isOnline() && target.getPlayer() != null) {
                    friendsHelper.removeFriend(player, target.getPlayer());
                } else {
                    player.sendMessage(ChatColor.RED + "The player is offline and cannot be removed as a friend.");
                }
                clickPlayer.sendMessage(ChatColor.RED + "You have removed " + target.getName() + " from your friends.");
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                window.close();
            }
        };

        Item inviteToParty = new SimpleItem(new ItemBuilder(Material.CAKE)
                .setDisplayName(ChatColor.GREEN + "Invite to Party")
                .addLoreLines("", "Click to invite the player to your party.", "")
                .addAllItemFlags()
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                PartyHelper partyHelper = partyModule.getPartyHelper();
                if (target.isOnline()) {
                    partyHelper.invitePlayer(player, target.getPlayer());
                } else {
                    player.sendMessage(ChatColor.RED + "The player is offline and cannot be invited to a party.");
                }
                clickPlayer.sendMessage(ChatColor.GREEN + "You have invited " + target.getName() + " to your party.");
                clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }
            }
        };

        Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # R # P # # #",
                        "X # # # # # # # #")
                .addIngredient('#', border)
                .addIngredient('X', back)
                .addIngredient('R', removeFriend)
                .addIngredient('P', inviteToParty)
                .build();

        // Open the GUI
        Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.GREEN + "Options for " + target.getName())
                .open(player);
    }
}
