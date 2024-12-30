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
import eu.xaru.mysticrpg.pets.Pet;
import eu.xaru.mysticrpg.pets.PetHelper;
import eu.xaru.mysticrpg.pets.PetsModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages the player stats system. We also re-apply pet stats in recalculatePlayerStatsFor(Player).
 */
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
                    PlayerStats stats = recalculatePlayerStatsFor(player);

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

                    // Final Stats
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GOLD + "=== Final Stats (Base + Armor + Pet) ===");
                    for (StatType type : StatType.values()) {
                        double eff = stats.getEffectiveStat(type);
                        player.sendMessage(ChatColor.YELLOW + type.name() + ": " + ChatColor.WHITE + eff);
                    }
                })
                .register();
    }

    /**
     * Recalculate the player's stats, applying:
     * 1) Clear temp stats
     * 2) Armor/Item stats
     * 3) If the player has a pet, re-apply the pet's additionalStats as well
     * 4) Return the final combined result
     */
    public PlayerStats recalculatePlayerStatsFor(Player player) {
        PlayerStats stats = statsManager.loadStats(player);
        stats.clearTempStats();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // 1) Non-armor items in main/off hand
        applyItemAttributesIfAppropriate(mainHand, stats, false);
        applyItemAttributesIfAppropriate(offHand, stats, false);

        int setCount = 0;
        String setId = null;

        // 2) Armor items in armor slots
        for (ItemStack piece : armor) {
            applyItemAttributesIfAppropriate(piece, stats, true);
            // Check sets
            if (piece != null && piece.getType() != Material.AIR) {
                if (CustomItemUtils.isCustomItem(piece)) {
                    NamespacedKey setKey = new NamespacedKey(MysticCore.getInstance(), "custom_item_set");
                    if (piece.getItemMeta() != null
                            && piece.getItemMeta().getPersistentDataContainer().has(setKey, PersistentDataType.STRING)) {

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
        }

        // 3) Apply set bonuses
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
                        for (Map.Entry<StatType, Double> bonus : bonuses.entrySet()) {
                            double currentValue = stats.getEffectiveStat(bonus.getKey());
                            double addition = currentValue * bonus.getValue();
                            stats.addTempStat(bonus.getKey(), addition);
                        }
                    }
                }
            }
        }

        // 4) Re-apply pet stats if the player has an equipped pet
        reapplyPetStats(player, stats);

        return stats;
    }

    /**
     * Applies item attributes if appropriate.
     * If isArmorSlot==true, only apply if item is armor.
     * If isArmorSlot==false, only apply if item is not armor.
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

        boolean isArmor = (customItem.getArmorType() != null);
        if (isArmor == isArmorSlot) {
            Map<StatType, Double> itemStats = CustomItemUtils.getItemStats(item);
            for (Map.Entry<StatType, Double> entry : itemStats.entrySet()) {
                stats.addTempStat(entry.getKey(), entry.getValue());
            }
        } else {
            DebugLogger.getInstance().log(Level.INFO, "Item " + customItem.getId() + " is "
                    + (isArmor ? "armor" : "not armor") + ", but isArmorSlot=" + isArmorSlot + "; skipping.");
        }
    }

    /**
     * If the player has an equipped pet, add that pet's stats to the player's temp stats.
     */
    private void reapplyPetStats(Player player, PlayerStats stats) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData data = cache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        String equippedPetId = data.getEquippedPet();
        if (equippedPetId == null || equippedPetId.isEmpty()) {
            return; // no pet
        }

        // Grab the Pet from PetHelper
        PetsModule petsModule = ModuleManager.getInstance().getModuleInstance(PetsModule.class);
        if (petsModule == null) return;

        PetHelper petHelper = petsModule.getPetHelper();
        Pet pet = petHelper.getPetById(equippedPetId);
        if (pet == null) return;

        Map<String, Object> additionalStats = pet.getAdditionalStats();
        if (additionalStats == null || additionalStats.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : additionalStats.entrySet()) {
            StatType st = parseStatType(entry.getKey());
            double val = parseDouble(entry.getValue(), 0.0);
            if (st != null && val != 0.0) {
                stats.addTempStat(st, val);
            }
        }
    }

    private StatType parseStatType(String key) {
        try {
            return StatType.valueOf(key.toUpperCase());
        } catch (Exception e) {
            DebugLogger.getInstance().warning("Invalid pet stat type: " + key);
            return null;
        }
    }

    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    public PlayerStatsManager getStatsManager() {
        return statsManager;
    }
}
