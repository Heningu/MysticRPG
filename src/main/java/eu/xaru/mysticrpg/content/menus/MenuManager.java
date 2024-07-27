package eu.xaru.mysticrpg.content.menus;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MenuManager {
    private final Main plugin;
    private final Map<String, Inventory> menus = new HashMap<>();
    private final Map<String, JSONObject> hotbarItems = new HashMap<>();
    private final InventoryManager inventoryManager;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
        loadMenus();
        this.inventoryManager = new InventoryManager(menus);
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public void loadMenus() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }

        File[] menuFiles = menusFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (menuFiles != null) {
            for (File menuFile : menuFiles) {
                try (FileReader reader = new FileReader(menuFile)) {
                    JSONObject menuData = (JSONObject) new JSONParser().parse(reader);
                    String menuName = menuFile.getName().replace(".json", "");
                    menus.put(menuName, createMenu(menuData));
                    if ((boolean) menuData.getOrDefault("use_hotbar_item", false)) {
                        hotbarItems.put(menuName, (JSONObject) menuData.get("hotbar_item"));
                    }
                } catch (IOException | ParseException e) {
                    Logger.error("Failed to load menu " + menuFile.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private Inventory createMenu(JSONObject menuData) {
        int rows = ((Long) menuData.get("Rows")).intValue();
        int columns = ((Long) menuData.get("Columns")).intValue();
        int size = rows * columns;
        String title = ChatColor.translateAlternateColorCodes('&', (String) menuData.get("Menu_name"));
        Inventory menu = Bukkit.createInventory(null, size, title);

        for (Object itemObj : (Iterable<?>) menuData.get("items")) {
            JSONObject itemData = (JSONObject) itemObj;
            ItemStack itemStack = createMenuItem(itemData);
            int row = ((Long) itemData.get("Row")).intValue() - 1;
            int column = ((Long) itemData.get("Column")).intValue() - 1;
            int slot = row * columns + column;
            menu.setItem(slot, itemStack);
        }

        if ((boolean) menuData.getOrDefault("fill_rest_with_white_glass", false)) {
            ItemStack filler = createMenuItem(new JSONObject());
            filler.setType(Material.WHITE_STAINED_GLASS_PANE);
            for (int i = 0; i < size; i++) {
                if (menu.getItem(i) == null) {
                    menu.setItem(i, filler);
                }
            }
        }

        return menu;
    }

    private ItemStack createMenuItem(JSONObject itemData) {
        Material material = Material.valueOf((String) itemData.get("material"));
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (String) itemData.get("name")));
            meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', Arrays.toString(((String) itemData.get("description")).split("\n")))));
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
        for (Map.Entry<String, JSONObject> entry : hotbarItems.entrySet()) {
            JSONObject hotbarItemData = entry.getValue();
            if ((boolean) hotbarItemData.get("use")) {
                ItemStack hotbarItem = createHotbarItem(hotbarItemData);
                int slot = ((Long) hotbarItemData.get("hotbar_slot")).intValue() - 1;
                player.getInventory().setItem(slot, hotbarItem);
            }
        }
    }

    private ItemStack createHotbarItem(JSONObject itemData) {
        Material material = Material.valueOf((String) itemData.get("material"));
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (String) itemData.get("name")));
            meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', Arrays.toString(((String) itemData.get("item_description")).split("\n")))));
            if ((boolean) itemData.get("hideEnchants")) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (inventoryManager.isMenuItem(item)) {
            event.setCancelled(true);
        }
    }
}
