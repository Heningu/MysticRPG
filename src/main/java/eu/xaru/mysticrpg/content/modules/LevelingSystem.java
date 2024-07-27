package eu.xaru.mysticrpg.content.modules;

import eu.xaru.mysticrpg.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class LevelingSystem implements Module {
    private final Main plugin;
    private boolean useLevelingSystem;
    private Map<String, Level> levels;

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
        ConfigurationSection config = plugin.getConfigManager().getConfig().getConfigurationSection("leveling");
        if (config != null) {
            useLevelingSystem = config.getBoolean("use_leveling_system", true);
            levels = new HashMap<>();
            for (String key : config.getKeys(false)) {
                ConfigurationSection levelSection = config.getConfigurationSection(key);
                if (levelSection != null) {
                    Level level = new Level();
                    level.setXp(levelSection.getInt("xp", 0));
                    level.setHp(levelSection.getInt("hp", 10));
                    level.setMana(levelSection.getInt("mana", 10));
                    levels.put(key, level);
                }
            }
        }
    }

    public boolean isUseLevelingSystem() {
        return useLevelingSystem;
    }

    public Map<String, Level> getLevels() {
        return levels;
    }

    public static class Level {
        private int xp;
        private int hp;
        private int mana;

        public int getXp() {
            return xp;
        }

        public void setXp(int xp) {
            this.xp = xp;
        }

        public int getHp() {
            return hp;
        }

        public void setHp(int hp) {
            this.hp = hp;
        }

        public int getMana() {
            return mana;
        }

        public void setMana(int mana) {
            this.mana = mana;
        }
    }
}
