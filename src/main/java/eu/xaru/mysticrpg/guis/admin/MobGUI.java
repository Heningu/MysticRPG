package eu.xaru.mysticrpg.guis.admin;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for displaying all registered custom mobs as their default heads using a paged InvUI GUI.
 */
public class MobGUI {

    private final MobManager mobManager;
    private final JavaPlugin plugin;

    /**
     * Constructor for the MobGUI.
     *
     * @param mobManager The MobManager instance to access mob configurations and spawning.
     */
    public MobGUI(MobManager mobManager) {
        this.mobManager = mobManager;
        this.plugin = JavaPlugin.getPlugin(MysticCore.class); // Ensure MysticCore is your main class
    }

    /**
     * Opens the Mob Paged GUI for the specified player.
     *
     * @param player The player to open the GUI for.
     */
    public void openMobGUI(Player player) {
        // Retrieve all registered mob configurations
        Map<String, CustomMob> mobConfigurations = mobManager.getMobConfigurations();
        int mobCount = mobConfigurations.size();

        if (mobCount == 0) {
            player.sendMessage(Utils.getInstance().$(ChatColor.RED + "No mobs are currently registered."));
            return;
        }

        // Create a list of Items representing each mob
        List<Item> mobItems = mobConfigurations.values().stream().map(customMob -> {
            // Determine the appropriate head material based on the entity type
            Material headMaterial = getHeadMaterial(customMob.getEntityType());

            // If the entity has a specific head material, use it; otherwise, fallback to PLAYER_HEAD
            ItemStack mobHead;
            if (headMaterial != null) {
                mobHead = new ItemStack(headMaterial, 1);
            } else {
                mobHead = new ItemStack(Material.PLAYER_HEAD, 1);
            }

            Item item = new SimpleItem(new ItemBuilder(mobHead)
                    .setDisplayName(ChatColor.YELLOW + customMob.getName())
                    .addLoreLines(
                            "",
                            Utils.getInstance().$("Level: " + customMob.getLevel()),
                            "",
                            "Click to spawn this mob."
                    )
                    .addAllItemFlags()
            ) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                    // Spawn the custom mob at the player's location
                    mobManager.spawnMobAtLocation(customMob, player.getLocation());

                    // Provide feedback to the player
                    player.sendMessage(Utils.getInstance().$("Spawned mob: " + customMob.getName()));

                    // Play a sound for feedback
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            };

            return item;
        }).collect(Collectors.toList());

        // Create Control Items: Back and Forward
        Item controler = new ChangePageItem();

        // Placeholder Item for borders
        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" "));


        // Create the paged GUI
        PagedGui<Item> pagedGui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "> # # # # # # # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be put
                .addIngredient('#', border)
                .addIngredient('>', controler)
                .setContent(mobItems)
                .build();

        // Create and open the Window with the paged GUI
        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + "Loaded custom mobs")
                .setGui(pagedGui)
                .build();
        window.open();
    }


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

    /**
     * Maps an EntityType to its corresponding Minecraft head material.
     *
     * @param entityType The EntityType of the mob.
     * @return The corresponding head Material, or null if no specific head exists.
     */
    private Material getHeadMaterial(org.bukkit.entity.EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case SKELETON:
                return Material.SKELETON_SKULL;
            case CREEPER:
                return Material.CREEPER_HEAD;
            case WITHER_SKELETON:
                return Material.WITHER_SKELETON_SKULL;
            case PLAYER:
                return Material.PLAYER_HEAD;
            // Add more cases as needed for other EntityTypes that have default head items
            default:
                return null; // No specific head material exists for this EntityType
        }
    }
}
