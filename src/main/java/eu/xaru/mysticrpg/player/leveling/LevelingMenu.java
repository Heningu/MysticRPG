package eu.xaru.mysticrpg.player.leveling;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.LevelData;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LevelingMenu {
    private final MysticCore plugin;
    private final PlayerDataCache playerDataCache;
    private final LevelModule levelModule;
    private final DebugLoggerModule logger;

    public LevelingMenu(MysticCore plugin) {
        this.plugin = plugin;
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            this.playerDataCache = saveModule.getPlayerDataCache();
        } else {
            throw new IllegalStateException("SaveModule not initialized. LevelingMenu cannot function without it.");
        }

        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        if (this.levelModule == null) {
            throw new IllegalStateException("LevelModule not initialized. LevelingMenu cannot function without it.");
        }

        this.logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
    }

    public void openLevelingMenu(Player player, int page) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
        if (playerData == null) {
            logger.error("No cached data found for player: " + player.getName());
            player.sendMessage(ChatColor.RED + "Failed to open leveling menu. No data found.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(player, 54, "Leveling Menu");

        // Player head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.setDisplayName(ChatColor.GOLD + player.getName() + " - Level: " + playerData.getLevel());
        playerHead.setItemMeta(headMeta);
        inventory.setItem(0, playerHead);

        // Pagination buttons
        ItemStack previousPage = createCustomHead("MHF_ArrowLeft", "Previous Page");
        ItemStack nextPage = createCustomHead("MHF_ArrowRight", "Next Page");
        inventory.setItem(45, page > 1 ? previousPage : createNoPageItem("No Previous Page"));
        inventory.setItem(53, hasNextPage(page) ? nextPage : createNoPageItem("No Next Page"));

        // Populate levels
        for (int i = 0; i < 26; i++) {
            int level = (page - 1) * 26 + i + 1; // Adjusted to start at index + 1
            if (level > levelModule.getMaxLevel()) break;
            int slot = calculateSlot(i);
            ItemStack levelItem = createLevelItem(playerData, level);
            inventory.setItem(slot, levelItem);
        }

        // Fill remaining slots with white glass panes
        fillEmptySlots(inventory);

        player.openInventory(inventory);
    }

    private boolean hasNextPage(int page) {
        return page * 26 < levelModule.getMaxLevel();
    }

    private int calculateSlot(int index) {
        int[] slots = {
                1, 2, 3, 4, 5, 6, 7, 8,
                17,
                19, 20, 21, 22, 23, 24, 25, 26,
                28,
                37, 38, 39, 40, 41, 42, 43, 44
        };

        return slots[index];
    }

    private ItemStack createLevelItem(PlayerData playerData, int level) {
        int playerLevel = playerData.getLevel();
        Material material = playerLevel >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

        LevelData levelData = levelModule.getLevelData(level);
        if (levelData == null) return new ItemStack(Material.BARRIER);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Level: " + level);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Required XP: " + levelData.getXpRequired());
        lore.add(ChatColor.WHITE + "Rewards:");
        levelData.getRewards().forEach((reward, value) -> {
            lore.add(ChatColor.GRAY + reward + ": " + value);
        });

        meta.setLore(lore);
        item.setItemMeta(meta);

        if (playerLevel >= level) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        return item;
    }


    private void fillEmptySlots(Inventory inventory) {
        ItemStack fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        fillerMeta.setDisplayName(" ");
        fillerItem.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillerItem);
            }
        }
    }

    private ItemStack createCustomHead(String playerName, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(displayName);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createNoPageItem(String displayName) {
        ItemStack item = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }
}
