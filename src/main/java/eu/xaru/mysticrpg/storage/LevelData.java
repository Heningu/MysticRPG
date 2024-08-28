package eu.xaru.mysticrpg.storage;

import java.util.Map;

public class LevelData {
    private int xpRequired;
    private String command;
    private Map<String, Integer> rewards;
    private boolean special;

    public LevelData(int xpRequired, String command, Map<String, Integer> rewards, boolean special) {
        this.xpRequired = xpRequired;
        this.command = command;
        this.rewards = rewards;
        this.special = special;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, Integer> getRewards() {
        return rewards;
    }

    public boolean isSpecial() {
        return special;
    }
}
