package eu.xaru.mysticrpg.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private final File dataFile;
    private final FileConfiguration dataConfig;
    private final Map<String, Integer> attributes = new HashMap<>();
    private double balance;
    private int xp;
    private int level;
    private int nextLevelXP;

    public PlayerData(File dataFile) {
        this.dataFile = dataFile;
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    public void load() {
        balance = dataConfig.getDouble("balance", 0);
        xp = dataConfig.getInt("xp", 0);
        level = dataConfig.getInt("level", 1);
        nextLevelXP = dataConfig.getInt("nextLevelXP", 100);

        // Load attributes
        if (dataConfig.contains("attributes")) {
            for (String key : dataConfig.getConfigurationSection("attributes").getKeys(false)) {
                attributes.put(key, dataConfig.getInt("attributes." + key, 1));
            }
        }
    }

    public void save() {
        save(this.dataFile);
    }

    public void save(File file) {
        dataConfig.set("balance", balance);
        dataConfig.set("xp", xp);
        dataConfig.set("level", level);
        dataConfig.set("nextLevelXP", nextLevelXP);

        // Save attributes
        for (Map.Entry<String, Integer> entry : attributes.entrySet()) {
            dataConfig.set("attributes." + entry.getKey(), entry.getValue());
        }

        try {
            dataConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter and Setter methods for balance, xp, level, nextLevelXP, and attributes

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getNextLevelXP() {
        return nextLevelXP;
    }

    public void setNextLevelXP(int nextLevelXP) {
        this.nextLevelXP = nextLevelXP;
    }

    public int getAttribute(String key) {
        return attributes.getOrDefault(key, 1);
    }

    public void setAttribute(String key, int value) {
        attributes.put(key, value);
    }

    // Specific attribute getters and setters

    public int getHp() {
        return getAttribute("HP");
    }

    public void setHp(int hp) {
        setAttribute("HP", hp);
    }

    public int getMana() {
        return getAttribute("MANA");
    }

    public void setMana(int mana) {
        setAttribute("MANA", mana);
    }

    public int getLuck() {
        return getAttribute("LUCK");
    }

    public void setLuck(int luck) {
        setAttribute("LUCK", luck);
    }

    public int getAttackDamage() {
        return getAttribute("ATTACK_DAMAGE");
    }

    public void setAttackDamage(int attackDamage) {
        setAttribute("ATTACK_DAMAGE", attackDamage);
    }

    public int getToughness() {
        return getAttribute("TOUGHNESS");
    }

    public void setToughness(int toughness) {
        setAttribute("TOUGHNESS", toughness);
    }

    public int getAttackDamageDex() {
        return getAttribute("ATTACK_DAMAGE_DEX");
    }

    public void setAttackDamageDex(int attackDamageDex) {
        setAttribute("ATTACK_DAMAGE_DEX", attackDamageDex);
    }

    public int getAttackDamageMana() {
        return getAttribute("ATTACK_DAMAGE_MANA");
    }

    public void setAttackDamageMana(int attackDamageMana) {
        setAttribute("ATTACK_DAMAGE_MANA", attackDamageMana);
    }

    // Stat attributes

    public int getVitality() {
        return getAttribute("Vitality");
    }

    public void setVitality(int vitality) {
        setAttribute("Vitality", vitality);
    }

    public int getIntelligence() {
        return getAttribute("Intelligence");
    }

    public void setIntelligence(int intelligence) {
        setAttribute("Intelligence", intelligence);
    }

    public int getDexterity() {
        return getAttribute("Dexterity");
    }

    public void setDexterity(int dexterity) {
        setAttribute("Dexterity", dexterity);
    }

    public int getStrength() {
        return getAttribute("Strength");
    }

    public void setStrength(int strength) {
        setAttribute("Strength", strength);
    }
    public int getAttributePoints() {
        return getAttribute("ATTRIBUTE_POINTS");
    }

    public void setAttributePoints(int points) {
        setAttribute("ATTRIBUTE_POINTS", points);
    }
}
