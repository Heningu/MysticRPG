package eu.xaru.mysticrpg.content.levelingsystem;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;

public class GetXP {
    private final Main plugin;

    public GetXP(Main plugin) {
        this.plugin = plugin;
    }

    public int getCurrentXP(PlayerData playerData) {
        return playerData.getXp();
    }

    public int getRequiredXP(int level) {
        LevelingSystem levelingSystem = plugin.getManagers().getLevelingSystem();
        LevelingSystem.Level levelData = levelingSystem.getLevels().get(level);
        return levelData != null ? levelData.getRequiredXp() : Integer.MAX_VALUE;
    }

    public boolean addXP(PlayerData playerData, int xp) {
        int currentXP = playerData.getXp();
        int newXP = currentXP + xp;
        playerData.setXp(newXP);

        LevelingSystem levelingSystem = plugin.getManagers().getLevelingSystem();
        while (newXP >= getRequiredXP(playerData.getLevel())) {
            levelUp(playerData);
        }

        plugin.getLocalStorage().savePlayerData(playerData);
        return true;
    }

    private void levelUp(PlayerData playerData) {
        int currentLevel = playerData.getLevel();
        int nextLevel = currentLevel + 1;

        LevelingSystem levelingSystem = plugin.getManagers().getLevelingSystem();
        LevelingSystem.Level levelData = levelingSystem.getLevels().get(nextLevel);

        if (levelData != null) {
            playerData.setLevel(nextLevel);
            playerData.setXp(playerData.getXp() - levelData.getRequiredXp());

            String rewardCommand = levelData.getRewardCommand();
            if (rewardCommand != null && !rewardCommand.isEmpty()) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), rewardCommand.replace("{player}", playerData.getUUID().toString()));
            }
        }
    }
}
