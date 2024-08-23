//package eu.xaru.mysticrpg.player.leveling;
//
//import eu.xaru.mysticrpg.cores.MysticCore;
//import eu.xaru.mysticrpg.storage.old_playerdata;
//import eu.xaru.mysticrpg.storage.PlayerDataManager;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.inventory.meta.SkullMeta;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class LevelingMenu {
//    private final MysticCore plugin;
//    private final LevelingManager levelingManager;
//    private final PlayerDataManager playerDataManager;
//
//    public LevelingMenu(MysticCore plugin, LevelingManager levelingManager, PlayerDataManager playerDataManager) {
//        this.plugin = plugin;
//        this.levelingManager = levelingManager;
//        this.playerDataManager = playerDataManager;
//    }
//
//    public void openLevelingMenu(Player player, int page) {
//        old_playerdata playerData = playerDataManager.getPlayerData(player);
//        Inventory inventory = Bukkit.createInventory(player, 54, "Leveling Menu");
//
//        // Player head
//        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
//        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
//        headMeta.setOwningPlayer(player);
//        headMeta.setDisplayName(ChatColor.GOLD + player.getName() + " - Level: " + playerData.getLevel());
//        playerHead.setItemMeta(headMeta);
//        inventory.setItem(0, playerHead);
//
//        // Pagination buttons
//        ItemStack previousPage = createCustomHead("MHF_ArrowLeft", "Previous Page");
//        ItemStack nextPage = createCustomHead("MHF_ArrowRight", "Next Page");
//        inventory.setItem(45, page > 1 ? previousPage : createNoPageItem("No Previous Page"));
//        inventory.setItem(53, hasNextPage(page) ? nextPage : createNoPageItem("No Next Page"));
//
//        // Populate levels
//        for (int i = 0; i < 26; i++) {
//            int level = (page - 1) * 26 + i + 1; // Adjusted to start at index + 1
//            if (level > levelingManager.getMaxLevel()) break;
//            int slot = calculateSlot(i);
//            ItemStack levelItem = createLevelItem(playerData, level);
//            inventory.setItem(slot, levelItem);
//        }
//
//        // Fill remaining slots with white glass panes
//        fillEmptySlots(inventory);
//
//        player.openInventory(inventory);
//    }
//
//    private boolean hasNextPage(int page) {
//        return page * 26 < levelingManager.getMaxLevel();
//    }
//
//    private int calculateSlot(int index) {
//        int[] slots = {
//                1, 2, 3, 4, 5, 6, 7, 8,
//                17,
//                19, 20, 21, 22, 23, 24, 25, 26,
//                28,
//                37, 38, 39, 40, 41, 42, 43, 44
//        };
//
//        return slots[index];
//    }
//
//    private ItemStack createLevelItem(old_playerdata playerData, int level) {
//        int playerLevel = playerData.getLevel();
//        Material material = playerLevel >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
//
//        if (levelingManager.isSpecialLevel(level)) {
//            material = Material.NETHER_STAR;
//        }
//
//        ItemStack item = new ItemStack(material);
//        ItemMeta meta = item.getItemMeta();
//        meta.setDisplayName(ChatColor.YELLOW + "Level: " + level);
//
//        List<String> lore = new ArrayList<>();
//        lore.add(ChatColor.WHITE + "Required XP: " + levelingManager.getLevelThreshold(level));
//        lore.add(ChatColor.WHITE + "Rewards:");
//        levelingManager.getLevelRewards(level).forEach((reward, value) -> {
//            lore.add(ChatColor.GRAY + reward + ": " + value);
//        });
//
//        meta.setLore(lore);
//        item.setItemMeta(meta);
//
//        if (playerLevel >= level) {
//            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
//            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
//        }
//
//        return item;
//    }
//
//    private void fillEmptySlots(Inventory inventory) {
//        ItemStack fillerItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
//        ItemMeta fillerMeta = fillerItem.getItemMeta();
//        fillerMeta.setDisplayName(" ");
//        fillerItem.setItemMeta(fillerMeta);
//
//        for (int i = 0; i < inventory.getSize(); i++) {
//            if (inventory.getItem(i) == null) {
//                inventory.setItem(i, fillerItem);
//            }
//        }
//    }
//
//    private ItemStack createCustomHead(String playerName, String displayName) {
//        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
//        SkullMeta meta = (SkullMeta) head.getItemMeta();
//        meta.setOwner(playerName);
//        meta.setDisplayName(displayName);
//        head.setItemMeta(meta);
//        return head;
//    }
//
//    private ItemStack createNoPageItem(String displayName) {
//        ItemStack item = new ItemStack(Material.WITHER_SKELETON_SKULL);
//        ItemMeta meta = item.getItemMeta();
//        meta.setDisplayName(displayName);
//        item.setItemMeta(meta);
//        return item;
//    }
//}
