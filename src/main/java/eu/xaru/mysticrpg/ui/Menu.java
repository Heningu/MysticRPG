//package eu.xaru.mysticrpg.ui;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.lang.reflect.Type;
//import java.util.List;
//
//public class Menu {
//    private final String name;
//    private final int rows;
//    private final boolean fillWithGlass;
//    private final MenuHotbarItem hotbarItem;
//    private final List<MenuItem> items;
//
//    public Menu(String name, int rows, boolean fillWithGlass, MenuHotbarItem hotbarItem, List<MenuItem> items) {
//        this.name = name;
//        this.rows = rows;
//        this.fillWithGlass = fillWithGlass;
//        this.hotbarItem = hotbarItem;
//        this.items = items;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public MenuHotbarItem getHotbarItem() {
//        return hotbarItem;
//    }
//
//    public void open(Player player) {
//        Inventory inv = Bukkit.createInventory(player, rows * 9, name);
//
//        if (fillWithGlass) {
//            ItemStack glassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
//            ItemMeta glassMeta = glassPane.getItemMeta();
//            glassMeta.setDisplayName(" ");
//            glassPane.setItemMeta(glassMeta);
//            for (int i = 0; i < inv.getSize(); i++) {
//                inv.setItem(i, glassPane);
//            }
//        }
//
//        for (MenuItem menuItem : items) {
//            ItemStack item = new ItemStack(Material.getMaterial(menuItem.getMaterial()));
//            ItemMeta meta = item.getItemMeta();
//            meta.setDisplayName(menuItem.getName());
//            meta.setLore(menuItem.getDescription());
//            if (menuItem.isHideEnchants()) {
//                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
//            }
//            item.setItemMeta(meta);
//            inv.setItem(menuItem.getRow() * 9 + menuItem.getColumn(), item);
//        }
//
//        player.openInventory(inv);
//    }
//
//    public static Menu loadFromFile(File file) {
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        try (FileReader reader = new FileReader(file)) {
//            Type menuType = new TypeToken<Menu>() {}.getType();
//            return gson.fromJson(reader, menuType);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}
