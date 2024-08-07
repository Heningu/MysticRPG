package eu.xaru.mysticrpg.leveling;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class LevelingManager {
    private final PlayerDataManager playerDataManager;
    private final Map<String, Integer> xpValues;
    private final Map<Integer, Integer> levelThresholds;
    private final Map<Integer, String> levelUpCommands;
    private final int maxLevel;

    public LevelingManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;

        Gson gson = new Gson();
        Type xpValuesType = new TypeToken<Map<String, Integer>>(){}.getType();
        Type levelThresholdsType = new TypeToken<Map<Integer, Integer>>(){}.getType();
        Type levelUpCommandsType = new TypeToken<Map<Integer, String>>(){}.getType();

        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/leveling/XPValues.json"))) {
            this.xpValues = gson.fromJson(reader, xpValuesType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load XPValues.json", e);
        }

        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/leveling/Levels.json"))) {
            this.levelThresholds = gson.fromJson(reader, levelThresholdsType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Levels.json", e);
        }

        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/leveling/LevelUpCommands.json"))) {
            this.levelUpCommands = gson.fromJson(reader, levelUpCommandsType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load LevelUpCommands.json", e);
        }

        this.maxLevel = levelThresholds.keySet().stream().max(Integer::compare).orElse(100);
    }

    public void addXp(Player player, int amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        if (playerData.getLevel() < maxLevel) {
            int newXp = playerData.getXp() + amount;
            playerData.setXp(newXp);

            while (playerData.getLevel() < maxLevel && newXp >= levelThresholds.get(playerData.getLevel() + 1)) {
                playerData.setLevel(playerData.getLevel() + 1);
                newXp -= levelThresholds.get(playerData.getLevel());

                // Execute level up commands
                String command = levelUpCommands.get(playerData.getLevel());
                if (command != null) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                }
            }

            playerDataManager.save(player);
        }
    }

    public void setXp(Player player, int amount) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        playerData.setXp(amount);
        playerDataManager.save(player);
    }

    public int getXp(Player player) {
        PlayerData playerData = playerDataManager.getPlayerData(player);
        return playerData.getXp();
    }

    public int getXpForEntity(String entityType) {
        return xpValues.getOrDefault(entityType, 0);
    }
}
