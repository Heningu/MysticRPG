package eu.xaru.mysticrpg.content.player;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private int skillPoints;
    private int strength;
    private int health;
    private int dexterity;
    private int luck;
    private int wisdom;
    private int toughness;
    private double critChance;
    private double critDamage;
    private double speed;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.skillPoints = 0;
        this.strength = 0;
        this.health = 20; // Default health
        this.dexterity = 0;
        this.luck = 0;
        this.wisdom = 0;
        this.toughness = 0;
        this.critChance = 0.0;
        this.critDamage = 0.0;
        this.speed = 0.2; // Default speed
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getLuck() {
        return luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public int getWisdom() {
        return wisdom;
    }

    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(int toughness) {
        this.toughness = toughness;
    }

    public double getCritChance() {
        return critChance;
    }

    public void setCritChance(double critChance) {
        this.critChance = critChance;
    }

    public double getCritDamage() {
        return critDamage;
    }

    public void setCritDamage(double critDamage) {
        this.critDamage = critDamage;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
