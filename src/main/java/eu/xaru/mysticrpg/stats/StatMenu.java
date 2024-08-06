package eu.xaru.mysticrpg.stats;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;

public class StatMenu {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;

    public StatMenu(Main plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void openStatMenu(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        Inventory inventory = Bukkit.createInventory(player, 54, "Player Stats");

        // Player head with skin and combined values
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.setDisplayName(ChatColor.GOLD + player.getName());
        headMeta.setLore(Arrays.asList(
                ChatColor.WHITE + "Vitality: " + data.getVitality(),
                ChatColor.WHITE + "Intelligence: " + data.getIntelligence(),
                ChatColor.WHITE + "Dexterity: " + data.getDexterity(),
                ChatColor.WHITE + "Strength: " + data.getStrength(),
                ChatColor.WHITE + "HP: " + data.getHp(),
                ChatColor.WHITE + "Mana: " + data.getMana()
        ));
        playerHead.setItemMeta(headMeta);
        inventory.setItem(4, playerHead);

        // Stats and attributes
        addStatItem(inventory, 10, Material.APPLE, "Vitality", data.getVitality(), data.getHp(), data.getAttributePoints());
        addStatItem(inventory, 12, Material.DRAGON_BREATH, "Intelligence", data.getIntelligence(), data.getMana(), data.getAttributePoints());
        addStatItem(inventory, 14, Material.ARROW, "Dexterity", data.getDexterity(), data.getAttackDamageDex(), data.getAttributePoints());
        addStatItem(inventory, 16, Material.IRON_SWORD, "Strength", data.getStrength(), data.getAttackDamage(), data.getAttributePoints());

        // Attribute points
        ItemStack attributePoints = new ItemStack(Material.NETHER_STAR);
        ItemMeta pointsMeta = attributePoints.getItemMeta();
        pointsMeta.setDisplayName(ChatColor.GOLD + "Attribute Points");
        pointsMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Points: " + data.getAttributePoints()));
        attributePoints.setItemMeta(pointsMeta);
        inventory.setItem(53, attributePoints);

        // Fill the rest with white glass panes
        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        fillerMeta.setDisplayName(" ");
        fillerItem.setItemMeta(fillerMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillerItem);
            }
        }

        player.openInventory(inventory);
    }

    private void addStatItem(Inventory inventory, int slot, Material material, String name, int attribute, int stat, int attributePoints) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + name);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Attribute: " + attribute, ChatColor.WHITE + "Stat: " + stat));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);

        ItemStack button = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName(ChatColor.AQUA + "Increase " + name);
        buttonMeta.setLore(Collections.emptyList()); // Explicitly set lore to hide flight duration text
        button.setItemMeta(buttonMeta);
        inventory.setItem(slot + 9, button); // Assuming the button is placed in the row below the stat item
    }
}
