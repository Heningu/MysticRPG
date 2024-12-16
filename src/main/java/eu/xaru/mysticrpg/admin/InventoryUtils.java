package eu.xaru.mysticrpg.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class InventoryUtils {

    public static void saveInventoryToFile(Player player, File file) {
        YamlConfiguration config = new YamlConfiguration();
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();

        config.set("inventory.contents", contents);
        config.set("inventory.armor", armor);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadInventoryFromFile(Player player, File file) {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemStack[] contents = ((List<ItemStack>) config.getList("inventory.contents")).toArray(new ItemStack[0]);
        ItemStack[] armor = ((List<ItemStack>) config.getList("inventory.armor")).toArray(new ItemStack[0]);

        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
    }
}
