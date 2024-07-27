package eu.xaru.mysticrpg.content.menus;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MenuManager {
    private final Main plugin;
    private final JsonReader jsonReader;
    private final Map<String, Inventory> menus = new HashMap<>();
    private final InventoryManager inventoryManager;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
        this.jsonReader = new JsonReader();
        loadMenus();
        this.inventoryManager = new InventoryManager(menus, jsonReader.getHotbarItems());
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public void loadMenus() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        jsonReader.loadMenus(menusFolder);

        for (Map.Entry<String, MenuData> entry : jsonReader.getMenus().entrySet()) {
            menus.put(entry.getKey(), createMenu(entry.getValue()));
        }
    }

    private Inventory createMenu(MenuData menuData) {
        int rows = menuData.getRows();
        int columns = menuData.getColumns();
        int size = rows * columns;
        String title = ChatColor.translateAlternateColorCodes('&', menuData.getMenuName());
        Inventory menu = Bukkit.createInventory(null, size, title);

        for (MenuItem menuItem : menuData.getItems()) {
            ItemStack itemStack = createMenuItem(menuItem);
            int row = menuItem.getRow() - 1;
            int column = menuItem.getColumn() - 1;
            int slot = row * columns + column;
            menu.setItem(slot, itemStack);
        }

        if (menuData.isFillRestWithWhiteGlass()) {
            ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RESET + "");
                filler.setItemMeta(meta);
            }
            for (int i = 0; i < size; i++) {
                if (menu.getItem(i) == null) {
                    menu.setItem(i, filler);
                }
            }
        }

        return menu;
    }

    private ItemStack createMenuItem(MenuItem menuItem) {
        Material material = Material.valueOf(menuItem.getMaterial());
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', menuItem.getName()));
            meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', Arrays.toString(menuItem.getDescription().split("\n")))));
            if (menuItem.isHideEnchants()) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void openMenu(Player player, String menuName) {
        Inventory menu = menus.get(menuName);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }
        player.openInventory(menu);
    }

    public Set<String> getMenuNames() {
        return menus.keySet();
    }

    public void giveHotbarItemsToPlayer(Player player) {
        for (Map.Entry<String, HotbarItem> entry : jsonReader.getHotbarItems().entrySet()) {
            HotbarItem hotbarItemData = entry.getValue();
            if (hotbarItemData.isUse()) {
                ItemStack hotbarItem = createHotbarItem(hotbarItemData);
                int slot = hotbarItemData.getHotbarSlot() - 1;
                player.getInventory().setItem(slot, hotbarItem);
            }
        }
    }

    private ItemStack createHotbarItem(HotbarItem itemData) {
        Material material = Material.valueOf(itemData.getMaterial());
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemData.getName()));
            meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', Arrays.toString(itemData.getItemDescription().split("\n")))));
            if (itemData.isHideEnchants()) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (itemData.getEnchantments() != null) {
                itemData.getEnchantments().forEach((enchantment, level) -> {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.getByName(enchantment), level, true);
                });
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (inventoryManager.isMenuItem(item) || inventoryManager.isHotbarItem(item)) {
            event.setCancelled(true);
        }
    }
}
