package eu.xaru.mysticrpg.player.stats;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemUtils;
import eu.xaru.mysticrpg.customs.items.sets.SetManager;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;

public class StatsModule implements IBaseModule, Listener {

    private PlayerStatsManager statsManager;

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            DebugLogger.getInstance().severe("SaveModule not available. StatsModule cannot function.");
            return;
        }

        PlayerDataCache dataCache = PlayerDataCache.getInstance();
        statsManager = new PlayerStatsManager(dataCache);

        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getProvidingPlugin(getClass()));

        registerCommands();
        DebugLogger.getInstance().log(Level.INFO, "StatsModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "StatsModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Automatically load stats when player joins.
        Player player = event.getPlayer();
        statsManager.loadStats(player);
    }

    private void registerCommands() {
        new CommandAPICommand("showstats")
                .withPermission("mysticrpg.stats.view")
                .executesPlayer((player, args) -> {
                    PlayerStats stats = statsManager.loadStats(player);
                    player.sendMessage(ChatColor.GREEN + "=== Your Stats ===");
                    for (Map.Entry<StatType, Double> entry : stats.getAllEffectiveStats().entrySet()) {
                        player.sendMessage(ChatColor.YELLOW + entry.getKey().name() + ": " + ChatColor.WHITE + entry.getValue());
                    }
                })
                .register();

        new CommandAPICommand("addstat")
                .withPermission("mysticrpg.stats.modify")
                .withArguments(new StringArgument("stat").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    String[] names = new String[StatType.values().length];
                    for (int i = 0; i < StatType.values().length; i++) {
                        names[i] = StatType.values()[i].name();
                    }
                    return names;
                })))
                .withArguments(new IntegerArgument("amount", 1, 100))
                .executesPlayer((player, args) -> {
                    String statName = (String) args.get(0);
                    int amount = (int) args.get(1);
                    try {
                        StatType statType = StatType.valueOf(statName.toUpperCase());
                        statsManager.increaseBaseStat(player, statType, amount);
                        player.sendMessage(ChatColor.GREEN + "Increased " + statType.name() + " by " + amount);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid stat name!");
                    }
                })
                .register();

        new CommandAPICommand("fullstats")
                .withPermission("mysticrpg.stats.view")
                .executesPlayer((player, args) -> {
                    DebugLogger.getInstance().log(Level.INFO, "fullstats command used by " + player.getName());
                    PlayerStats stats = recalculatePlayerStatsFor(player); // use the returned stats with temp stats included

                    // Base Stats
                    player.sendMessage(ChatColor.GREEN + "=== Base Stats ===");
                    for (StatType type : StatType.values()) {
                        double base = stats.getBaseStat(type);
                        player.sendMessage(ChatColor.YELLOW + type.name() + ": " + ChatColor.WHITE + base);
                    }

                    // Temp Stats
                    player.sendMessage("");
                    player.sendMessage(ChatColor.BLUE + "=== Armor/Item Stats ===");
                    for (StatType type : StatType.values()) {
                        double temp = stats.getTempStat(type);
                        if (temp != 0) {
                            player.sendMessage(ChatColor.YELLOW + type.name() + ": " + ChatColor.WHITE + temp);
                        }
                    }

                    // Final Stats (Effective)
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GOLD + "=== Final Stats (Base + Armor) ===");
                    for (StatType type : StatType.values()) {
                        double eff = stats.getEffectiveStat(type);
                        player.sendMessage(ChatColor.YELLOW + type.name() + ": " + ChatColor.WHITE + eff);
                    }
                })
                .register();
    }


    /**
     * Recalculate the player's stats, applying temp stats from held non-armor items (main/off hand),
     * and applying armor stats only if actually equipped in armor slots. Also applies set bonuses properly.
     */
    public PlayerStats recalculatePlayerStatsFor(Player player) {
        PlayerStats stats = statsManager.loadStats(player);
        stats.clearTempStats();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // Apply stats from non-armor items if held in main/off hand
        applyItemAttributesIfAppropriate(mainHand, stats, false);  // false = not armor slot
        applyItemAttributesIfAppropriate(offHand, stats, false);

        int setCount = 0;
        String setId = null;

        // For armor items, only apply if they are actually in the armor slots.
        for (ItemStack piece : armor) {
            applyItemAttributesIfAppropriate(piece, stats, true); // true = armor slot
            // Check sets
            if (piece != null && CustomItemUtils.isCustomItem(piece)) {
                NamespacedKey setKey = new NamespacedKey(MysticCore.getInstance(), "custom_item_set");
                if (piece.getItemMeta() != null && piece.getItemMeta().getPersistentDataContainer().has(setKey, PersistentDataType.STRING)) {
                    String sId = piece.getItemMeta().getPersistentDataContainer().get(setKey, PersistentDataType.STRING);
                    if (sId != null) {
                        if (setId == null) {
                            setId = sId;
                            setCount = 1;
                        } else if (setId.equals(sId)) {
                            setCount++;
                        }
                    }
                }
            }
        }

        // Apply set bonuses after all items are applied
        if (setId != null) {
            var itemSet = SetManager.getInstance().getSet(setId);
            if (itemSet != null) {
                int maxThreshold = 0;
                for (Integer threshold : itemSet.getPieceBonuses().keySet()) {
                    if (setCount >= threshold && threshold > maxThreshold) {
                        maxThreshold = threshold;
                    }
                }
                if (maxThreshold > 0) {
                    Map<StatType, Double> bonuses = itemSet.getPieceBonuses().get(maxThreshold);
                    if (bonuses != null) {
                        // Now use effective stats (base + temp) before set bonus
                        // Then apply bonus as percentage of current effective stat
                        for (Map.Entry<StatType, Double> bonus : bonuses.entrySet()) {
                            double currentValue = stats.getEffectiveStat(bonus.getKey());
                            double addition = currentValue * bonus.getValue(); // e.g. 0.10 for 10%
                            stats.addTempStat(bonus.getKey(), addition);
                        }
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Applies item attributes if the item meets the criteria:
     * - If checking armor slot and item is armor, apply attributes
     * - If checking non-armor slot (main/off hand) and item is not armor, apply attributes
     */
    private void applyItemAttributesIfAppropriate(ItemStack item, PlayerStats stats, boolean isArmorSlot) {
        if (item == null || item.getType() == Material.AIR) return;
        if (!CustomItemUtils.isCustomItem(item)) {
            return;
        }

        CustomItem customItem = CustomItemUtils.fromItemStack(item);
        if (customItem == null) {
            return;
        }

        boolean isArmor = (customItem.getArmorType() != null); // If armorType is not null, it's armor
        // If it's armor, only apply if isArmorSlot == true
        // If it's not armor, only apply if isArmorSlot == false
        if (isArmor == isArmorSlot) {
            // Conditions met, apply stats
            Map<StatType, Double> itemStats = CustomItemUtils.getItemStats(item);
            for (Map.Entry<StatType, Double> entry : itemStats.entrySet()) {
                stats.addTempStat(entry.getKey(), entry.getValue());
            }
        } else {
            DebugLogger.getInstance().log(Level.INFO, "Item " + customItem.getId() + " ("+item.getType()+") is " + (isArmor?"armor":"not armor")
                    + " and isArmorSlot=" + isArmorSlot + ", not applying stats.");
        }
    }

    public PlayerStatsManager getStatsManager() {
        return statsManager;
    }
}
