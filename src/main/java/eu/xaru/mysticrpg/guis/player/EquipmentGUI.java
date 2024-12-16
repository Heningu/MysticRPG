package eu.xaru.mysticrpg.guis.player;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Map;

public class EquipmentGUI {

    private final JavaPlugin plugin;
    private final PlayerDataCache playerDataCache;
    private final ItemManager itemManager;
    private final NamespacedKey customItemKey;

    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final StatsModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;



    // Basic filler
    private final Item filler = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .setDisplayName("")
            .addAllItemFlags()
    );

    // Icons for the equipment types
    private final Item helmetIcon = iconItem(Material.DIAMOND_HELMET, "Helmet");
    private final Item chestplateIcon = iconItem(Material.DIAMOND_CHESTPLATE, "Chestplate");
    private final Item leggingsIcon = iconItem(Material.DIAMOND_LEGGINGS, "Leggings");
    private final Item bootsIcon = iconItem(Material.DIAMOND_BOOTS, "Boots");
    private final Item amuletIcon = iconItem(Material.HEART_OF_THE_SEA, "Amulet");
    private final Item cloakIcon = iconItem(Material.ELYTRA, "Cloak");
    private final Item glovesIcon = iconItem(Material.FLOWER_POT, "Gloves");
    private final Item coreIcon = iconItem(Material.NETHER_STAR, "Core");
    private final Item crownIcon = iconItem(Material.GOLDEN_HELMET, "Crown");

    // Slot indices (from structure)
    private static final int HELMET_SLOT = 19;
    private static final int CHESTPLATE_SLOT = 20;
    private static final int LEGGINGS_SLOT = 21;
    private static final int BOOTS_SLOT = 22;
    private static final int AMULET_SLOT = 37;
    private static final int CLOAK_SLOT = 38;
    private static final int GLOVES_SLOT = 39;
    private static final int CORE_SLOT = 40;
    private static final int CROWN_SLOT = 16;

    public EquipmentGUI() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        CustomItemModule cim = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class);
        this.itemManager = cim != null ? cim.getItemManager() : null;
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openEquipmentGUI(Player player) {


        // Static items
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };



        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# H C L B # Y # #",
                        "# . . . . # # # #",
                        "# A U K R # # # #",
                        "# . . . . # # # #",
                        "X # # # # # # # #")
                .addIngredient('#', filler)
                .addIngredient('H', helmetIcon)
                .addIngredient('C', chestplateIcon)
                .addIngredient('L', leggingsIcon)
                .addIngredient('B', bootsIcon)
                .addIngredient('A', amuletIcon)
                .addIngredient('U', cloakIcon)
                .addIngredient('K', glovesIcon)
                .addIngredient('R', coreIcon)
                .addIngredient('Y', crownIcon)
                .addIngredient('X',back)
                .build();

        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        Map<String, String> equipment = playerData != null ? playerData.getEquipment() : null;

        setOrCreateEmptySlot(gui, player, "Helmet", HELMET_SLOT, equipment != null ? equipment.get("Helmet") : null);
        setOrCreateEmptySlot(gui, player, "Chestplate", CHESTPLATE_SLOT, equipment != null ? equipment.get("Chestplate") : null);
        setOrCreateEmptySlot(gui, player, "Leggings", LEGGINGS_SLOT, equipment != null ? equipment.get("Leggings") : null);
        setOrCreateEmptySlot(gui, player, "Boots", BOOTS_SLOT, equipment != null ? equipment.get("Boots") : null);
        setOrCreateEmptySlot(gui, player, "Amulet", AMULET_SLOT, equipment != null ? equipment.get("Amulet") : null);
        setOrCreateEmptySlot(gui, player, "Cloak", CLOAK_SLOT, equipment != null ? equipment.get("Cloak") : null);
        setOrCreateEmptySlot(gui, player, "Gloves", GLOVES_SLOT, equipment != null ? equipment.get("Gloves") : null);
        setOrCreateEmptySlot(gui, player, "Core", CORE_SLOT, equipment != null ? equipment.get("Core") : null);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.RED + "Manage your equipment")
                .setGui(gui)
                .build();
        window.open();
    }

    private void setOrCreateEmptySlot(Gui gui, Player player, String slotName, int slotIndex, String serialized) {
        ItemStack item = deserializeItemStack(serialized);
        if (item == null || item.getType() == Material.AIR) {
            gui.setItem(slotIndex, createEmptySlotItem(gui, slotIndex, slotName, player));
        } else {
            gui.setItem(slotIndex, new SimpleItem(item) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                    handleEquipmentItemClick(gui, slotIndex, clickPlayer, slotName, event);
                }
            });
            equipArmorPiece(player, slotName, item);
        }
    }

    /**
     * Creates a SimpleItem representing an empty slot.
     * When clicked, it attempts to place the cursor item if valid, or remove if there's an item.
     */
    private Item createEmptySlotItem(Gui gui, int slotIndex, String slotName, Player player) {
        return new SimpleItem(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .setDisplayName(ChatColor.GRAY + "Empty " + slotName + " Slot")
                .addAllItemFlags()) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                ItemStack cursorItem = clickPlayer.getItemOnCursor();
                ItemStack currentItem = event.getCurrentItem();

                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    // Placing item into slot
                    if (isValidItemForSlot(slotName, cursorItem)) {
                        clickPlayer.setItemOnCursor(null);
                        saveEquipmentItem(clickPlayer, slotName, cursorItem);
                        equipArmorPiece(clickPlayer, slotName, cursorItem);
                        // Update via InvUI
                        gui.setItem(slotIndex, new SimpleItem(cursorItem) {
                            @Override
                            public void handleClick(@NotNull ClickType ct, @NotNull Player cp, @NotNull InventoryClickEvent e) {
                                handleEquipmentItemClick(gui, slotIndex, cp, slotName, e);
                            }
                        });
                    } else {
                        clickPlayer.sendMessage(Utils.getInstance().$(ChatColor.RED + "You cannot place that item in the " + slotName + " slot."));
                        event.setCancelled(true);
                    }
                } else {
                    // Removing item if any (shouldn't be any since it's empty slot item)
                    if (currentItem != null && currentItem.getType() != Material.GREEN_STAINED_GLASS_PANE) {
                        // There's some unexpected item here, let's remove it
                        clickPlayer.setItemOnCursor(currentItem);
                        saveEquipmentItem(clickPlayer, slotName, null);
                        equipArmorPiece(clickPlayer, slotName, null);
                        // Reset to empty slot via InvUI
                        gui.setItem(slotIndex, createEmptySlotItem(gui, slotIndex, slotName, player));
                    } else {
                        // Nothing to remove
                        event.setCancelled(true);
                    }
                }
            }
        };
    }

    private void handleEquipmentItemClick(Gui gui, int slotIndex, Player player, String slotName, InventoryClickEvent event) {
        ItemStack cursorItem = player.getItemOnCursor();
        ItemStack currentItem = event.getCurrentItem();

        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Trying to replace existing item
            if (isValidItemForSlot(slotName, cursorItem)) {
                player.setItemOnCursor(currentItem);
                saveEquipmentItem(player, slotName, cursorItem);
                equipArmorPiece(player, slotName, cursorItem);
                // Update via InvUI
                gui.setItem(slotIndex, new SimpleItem(cursorItem) {
                    @Override
                    public void handleClick(@NotNull ClickType ct, @NotNull Player cp, @NotNull InventoryClickEvent e) {
                        handleEquipmentItemClick(gui, slotIndex, cp, slotName, e);
                    }
                });
            } else {
                player.sendMessage(Utils.getInstance().$(ChatColor.RED + "You cannot place that item in the " + slotName + " slot."));
                event.setCancelled(true);
            }
        } else {
            // Removing item
            player.setItemOnCursor(currentItem);
            saveEquipmentItem(player, slotName, null);
            equipArmorPiece(player, slotName, null);
            // Reset to empty slot
            gui.setItem(slotIndex, createEmptySlotItem(gui, slotIndex, slotName, player));
        }
    }

    private boolean isValidItemForSlot(String slotName, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        String type = item.getType().name();

        switch (slotName) {
            case "Helmet": return type.endsWith("_HELMET") || isCustomArmorPiece(item, "HELMET");
            case "Chestplate": return type.endsWith("_CHESTPLATE") || isCustomArmorPiece(item, "CHESTPLATE");
            case "Leggings": return type.endsWith("_LEGGINGS") || isCustomArmorPiece(item, "LEGGINGS");
            case "Boots": return type.endsWith("_BOOTS") || isCustomArmorPiece(item, "BOOTS");
            case "Amulet": return item.getType() == Material.HEART_OF_THE_SEA || item.getType() == Material.AMETHYST_SHARD;
            case "Cloak": return item.getType() == Material.ELYTRA;
            case "Gloves": return item.getType() == Material.LEATHER || item.getType() == Material.RABBIT_HIDE;
            case "Core": return item.getType() == Material.NETHER_STAR;
            default: return false;
        }
    }

    private boolean isCustomArmorPiece(ItemStack item, String armorType) {
        if (itemManager == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String customItemId = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        if (customItemId != null) {
            CustomItem customItem = itemManager.getCustomItem(customItemId);
            return customItem != null && armorType.equalsIgnoreCase(customItem.getArmorType());
        }
        return false;
    }

    private void saveEquipmentItem(Player player, String slotName, ItemStack item) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) return;
        Map<String, String> equipment = playerData.getEquipment();
        if (equipment == null) return;

        if (item == null || item.getType() == Material.AIR) {
            equipment.remove(slotName);
        } else {
            equipment.put(slotName, serializeItemStack(item));
        }
    }

    private void equipArmorPiece(Player player, String slotName, ItemStack item) {
        switch (slotName) {
            case "Helmet": player.getInventory().setHelmet(item); break;
            case "Chestplate": player.getInventory().setChestplate(item); break;
            case "Leggings": player.getInventory().setLeggings(item); break;
            case "Boots": player.getInventory().setBoots(item); break;
            default:
                // For other items (Amulet, Cloak, Gloves, Core), implement custom logic if needed
                break;
        }
    }

    private String serializeItemStack(ItemStack item) {
        if (item == null) return null;
        try {
            return Utils.getInstance().itemStackToBase64(item);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ItemStack deserializeItemStack(String data) {
        if (data == null) return null;
        try {
            return Utils.getInstance().itemStackFromBase64(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Item iconItem(Material mat, String name) {
        return new SimpleItem(new ItemBuilder(mat)
                .setDisplayName(name)
                .addLoreLines(
                        "",
                        "Drag a " + name + " into the empty slot",
                        "to equip the desired " + name,
                        ""
                )
                .addAllItemFlags());
    }
}
