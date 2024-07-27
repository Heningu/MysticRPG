package eu.xaru.mysticrpg.storage;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int coins;
    private int hp;
    private int maxHp;
    private int mana;
    private int maxMana;
    private int skillPoints;
    private int level;
    private int xp;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1; // Default level
        this.xp = 0; // Default XP
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}
