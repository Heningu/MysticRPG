package eu.xaru.mysticrpg.content.levelingsystem;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.content.modules.Module;

import java.util.HashMap;
import java.util.Map;

public class LevelingSystem implements Module {
    private final Main plugin;
    private boolean useLevelingSystem;
    private Map<Integer, Level> levels;

    public LevelingSystem(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean load() {
        loadLevelingSystem();
        return true; // Ensure it returns a boolean
    }

    @Override
    public String getName() {
        return "LevelingSystem";
    }

    private void loadLevelingSystem() {
        JsonReader jsonReader = new JsonReader();
        jsonReader.loadLevels();
        this.levels = jsonReader.getLevels();
    }

    public boolean isUseLevelingSystem() {
        return useLevelingSystem;
    }

    public Map<Integer, Level> getLevels() {
        return levels;
    }

    public static class Level {
        private int requiredXp;
        private String rewardCommand;

        public int getRequiredXp() {
            return requiredXp;
        }

        public void setRequiredXp(int requiredXp) {
            this.requiredXp = requiredXp;
        }

        public String getRewardCommand() {
            return rewardCommand;
        }

        public void setRewardCommand(String rewardCommand) {
            this.rewardCommand = rewardCommand;
        }
    }
}
