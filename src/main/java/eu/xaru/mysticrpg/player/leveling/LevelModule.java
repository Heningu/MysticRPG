package eu.xaru.mysticrpg.player.leveling;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.events.PlayerStatsChangedEvent;
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
        DebugLogger.getInstance().log(Level.INFO, "LevelModule started", 0);

        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            String inventoryTitle = event.getView().getTitle();
            if ("Leveling Menu".equals(inventoryTitle)) {
                DebugLogger.getInstance().log("Player is dragging items in the Leveling Menu.");
                event.setCancelled(true);
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
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

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

    public void addXp(Player player, int amount) {
        PlayerData playerData = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            DebugLogger.getInstance().warning("No cached data found for player: " + player.getName());
            return;
        }

        if (playerData.getLevel() < maxLevel) {
            int newXp = playerData.getXp() + amount;
            playerData.setXp(newXp);
            player.sendMessage(Utils.getInstance().$("You gained " + amount + " XP!"));

            // Level up logic
            while (playerData.getLevel() < maxLevel && newXp >= getLevelThreshold(playerData.getLevel() + 1)) {
                newXp -= getLevelThreshold(playerData.getLevel() + 1);
                playerData.setLevel(playerData.getLevel() + 1);
                playerData.setXp(newXp);

                player.sendMessage(Utils.getInstance().$("Congratulations! You reached level " + playerData.getLevel() + "!"));

                // Apply stat scaling based on new level
                applyLevelScaling(playerData);

                // Apply level up rewards
                LevelData levelData = levelDataMap.get(playerData.getLevel());
                if (levelData != null) {
                    levelData.getRewards().forEach((stat, value) -> applyReward(playerData, stat, value));
                    String command = levelData.getCommand();
                    if (command != null) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                    }
                }

                // Notify listeners
                if (levelUpListener != null) {
                    levelUpListener.onPlayerLevelUp(player);
                }

                // Stats changed, fire event
                Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
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
        playerData.getAttributes().put(stat, playerData.getAttributes().getOrDefault(stat, 0) + value);
    }

    /**
     * Applies scaling formulas to the player's attributes based on their new level.
     * For example:
     * HEALTH = 20 + level*2
     * MANA = 10 + level
     * STRENGTH = 1 + (level/2)
     * DEFENSE, CRIT_CHANCE, etc. can also be scaled.
     */
    private void applyLevelScaling(PlayerData playerData) {
        int level = playerData.getLevel();
        Map<String, Integer> attrs = playerData.getAttributes();

        // Example scaling formulas
        attrs.put("HEALTH", 20 + level * 2);
        attrs.put("MANA", 10 + level);
        attrs.put("STRENGTH", 1 + (int)(level * 0.5));
        // You can scale other attributes similarly:
        // attrs.put("DEFENSE", 0 + level);
        // attrs.put("CRIT_DAMAGE", 10 + level);
        // etc.

        // Update current HP to not exceed new max
        int currentHp = playerData.getCurrentHp();
        int maxHp = attrs.get("HEALTH");
        if (currentHp > maxHp) {
            playerData.setCurrentHp(maxHp);
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

    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }

    public interface LevelUpListener {
        void onPlayerLevelUp(Player player);
    }

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
