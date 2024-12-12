package eu.xaru.mysticrpg.guis.quests;

import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Map;

public class ShopGUI {

    private final Player player;
    private final Map<String, Integer> shopItems; // itemName -> price

    public ShopGUI(Player player, Map<String,Integer> shopItems) {
        this.player = player;
        this.shopItems = shopItems;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # # # # # # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', createFiller())
                .build();

        int index = 0;
        for (Map.Entry<String,Integer> entry : shopItems.entrySet()) {
            if (index > 26) break;
            ItemStack item = createShopItemStack(entry.getKey());
            int price = entry.getValue();
            gui.setItem(index, createShopItem(item, price));
            index++;
        }

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GOLD + "Shop")
                .setGui(gui)
                .build();
        window.open();
    }

    private Item createFiller() {
        return new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ").addAllItemFlags());
    }

    private ItemStack createShopItemStack(String itemId) {
        Material mat = Material.matchMaterial(itemId.toUpperCase());
        if (mat == null) mat = Material.STONE;
        return new ItemStack(mat);
    }

    private Item createShopItem(ItemStack item, int price) {
        return new SimpleItem(new ItemBuilder(item)
                .setDisplayName(ChatColor.YELLOW + "Buy " + item.getType().name())
                .addLoreLines(ChatColor.GRAY + "Price: " + price + " gold", ChatColor.GREEN + "Click to buy")
                .addAllItemFlags())
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                // Check player's balance from PlayerData, for example:
                // PlayerData data = playerDataCache.getCachedPlayerData(clickPlayer.getUniqueId());
                // int playerBalance = data.getBalance(); (Assuming getBalance() returns player's gold)
                int playerBalance = 1000; // example hardcoded

                if (playerBalance >= price) {
                    // reduce balance, give item
                    // data.setBalance(playerBalance - price);
                    clickPlayer.getInventory().addItem(item.clone());
                    clickPlayer.sendMessage(Utils.getInstance().$("You purchased " + item.getType().name() + "!"));
                } else {
                    clickPlayer.sendMessage(Utils.getInstance().$("Not enough gold!"));
                }
            }
        };
    }
}
