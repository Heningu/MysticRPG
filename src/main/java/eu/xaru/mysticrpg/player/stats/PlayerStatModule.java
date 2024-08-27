package eu.xaru.mysticrpg.player.stats;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerStatModule implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private DebugLoggerModule logger;
    private StatMenu statMenu;
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        MysticCore plugin = JavaPlugin.getPlugin(MysticCore.class);

        registerStatsCommand();

        if (saveModule != null) {
            playerDataCache = saveModule.getPlayerDataCache();
        } else {
            logger.error("SaveModule not initialized. PlayerStatModule cannot function without it.");
            return;
        }

        if (playerDataCache == null) {
            logger.error("PlayerDataCache not initialized. PlayerStatModule cannot function without it.");
            return;
        }

        statMenu = new StatMenu(plugin);

        logger.log(Level.INFO, "PlayerStatModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "PlayerStatModule started", 0);

        // Register InventoryClickEvent for StatMenu
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null) return;

            String inventoryTitle = event.getView().getTitle();
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            // Handle clicks in the Player Stats menu
            if ("Player Stats".equals(inventoryTitle)) {
                logger.log("Player " + player.getName() + " clicked in the Player Stats menu.");
                if (displayName.startsWith("Increase ")) {
                    logger.log("Passing attribute name to StatManager: " + displayName);
                    increaseAttribute(player, displayName);
                    statMenu.openStatMenu(player); // Refresh the inventory to show updated stats
                }
                event.setCancelled(true); // Prevent item movement
            }
        });

        // Register InventoryDragEvent for StatMenu
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if ("Player Stats".equals(inventoryTitle)) {
                logger.log("Player is dragging items in the Player Stats menu.");
                event.setCancelled(true); // Prevent item movement
            }
        });
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "PlayerStatModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "PlayerStatModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    // Increase player attribute
    public void increaseAttribute(Player player, String attributeName) {
        UUID playerUUID = player.getUniqueId();
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);

        if (data == null) {
            logger.error("No cached data found for player: " + player.getName());
            return;
        }

        if (data.getAttributePoints() <= 0) {
            logger.log(Level.INFO, "Player " + player.getName() + " has no attribute points.", 0);
            return;
        }

        // Get the current attributes map
        Map<String, Integer> attributes = new HashMap<>(data.getAttributes());

        switch (attributeName) {
            case "Increase Vitality":
                attributes.put("Vitality", attributes.get("Vitality") + 1);
                attributes.put("HP", attributes.get("HP") + 2);
                logger.log(Level.INFO, "Player " + player.getName() + " increased Vitality to " + attributes.get("Vitality"), 0);
                break;
            case "Increase Intelligence":
                attributes.put("Intelligence", attributes.get("Intelligence") + 1);
                attributes.put("MANA", attributes.get("MANA") + 2);
                logger.log(Level.INFO, "Player " + player.getName() + " increased Intelligence to " + attributes.get("Intelligence"), 0);
                break;
            case "Increase Dexterity":
                attributes.put("Dexterity", attributes.get("Dexterity") + 1);
                attributes.put("AttackDamageDex", attributes.getOrDefault("AttackDamageDex", 0) + 1);
                logger.log(Level.INFO, "Player " + player.getName() + " increased Dexterity to " + attributes.get("Dexterity"), 0);
                break;
            case "Increase Strength":
                attributes.put("Strength", attributes.get("Strength") + 1);
                attributes.put("AttackDamage", attributes.getOrDefault("AttackDamage", 0) + 1);
                logger.log(Level.INFO, "Player " + player.getName() + " increased Strength to " + attributes.get("Strength"), 0);
                break;
            default:
                logger.error("Unknown attribute name: " + attributeName);
                return;
        }

        // Update attributes and attribute points
        data.setAttributes(attributes);
        data.setAttributePoints(data.getAttributePoints() - 1);
        logger.log(Level.INFO, "Player " + player.getName() + " now has " + data.getAttributePoints() + " attribute points left.", 0);
    }

    private void registerStatsCommand() {
        new CommandAPICommand("stats")
                .withAliases("statmenu")
                .withPermission("mysticrpg.stats")
                .withSubcommand(new CommandAPICommand("reset")
                        .executesPlayer((player, args) -> {
                            PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());

                            if (playerData == null) {
                                player.sendMessage(ChatColor.RED + "No cached data found for you.");
                                logger.error("No cached data found for player: " + player.getName());
                                return;
                            }

                            resetStats(playerData);
                            player.sendMessage(ChatColor.GREEN + "Your stats have been reset.");
                            logger.log("Player " + player.getName() + "'s stats have been reset.");
                        }))
                .executesPlayer((player, args) -> {
                    statMenu.openStatMenu(player); // Open the stats menu for the player
                })
                .register();
    }

    private void resetStats(PlayerData data) {
        Map<String, Integer> attributes = new HashMap<>();
        attributes.put("Vitality", 1);
        attributes.put("Intelligence", 1);
        attributes.put("Dexterity", 1);
        attributes.put("Strength", 1);
        attributes.put("HP", 20);
        attributes.put("MANA", 20);
        data.setAttributes(attributes);
        data.setAttributePoints(1);
    }
}
