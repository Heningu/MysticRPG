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
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
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

/**
 * LevelModule handles player leveling up, XP management, and level-up rewards.
 */
public class LevelModule implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private Map<Integer, LevelData> levelDataMap;
    private int maxLevel;
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));
    private LevelUpListener levelUpListener;

    @Override
    public void initialize() {

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized. LevelModule cannot function without it.");
        }
        this.playerDataCache = PlayerDataCache.getInstance();

        // Fetch levels data from the local Levels.json file
        this.levelDataMap = loadLevelsFromFile();
        if (this.levelDataMap != null && !this.levelDataMap.isEmpty()) {
            DebugLogger.getInstance().log("Levels loaded from Levels.json successfully.");
        } else {
            throw new RuntimeException("Failed to load levels from Levels.json.");
        }

        this.maxLevel = levelDataMap.keySet().stream().max(Integer::compare).orElse(100);

        registerLevelsCommand();
        DebugLogger.getInstance().log(Level.INFO, "LevelModule initialized", 0);
    }

    @Override
    public void start() {
        // Initialize LevelingMenu here instead of in initialize method
        DebugLogger.getInstance().log(Level.INFO, "LevelModule started", 0);

        // Register InventoryDragEvent for blocking dragging in leveling menus
        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if ("Leveling Menu".equals(inventoryTitle)) {
                DebugLogger.getInstance().log("Player is dragging items in the Leveling Menu.");
                event.setCancelled(true); // Prevent item movement
            }
        });
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "LevelModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "LevelModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( SaveModule.class); // Dependencies for this module
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL; // Standard priority
    }

    /**
     * Registers the /levels commands using CommandAPI.
     */
    private void registerLevelsCommand() {
        new CommandAPICommand("leveling")
                .withSubcommand(new CommandAPICommand("give")
                        .withPermission("mysticrpg.adminlevels")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) args.get("amount");
                            addXp(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Gave " + amount + " XP to " + target.getName()));
                            target.sendMessage(Utils.getInstance().$("You received " + amount + " XP."));
                        }))
                .withSubcommand(new CommandAPICommand("set")
                        .withPermission("mysticrpg.adminlevels")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            int amount = (int) args.get("amount");
                            setXp(target, amount);
                            sender.sendMessage(Utils.getInstance().$("Set " + target.getName() + "'s XP to " + amount));
                            target.sendMessage(Utils.getInstance().$("Your XP was set to " + amount));
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
            DebugLogger.getInstance().warning("No cached data found for player: " + player.getName());
            return;
        }

        if (playerData.getLevel() < maxLevel) {
            int newXp = playerData.getXp() + amount;
            playerData.setXp(newXp);

            // Send a message to the player with color codes
            player.sendMessage(Utils.getInstance().$("You gained " + + amount + " XP!"));

            while (playerData.getLevel() < maxLevel && newXp >= getLevelThreshold(playerData.getLevel() + 1)) {
                newXp -= getLevelThreshold(playerData.getLevel() + 1);
                playerData.setLevel(playerData.getLevel() + 1);
                playerData.setXp(newXp);

                // Send level-up message to the player
                player.sendMessage(Utils.getInstance().$("Congratulations! You reached level "  + playerData.getLevel()  + "!"));

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

                // Notify listeners about the level up
                if (levelUpListener != null) {
                    levelUpListener.onPlayerLevelUp(player);
                }
            }

            playerDataCache.savePlayerData(player.getUniqueId(), new Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    DebugLogger.getInstance().log(Level.INFO, "Saved XP for player " + player.getName());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().log(Level.SEVERE, "Failed to save XP for player " + player.getName(), throwable);
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
            DebugLogger.getInstance().warning("No cached data found for player: " + player.getName());
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

    /**
     * Sets the LevelUpListener.
     *
     * @param listener The listener to set.
     */
    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }

    /**
     * Interface for listening to player level-up events.
     */
    public interface LevelUpListener {
        void onPlayerLevelUp(Player player);
    }

    // Helper method to load levels from a local JSON file (Levels.json)
    private Map<Integer, LevelData> loadLevelsFromFile() {
        Map<Integer, LevelData> levels = new HashMap<>();
        try (InputStream inputStream = getClass().getResourceAsStream("/leveling/Levels.json")) {
            if (inputStream == null) {
                DebugLogger.getInstance().warning("Levels.json file not found!");
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
            DebugLogger.getInstance().warning("Error reading Levels.json file:", e);
        }
        return levels;
    }
}
