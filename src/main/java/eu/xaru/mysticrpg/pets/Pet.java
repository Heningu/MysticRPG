package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class Pet {

    private final String id;
    private final String name;
    private final String displayItem;
    private final String modelId;
    private final String idleAnimation;
    private final String walkAnimation;
    private final Map<String, Object> additionalStats;
    private final List<String> effects;
    private final List<String> lore;
    private final PetRarity rarity;
    private final int maxLevel;

    private int level;
    private int currentXp;

    // xpTable: nextLevel -> xpNeeded
    private final Map<Integer, Integer> xpTable;

    public Pet(String id,
               String name,
               String displayItem,
               String modelId,
               String idleAnimation,
               String walkAnimation,
               Map<String, Object> stats,
               List<String> effects,
               List<String> lore,
               PetRarity rarity,
               int maxLevel,
               int initialLevel,
               int initialXp,
               Map<Integer, Integer> xpTable) {
        this.id = id;
        this.name = name;
        this.displayItem = displayItem;
        this.modelId = modelId;
        this.idleAnimation = idleAnimation;
        this.walkAnimation = walkAnimation;
        this.additionalStats = (stats != null) ? stats : Collections.emptyMap();
        this.effects = (effects != null) ? effects : Collections.emptyList();
        this.lore = (lore != null) ? lore : Collections.emptyList();
        this.rarity = (rarity != null) ? rarity : PetRarity.COMMON;

        this.maxLevel = (maxLevel > 0) ? maxLevel : 10;
        this.level = Math.min(Math.max(initialLevel, 1), this.maxLevel);
        this.currentXp = Math.max(0, initialXp);
        this.xpTable = (xpTable != null) ? xpTable : Collections.emptyMap();
    }

    public String getId()                  { return id; }
    public String getName()                { return name; }
    public String getDisplayItem()         { return displayItem; }
    public String getModelId()             { return modelId; }
    public String getIdleAnimation()       { return idleAnimation; }
    public String getWalkAnimation()       { return walkAnimation; }
    public Map<String, Object> getAdditionalStats() { return additionalStats; }
    public List<String> getEffects()       { return effects; }
    public List<String> getLore()          { return lore; }
    public PetRarity getRarity()           { return rarity; }
    public int getMaxLevel()               { return maxLevel; }
    public int getLevel()                  { return level; }
    public int getCurrentXp()              { return currentXp; }
    public Map<Integer, Integer> getXpTable() { return xpTable; }

    public void setLevel(int newLevel) {
        this.level = Math.min(newLevel, maxLevel);
    }

    public void setCurrentXp(int xp) {
        this.currentXp = Math.max(0, xp);
    }

    public void resetXp() {
        level = 1;
        currentXp = 0;
    }

    public void addXp(int amount) {
        if (amount <= 0 || level >= maxLevel) {
            return;
        }
        currentXp += amount;

        while (level < maxLevel) {
            int nextLevel = level + 1;
            Integer xpNeeded = xpTable.get(nextLevel);
            if (xpNeeded == null) {
                DebugLogger.getInstance().log(Level.WARNING,
                        "Pet [" + id + "] xp_table missing for level " + nextLevel
                                + ". Using fallback 999999999.");
                xpNeeded = 999999999;
            }
            if (currentXp >= xpNeeded) {
                currentXp -= xpNeeded;
                level++;
                if (level >= maxLevel) {
                    level = maxLevel;
                    currentXp = 0;
                    break;
                }
            } else {
                break;
            }
        }
    }

    public int getXpToNextLevel() {
        if (level >= maxLevel) return 0;
        Integer xpNeeded = xpTable.get(level + 1);
        if (xpNeeded == null) {
            return 0;
        }
        return Math.max(xpNeeded - currentXp, 0);
    }

    /**
     * Overhead name: color-coded rarity + ownerName + petName + level
     */
    public String getFancyName(String ownerName) {
        return "[" + rarity.getColor() + rarity.name() + ChatColor.RESET + "] "
                + ownerName + "'s " + name + " [LVL" + level + "]";
    }
}
