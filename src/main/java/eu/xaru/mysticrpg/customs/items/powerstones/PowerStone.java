package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.Rarity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class PowerStone {

    private final String id;
    private final String name;
    private final Material material;
    private final int customModelData;
    private final String description;
    private final String effect; // The effect name
    private final String applicableTo; // e.g., "WEAPON", "ARMOR", "ALL"
    private final Rarity rarity;

    public PowerStone(String id, String name, Material material, int customModelData, String description, String effect, String applicableTo, Rarity rarity) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.customModelData = customModelData;
        this.description = description;
        this.effect = effect;
        this.applicableTo = applicableTo;
        this.rarity = rarity;
    }

    public String getId() {
        return id;
    }

    public String getEffect() {
        return effect;
    }

    public String getApplicableTo() {
        return applicableTo;
    }

    public String getName() {
        return name;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            // Lore
            List<String> loreList = new ArrayList<>();
            loreList.add(rarity.getColor() + rarity.name());
            loreList.add("");
            loreList.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', description));

            meta.setLore(loreList);

            // CustomModelData
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Store power stone ID in PersistentDataContainer
            NamespacedKey idKey = new NamespacedKey(JavaPlugin.getPlugin(MysticCore.class), "power_stone_id");
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }
}
