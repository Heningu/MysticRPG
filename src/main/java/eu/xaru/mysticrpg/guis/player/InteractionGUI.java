package eu.xaru.mysticrpg.guis.player;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.interaction.trading.TradeRequestManager;
import eu.xaru.mysticrpg.social.friends.FriendsHelper;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class InteractionGUI {

    private final TradeRequestManager tradeRequestManager;
    private final PartyModule partyModule;
    private final PlayerDataCache playerDataCache;
    private FriendsHelper friendsHelper;

    /**
     * Constructor to initialize InteractionGUI.
     *
     * @param tradeRequestManager The TradeRequestManager instance.
     */
    public InteractionGUI(TradeRequestManager tradeRequestManager) {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.tradeRequestManager = tradeRequestManager;
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        this.friendsHelper = new FriendsHelper(playerDataCache);

    }

    /**
     * Opens the Interaction GUI for the interacting player and the target player.
     *
     * @param player  The player interacting.
     * @param target The player being interacted with.
     */
    public void openInteractionGUI(Player player, Player target) {

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" ")
        );

        Item addFriend = new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(ChatColor.GREEN + "Add Friend")
                .addLoreLines(
                        "",
                        "Click here to add the player as a friend"
                )
                .addAllItemFlags()
        )        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {

                friendsHelper.sendFriendRequest(player, target);


                clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                window.close();
            }
        };

        Item inviteToParty = new SimpleItem(new ItemBuilder(Material.CAKE)
                .setDisplayName(ChatColor.GREEN + "Invite To Party")
                .addLoreLines(
                        "",
                        "Click here to invite the player to a party"
                )
                .addAllItemFlags()
        )
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {

                PartyHelper partyHelper = partyModule.getPartyHelper();
                partyHelper.invitePlayer(player, target);


                clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                window.close();
            }
        };

        Item trade = new SimpleItem(new ItemBuilder(Material.CHEST_MINECART)
                .setDisplayName(ChatColor.RED + "❌ Trade")
                .addLoreLines(
                        "",
                        "Click here to trade with the player",
                        "Will be implemented in the future."
                )
                .addAllItemFlags()
        )
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {

                // Implemented but bugged. Needs to be fixed before proper use
                // sendTradeRequest(clickPlayer, target);

                clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        };

        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# A # # I # # T #",
                        "# # # # # # # # #")
                .addIngredient('#', border)
                .addIngredient('A', addFriend)
                .addIngredient('T', trade)
                .addIngredient('I', inviteToParty)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + "Player Interaction")
                .setGui(gui)
                .build();
        window.open();

    }

    /**
     * Sends a trade request from the initiator to the target player using a clickable chat message.
     *
     * @param initiator The player initiating the trade.
     * @param target    The target player to trade with.
     */
    private void sendTradeRequest(Player initiator, Player target) {
        // Send a clickable message to the target player
        TextComponent message = new TextComponent(ChatColor.GREEN + initiator.getName() + " has requested to trade with you. ");
        TextComponent accept = new TextComponent("[Yes]");
        accept.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        accept.setBold(true);
        accept.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(Action.RUN_COMMAND, "/trade_accept " + initiator.getName()));
        accept.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("Click to accept the trade").create()));

        TextComponent decline = new TextComponent(" [No]");
        decline.setColor(net.md_5.bungee.api.ChatColor.RED);
        decline.setBold(true);
        decline.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(Action.RUN_COMMAND, "/trade_decline " + initiator.getName()));
        decline.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("Click to decline the trade").create()));

        message.addExtra(accept);
        message.addExtra(decline);

        target.spigot().sendMessage(message);

        // Register the trade request
        tradeRequestManager.createTradeRequest(initiator, target);
    }
}
