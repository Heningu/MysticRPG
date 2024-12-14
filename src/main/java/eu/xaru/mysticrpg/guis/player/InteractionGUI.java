package eu.xaru.mysticrpg.guis.player;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class InteractionGUI {


    public InteractionGUI() {

    }


    public void openInteractionGUI(Player player, Player target) {


        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));

        Item addFriend = new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(ChatColor.GREEN + "Add Friend")
                .addLoreLines(
                        "",
                        "Click here to add the player as a friend"
                )
                .addAllItemFlags()
        );

        Item inviteToParty = new SimpleItem(new ItemBuilder(Material.CAKE)
                .setDisplayName(ChatColor.GREEN + "Invite To Party")
                .addLoreLines(
                        "",
                        "Click here to invite the player to a party"
                )
                .addAllItemFlags()
        );

        Item trade = new SimpleItem(new ItemBuilder(Material.CHEST_MINECART)
                .setDisplayName(ChatColor.GREEN + "Trade")
                .addLoreLines(
                        "",
                        "Click here to trade with the player"
                )
                .addAllItemFlags()
        );



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
                .setTitle(ChatColor.RED + "Loaded custom mobs")
                .setGui(gui)
                .build();
        window.open();


    }




}
