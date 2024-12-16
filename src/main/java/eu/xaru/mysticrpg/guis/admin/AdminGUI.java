package eu.xaru.mysticrpg.guis.admin;

import eu.xaru.mysticrpg.admin.AdminModule;
import eu.xaru.mysticrpg.guis.player.social.FriendsGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class AdminGUI {

    private final AdminModule module;


    public AdminGUI() {
        this.module = ModuleManager.getInstance().getModuleInstance(AdminModule.class);
    }



    public void openAdminGUI(Player player){


        Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags()
        );


        Item enterAdminMode = new SimpleItem(new ItemBuilder(Material.REDSTONE)
                .setDisplayName(ChatColor.RED + "Enter Admin Mode")
                .addLoreLines(
                        "Click to enter or leave admin mode"
                )
                .addAllItemFlags()
        ){
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {


                if (module.isInAdminMode(player)) {
                    module.exitAdminMode(player);
                } else {
                    module.enterAdminMode(player);
                }



            }
        };

        Item mobGUI = new SimpleItem(new ItemBuilder(Material.ZOMBIE_HEAD)
                .setDisplayName(ChatColor.GOLD + "Mob GUI")
                .addLoreLines(
                        "Click to open the MobGUI"
                )
                .addAllItemFlags()){
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Close the current GUI before opening the Equipment GUI
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ?
                        (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MobGUI mobg = new MobGUI();
                mobg.openMobGUI(player);


            }
        };

        Gui admin = Gui.normal().setStructure(
                        "# # # # # # # # #",
                        "# M # # # # # # #",
                        "E # # # # # # # #"


                )
                .addIngredient('E', enterAdminMode)
                .addIngredient('M', mobGUI)
                .addIngredient('#', filler)

                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + "Admin GUI")
                .setGui(admin)
                .build();

        window.open();
    }


}
