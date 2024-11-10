package eu.xaru.mysticrpg.player.leveling;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.*;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bson.Document;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LevelModule implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private Map<Integer, LevelData> levelDataMap;
    private int maxLevel;
    private Logger logger;
    private DebugLoggerModule debugLogger;
    private LevelingMenu levelingMenu;
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() {
        logger = Bukkit.getLogger();
        debugLogger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized. LevelModule cannot function without it.");
        }
        this.playerDataCache = saveModule.getPlayerDataCache();

        // Fetch levels data from the local Levels.json file
        this.levelDataMap = loadLevelsFromFile();
        if (this.levelDataMap != null && !this.levelDataMap.isEmpty()) {
            logger.info("Levels loaded from Levels.json successfully.");
        } else {
            throw new RuntimeException("Failed to load levels from Levels.json.");
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

        // Register InventoryDragEvent for blocking dragging in leveling menus
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if ("Leveling Menu".equals(inventoryTitle)) {
                debugLogger.log("Player is dragging items in the Leveling Menu.");
                event.setCancelled(true); // Prevent item movement
            }
        });

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
        return EModulePriority.NORMAL; // Standard priority
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

    /**
     * Adds XP to the player and handles leveling up.
     *
     * @param player The player to add XP to.
     * @param amount The amount of XP to add.
     */
    public void addXp(Player player, int amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            logger.warning("No cached data found for player: " + player.getName());
            return;
        }

        if (playerData.getLevel() < maxLevel) {
            int newXp = playerData.getXp() + amount;
            playerData.setXp(newXp);

            // Send a message to the player with color codes
            player.sendMessage(ChatColor.AQUA + "You gained " + ChatColor.GOLD + amount + " XP!");

            while (playerData.getLevel() < maxLevel && newXp >= getLevelThreshold(playerData.getLevel() + 1)) {
                newXp -= getLevelThreshold(playerData.getLevel() + 1);
                playerData.setLevel(playerData.getLevel() + 1);
                playerData.setXp(newXp);

                // Send level-up message to the player
                player.sendMessage(ChatColor.GREEN + "Congratulations! You reached level " + ChatColor.GOLD + playerData.getLevel() + ChatColor.GREEN + "!");

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

            playerDataCache.savePlayerData(player.getUniqueId(), new Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    logger.log(Level.INFO, "saved");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.log(Level.SEVERE, "failed to save", throwable);
                }
            });
        }
    }

    /**
     * Sets the player's XP to a specific amount.
     *
     * @param player The player whose XP to set.
     * @param amount The amount of XP to set.
     */
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

    // Helper method to load levels from a local JSON file (Levels.json)
    private Map<Integer, LevelData> loadLevelsFromFile() {
        Map<Integer, LevelData> levels = new HashMap<>();
        try (InputStream inputStream = getClass().getResourceAsStream("/leveling/Levels.json")) {
            if (inputStream == null) {
                logger.warning("Levels.json file not found!");
                return levels;
            }

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                String jsonContent = scanner.useDelimiter("\\A").next();
                Document document = Document.parse(jsonContent);

                // Parse the levels from JSON
                for (String key : document.keySet()) {
                    int level = Integer.parseInt(key);
                    Document levelDataDoc = document.get(key, Document.class);
                    LevelData levelData = new LevelData(
                            levelDataDoc.getInteger("xp_required"),
                            levelDataDoc.getString("command"),
                            (Map<String, Integer>) levelDataDoc.get("rewards")
                    );
                    levels.put(level, levelData);
                }
            }

        } catch (Exception e) {
            logger.warning("Error reading Levels.json file: " + e.getMessage());
        }
        return levels;
    }
}
