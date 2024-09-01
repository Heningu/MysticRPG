package eu.xaru.mysticrpg.player.leveling;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.LevelData;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LevelModule implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private Map<Integer, LevelData> levelDataMap;
    private int maxLevel;
    private Logger logger;
    private DebugLoggerModule debugLogger;
    private LevelingMenu levelingMenu;

    @Override
    public void initialize() {
        logger = Bukkit.getLogger();
        debugLogger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized. LevelModule cannot function without it.");
        }
        this.playerDataCache = saveModule.getPlayerDataCache();

        // Fetch levels data from MongoDB using SaveHelper
        Map<Integer, LevelData> loadedLevels = saveModule.getSaveHelper().fetchLevels();
        if (loadedLevels != null) {
            this.levelDataMap = loadedLevels;
            logger.info("Levels loaded from database successfully.");
        } else {
            throw new RuntimeException("Failed to load levels from database.");
        }

        this.maxLevel = levelDataMap.keySet().stream().max(Integer::compare).orElse(100);

        registerLevelsCommand();
        debugLogger.log(Level.INFO, "LevelModule initialized", 0);
    }

    @Override
    public void start() {
        // Initialize LevelingMenu here instead of in initialize method
        this.levelingMenu = new LevelingMenu(JavaPlugin.getPlugin(MysticCore.class));

        debugLogger.log(Level.INFO, "LevelModule started", 0);
    }

    @Override
    public void stop() {
        debugLogger.log(Level.INFO, "LevelModule stopped", 0);
    }

    @Override
    public void unload() {
        debugLogger.log(Level.INFO, "LevelModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, SaveModule.class); // Dependencies for this module
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW; // Standard priority
    }

    private void registerLevelsCommand() {
        new CommandAPICommand("levels")
                .withPermission("mysticrpg.levels")
                .executesPlayer((player, args) -> {
                    levelingMenu.openLevelingMenu(player, 1);
                })
                .withSubcommand(new CommandAPICommand("give")
                        .withPermission("mysticrpg.admin")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) args.get("amount");
                            addXp(target, amount);
                            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " XP to " + target.getName());
                            target.sendMessage(ChatColor.GREEN + "You received " + amount + " XP.");
                        }))
                .withSubcommand(new CommandAPICommand("set")
                        .withPermission("mysticrpg.admin")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) args.get("amount");
                            setXp(target, amount);
                            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s XP to " + amount);
                            target.sendMessage(ChatColor.GREEN + "Your XP was set to " + amount);
                        }))
                .register();
    }

    public void addXp(Player player, int amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            logger.warning("No cached data found for player: " + player.getName());
            return;
        }

        if (playerData.getLevel() < maxLevel) {
            int newXp = playerData.getXp() + amount;
            playerData.setXp(newXp);

            while (playerData.getLevel() < maxLevel && newXp >= getLevelThreshold(playerData.getLevel() + 1)) {
                newXp -= getLevelThreshold(playerData.getLevel() + 1);
                playerData.setLevel(playerData.getLevel() + 1);
                playerData.setXp(newXp);

                // Apply level up rewards
                LevelData levelData = levelDataMap.get(playerData.getLevel());
                if (levelData != null) {
                    levelData.getRewards().forEach((stat, value) -> applyReward(playerData, stat, value));

                    // Execute level up commands
                    String command = levelData.getCommand();
                    if (command != null) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                    }
                }
            }

            playerDataCache.savePlayerData(player.getUniqueId(), null);
        }
    }

    public void setXp(Player player, int amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            logger.warning("No cached data found for player: " + player.getName());
            return;
        }

        playerData.setXp(amount);
        playerDataCache.savePlayerData(player.getUniqueId(), null);
    }

    private void applyReward(PlayerData playerData, String stat, int value) {
        switch (stat) {
            case "HP":
                playerData.getAttributes().put("HP", playerData.getAttributes().getOrDefault("HP", 0) + value);
                break;
            case "Strength":
                playerData.getAttributes().put("Strength", playerData.getAttributes().getOrDefault("Strength", 0) + value);
                break;
            case "AttributePoints":
                playerData.setAttributePoints(playerData.getAttributePoints() + value);
                break;
            // Add other stats and attributes here as needed
            default:
                playerData.getAttributes().put(stat, playerData.getAttributes().getOrDefault(stat, 0) + value);
                break;
        }
    }

    public int getLevelThreshold(int level) {
        LevelData levelData = levelDataMap.get(level);
        return (levelData != null) ? levelData.getXpRequired() : 0;
    }

    public boolean isSpecialLevel(int level) {
        LevelData levelData = levelDataMap.get(level);
        return levelData != null && levelData.isSpecial();
    }

    public LevelData getLevelData(int level) {
        return levelDataMap.get(level);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<String, Integer> getLevelRewards(int level) {
        LevelData levelData = levelDataMap.get(level);
        return levelData != null ? levelData.getRewards() : Collections.emptyMap();
    }
}
