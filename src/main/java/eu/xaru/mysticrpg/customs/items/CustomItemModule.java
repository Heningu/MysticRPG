package eu.xaru.mysticrpg.customs.items;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneManager;
import eu.xaru.mysticrpg.customs.items.powerstones.PowerStoneModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class CustomItemModule implements IBaseModule {

    private ItemManager itemManager;

    private EventManager eventManager;
    private JavaPlugin plugin;
    private PowerStoneManager powerStoneManager;

    @Override
    public void initialize() {
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        itemManager = new ItemManager();
        eventManager = new EventManager(plugin);

        // Get PowerStoneManager instance
        PowerStoneModule powerStoneModule = ModuleManager.getInstance().getModuleInstance(PowerStoneModule.class);
        if (powerStoneModule == null) {
            throw new IllegalStateException("PowerStoneModule not initialized. CustomItemModule cannot function without it.");
        }
        powerStoneManager = powerStoneModule.getPowerStoneManager();

        registerCommands();
        registerEventHandlers();

        DebugLogger.getInstance().log(Level.INFO, "CustomItemModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "CustomItemModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "CustomItemModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "CustomItemModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(PowerStoneModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public CustomItem getCustomItemById(String id) {
        return itemManager.getCustomItem(id);
    }

    /**
     * Creates a CustomItem from a normal ItemStack.
     * This is a fallback method used if we want to generate a custom item from an existing stack.
     */
    public CustomItem createCustomItemFromItemStack(ItemStack itemStack) {
        String id = generateUniqueIdForItemStack(itemStack);
        String name = (itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName()) ?
                itemStack.getItemMeta().getDisplayName() : itemStack.getType().toString();

        // Default rarity and category
        Rarity rarity = Rarity.COMMON;
        Category category = Category.MISC;

        // No tier system, no sets, no enchantments by default
        return new CustomItem(
                id, name, itemStack.getType(), rarity, category, 0,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(),
                false, false, 1, 1, Collections.emptyMap(),
                false, 0, null, null
        );
    }

    private String generateUniqueIdForItemStack(ItemStack itemStack) {
        return "item_" + UUID.randomUUID().toString();
    }

    private void registerCommands() {
        new CommandAPICommand("customitem")
                .withPermission("mysticrpg.customitem")
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new StringArgument("itemId").replaceSuggestions(ArgumentSuggestions.strings(info -> itemManager.getAllCustomItems().stream()
                                .map(CustomItem::getId).toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String itemId = (String) args.get("itemId");
                            CustomItem customItem = itemManager.getCustomItem(itemId);
                            if (customItem == null) {
                                player.sendMessage(Utils.getInstance().$("Custom item not found: " + itemId));
                                return;
                            }
                            player.getInventory().addItem(customItem.toItemStack());
                            player.sendMessage(Utils.getInstance().$("You have received a custom item: " + customItem.getId()));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.getInstance().$("Available Custom Items:"));
                            for (CustomItem item : itemManager.getAllCustomItems()) {
                                player.sendMessage(Utils.getInstance().$("- " + item.getId()));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            itemManager.loadCustomItems(); // Reload custom items
                            sender.sendMessage(Utils.getInstance().$("Custom items reloaded successfully."));
                        }))
                .register();
    }

    private void registerEventHandlers() {
        // Event handler for applying upgrade stones via InventoryClickEvent
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (event.isCancelled()) return;

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack cursorItem = event.getCursor(); // Item on the cursor (upgrade stone)
            ItemStack clickedItem = event.getCurrentItem(); // Custom item

            if (cursorItem == null || cursorItem.getType().isAir()) return;
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            DebugLogger.getInstance().log(Level.INFO, "InventoryClickEvent: Player " + player.getName() +
                    " clicked with " + cursorItem.getType() + " on " + clickedItem.getType(), 0);

            if (isUpgradeStone(cursorItem)) {
                if (CustomItemUtils.canUpgradeItem(clickedItem)) {
                    event.setCancelled(true);
                    applyUpgradeStone(player, clickedItem, cursorItem);

                    // Remove one upgrade stone from cursor
                    if (cursorItem.getAmount() > 1) {
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                    } else {
                        event.setCursor(null);
                    }

                    player.updateInventory();
                }
            }
        }, EventPriority.HIGHEST);

        // Event handler for applying upgrade stones via InventoryDragEvent
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            if (event.isCancelled()) return;

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack cursorItem = event.getOldCursor(); // Item on the cursor (upgrade stone)

            if (cursorItem == null || cursorItem.getType().isAir()) return;

            DebugLogger.getInstance().log(Level.INFO, "InventoryDragEvent: Player " + player.getName() +
                    " dragged " + cursorItem.getType(), 0);

            if (isUpgradeStone(cursorItem)) {
                for (int slot : event.getRawSlots()) {
                    ItemStack targetItem = event.getView().getItem(slot);
                    if (targetItem == null || targetItem.getType().isAir()) continue;

                    if (CustomItemUtils.canUpgradeItem(targetItem)) {
                        event.setCancelled(true);
                        applyUpgradeStone(player, targetItem, cursorItem);

                        // Remove one upgrade stone from cursor
                        if (cursorItem.getAmount() > 1) {
                            cursorItem.setAmount(cursorItem.getAmount() - 1);
                        } else {
                            player.setItemOnCursor(null);
                        }

                        player.updateInventory();
                        break;
                    }
                }
            }
        }, EventPriority.HIGHEST);
    }

    // Helper methods
    private boolean isUpgradeStone(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String itemId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        return "upgrade_stone".equals(itemId);
    }

    private void applyUpgradeStone(Player player, ItemStack itemStack, ItemStack upgradeStoneItem) {
        boolean success = CustomItemUtils.upgradeItem(itemStack, powerStoneManager);
        if (success) {
            player.sendMessage(Utils.getInstance().$("Your item has been upgraded!"));
        } else {
            player.sendMessage(Utils.getInstance().$("Failed to upgrade item."));
        }
    }
}
