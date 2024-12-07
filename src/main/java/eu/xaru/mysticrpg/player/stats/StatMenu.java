package eu.xaru.mysticrpg.player.stats;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
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
import java.util.Map;
import java.util.UUID;

public class StatMenu {
    private final MysticCore plugin;
    private final PlayerDataCache playerDataCache;
    

    public StatMenu(MysticCore plugin) {
        this.plugin = plugin;
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);

        if (saveModule != null) {
            this.playerDataCache = saveModule.getPlayerDataCache();
        } else {
            throw new IllegalStateException("SaveModule not initialized. StatMenu cannot function without it.");
        }

    }

    public void openStatMenu(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);

        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            player.sendMessage(Utils.getInstance().$("Failed to open stat menu. No data found."));
            return;
        }

        Inventory inventory = Bukkit.createInventory(player, 54, "Player Stats");
        Map<String, Integer> attributes = data.getAttributes();

        // Player head with skin and combined values
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.setDisplayName(Utils.getInstance().$( player.getName()));
        headMeta.setLore(Arrays.asList(
                Utils.getInstance().$("Vitality: " + attributes.getOrDefault("Vitality", 0)),
                Utils.getInstance().$("Intelligence: " + attributes.getOrDefault("Intelligence", 0)),
                Utils.getInstance().$("Dexterity: " + attributes.getOrDefault("Dexterity", 0)),
                Utils.getInstance().$("Strength: " + attributes.getOrDefault("Strength", 0)),
                Utils.getInstance().$("HP: " + attributes.getOrDefault("HP", 0)),
                Utils.getInstance().$("Mana: " + attributes.getOrDefault("MANA", 0))
        ));
        playerHead.setItemMeta(headMeta);
        inventory.setItem(4, playerHead);

        // Stats and attributes
        addStatItem(inventory, 10, Material.APPLE, "Vitality", attributes.getOrDefault("Vitality", 0), attributes.getOrDefault("HP", 0), data.getAttributePoints());
        addStatItem(inventory, 12, Material.DRAGON_BREATH, "Intelligence", attributes.getOrDefault("Intelligence", 0), attributes.getOrDefault("MANA", 0), data.getAttributePoints());
        addStatItem(inventory, 14, Material.ARROW, "Dexterity", attributes.getOrDefault("Dexterity", 0), attributes.getOrDefault("AttackDamageDex", 0), data.getAttributePoints());
        addStatItem(inventory, 16, Material.IRON_SWORD, "Strength", attributes.getOrDefault("Strength", 0), attributes.getOrDefault("AttackDamage", 0), data.getAttributePoints());

        // Attribute points
        ItemStack attributePoints = new ItemStack(Material.NETHER_STAR);
        ItemMeta pointsMeta = attributePoints.getItemMeta();
        pointsMeta.setDisplayName(Utils.getInstance().$( "Attribute Points"));
        pointsMeta.setLore(Collections.singletonList(Utils.getInstance().$("Points: " + data.getAttributePoints())));
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
        meta.setDisplayName(Utils.getInstance().$( name));
        meta.setLore(Arrays.asList(Utils.getInstance().$( "Attribute: " + attribute),Utils.getInstance().$( "Stat: " + stat)));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);

        Material buttonMaterial = attributePoints > 0 ? Material.SUNFLOWER : Material.BEDROCK;
        ItemStack button = new ItemStack(buttonMaterial);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName(attributePoints > 0 ? Utils.getInstance().$( "Increase " + name) : Utils.getInstance().$( "[NO POINTS]"));
        buttonMeta.setLore(Collections.emptyList()); // Clear lore
        button.setItemMeta(buttonMeta);
        inventory.setItem(slot + 9, button); // Assuming the button is placed in the row below the stat item
    }
}
