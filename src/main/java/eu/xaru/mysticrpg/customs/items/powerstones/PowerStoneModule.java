package eu.xaru.mysticrpg.customs.items.powerstones;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.customs.items.effects.Effect;
import eu.xaru.mysticrpg.customs.items.effects.EffectRegistry; // Import the EffectRegistry
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public class PowerStoneModule implements IBaseModule {

    private EventManager eventManager;
    private PowerStoneManager powerStoneManager;

    private JavaPlugin plugin;

    @Override
    public void initialize() {
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        eventManager = new EventManager(plugin);
        powerStoneManager = new PowerStoneManager();

        // Initialize and register all effects
        EffectRegistry.initializeEffects(); // <-- Registering effects here

        registerCommands();
        registerEventHandlers();

        DebugLogger.getInstance().log(Level.INFO, "PowerStoneModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "PowerStoneModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "PowerStoneModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "PowerStoneModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public PowerStoneManager getPowerStoneManager() {
        return powerStoneManager;
    }

    private void registerCommands() {
        new CommandAPICommand("powerstone")
                .withPermission("mysticrpg.powerstone")
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new StringArgument("powerStoneId").replaceSuggestions(ArgumentSuggestions.strings(info -> powerStoneManager.getAllPowerStones().keySet().toArray(new String[0]))))
                        .executesPlayer((player, args) -> {
                            String powerStoneId = (String) args.get("powerStoneId");

                            PowerStone powerStone = powerStoneManager.getPowerStone(powerStoneId);
                            if (powerStone == null) {
                                player.sendMessage(Utils.getInstance().$("Power stone not found: " + powerStoneId));
                                return;
                            }

                            ItemStack powerStoneItem = powerStone.toItemStack();
                            powerStoneItem.setAmount(1);
                            player.getInventory().addItem(powerStoneItem);
                            player.sendMessage(Utils.getInstance().$("You have received " + powerStone.getName()));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executes((player, args) -> {
                            player.sendMessage(Utils.getInstance().$("Available Power Stones:"));
                            for (PowerStone ps : powerStoneManager.getAllPowerStones().values()) {
                                player.sendMessage(Utils.getInstance().$("- " + (ps.getName() + " (" + ps.getId() + ")")));
                            }
                        }))
                .register();
    }

    private void registerEventHandlers() {
        // InventoryClickEvent handler
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if (event.isCancelled()) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack cursorItem = event.getCursor(); // Power stone
            ItemStack clickedItem = event.getCurrentItem(); // Custom item

            if (cursorItem == null || cursorItem.getType().isAir()) return;
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            DebugLogger.getInstance().log(Level.INFO, "InventoryClickEvent: Player " + player.getName() +
                    " clicked with " + cursorItem.getType() + " on " + clickedItem.getType(), 0);

            if (isPowerStone(cursorItem)) {
                PowerStone powerStone = getPowerStone(cursorItem);
                if (powerStone == null) return;

                if (powerStone.getId().equalsIgnoreCase("deconstruct_stone")) {
                    // Handle deconstruct stone
                    if (CustomItemUtils.isCustomItem(clickedItem)) {
                        event.setCancelled(true);
                        boolean success = CustomItemUtils.deconstructItem(clickedItem, powerStoneManager);
                        if (success) {
                            player.sendMessage(Utils.getInstance().$("All power stones have been removed from your item."));
                            // Remove one deconstruct stone from cursor
                            if (cursorItem.getAmount() > 1) {
                                cursorItem.setAmount(cursorItem.getAmount() - 1);
                            } else {
                                event.setCursor(null);
                            }
                            player.updateInventory();
                        } else {
                            player.sendMessage(Utils.getInstance().$("Failed to deconstruct item."));
                        }
                    }
                } else {
                    // Handle regular power stones
                    if (CustomItemUtils.canApplyPowerStone(clickedItem)) {
                        event.setCancelled(true);
                        applyPowerStone(player, clickedItem, powerStone);

                        // Remove one power stone from cursor
                        if (cursorItem.getAmount() > 1) {
                            cursorItem.setAmount(cursorItem.getAmount() - 1);
                        } else {
                            event.setCursor(null);
                        }

                        player.updateInventory();
                    }
                }
            }
        }, EventPriority.HIGHEST);

        // InventoryDragEvent handler
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            if (event.isCancelled()) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack cursorItem = event.getOldCursor(); // Power stone

            if (cursorItem == null || cursorItem.getType().isAir()) return;

            DebugLogger.getInstance().log(Level.INFO, "InventoryDragEvent: Player " + player.getName() +
                    " dragged " + cursorItem.getType(), 0);

            if (isPowerStone(cursorItem)) {
                PowerStone powerStone = getPowerStone(cursorItem);
                if (powerStone == null) return;

                if (powerStone.getId().equalsIgnoreCase("deconstruct_stone")) {
                    // Handle deconstruct stone
                    for (int slot : event.getRawSlots()) {
                        ItemStack targetItem = event.getView().getItem(slot);
                        if (targetItem == null || targetItem.getType().isAir()) continue;

                        if (CustomItemUtils.isCustomItem(targetItem)) {
                            event.setCancelled(true);
                            boolean success = CustomItemUtils.deconstructItem(targetItem, powerStoneManager);
                            if (success) {
                                player.sendMessage(Utils.getInstance().$("All power stones have been removed from your item."));
                                // Remove one deconstruct stone from cursor
                                if (cursorItem.getAmount() > 1) {
                                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                                } else {
                                    player.setItemOnCursor(null);
                                }
                                player.updateInventory();
                                break;
                            } else {
                                player.sendMessage(Utils.getInstance().$("Failed to deconstruct item."));
                            }
                        }
                    }
                } else {
                    // Handle regular power stones
                    for (int slot : event.getRawSlots()) {
                        ItemStack targetItem = event.getView().getItem(slot);
                        if (targetItem == null || targetItem.getType().isAir()) continue;

                        if (CustomItemUtils.canApplyPowerStone(targetItem)) {
                            event.setCancelled(true);
                            applyPowerStone(player, targetItem, powerStone);

                            // Remove one power stone from cursor
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
            }
        }, EventPriority.HIGHEST);

        // EntityDamageByEntityEvent handler
        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            if (!(event.getDamager() instanceof Player)) return;

            Player player = (Player) event.getDamager();
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon == null || weapon.getType().isAir()) return;

            ItemMeta meta = weapon.getItemMeta();
            if (meta == null) return;

            NamespacedKey key = new NamespacedKey(plugin, "applied_power_stones");
            String appliedStones = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (appliedStones == null || appliedStones.isEmpty()) return;

            Set<String> powerStoneIds = new HashSet<>(Arrays.asList(appliedStones.split(",")));

            for (String psId : powerStoneIds) {
                PowerStone ps = powerStoneManager.getPowerStone(psId);
                if (ps != null) {
                    Effect effect = powerStoneManager.getEffect(ps.getEffect());
                    if (effect != null) {
                        effect.apply(event, player);
                    }
                }
            }
        });
    }

    private boolean isPowerStone(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey key = new NamespacedKey(plugin, "power_stone_id");
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    private PowerStone getPowerStone(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(plugin, "power_stone_id");
        String powerStoneId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (powerStoneId == null) return null;

        return powerStoneManager.getPowerStone(powerStoneId);
    }

    private void applyPowerStone(Player player, ItemStack itemStack, PowerStone powerStone) {
        boolean success = CustomItemUtils.applyPowerStoneToItem(itemStack, powerStone, powerStoneManager);
        if (success) {
            player.sendMessage(Utils.getInstance().$("Applied " + powerStone.getName()) + " to your item.");
        } else {
            player.sendMessage(Utils.getInstance().$("Failed to apply power stone."));
        }
    }
}
