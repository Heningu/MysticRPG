package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents a single pet, including level & XP logic.
 */
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

    /**
     * xpTable says how many XP are needed to go from (level) to (level+1).
     * Key: nextLevel => xpNeeded.
     * For example, xpTable.get(2) might be 60, meaning you need 60 XP to go from L1 to L2.
     */
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
        // Clamp initial level to [1..maxLevel].
        this.level = Math.min(Math.max(initialLevel, 1), this.maxLevel);
        // XP cannot go below 0.
        this.currentXp = Math.max(0, initialXp);

        // Table can’t be null. If missing, we fallback to an empty map, meaning no further leveling.
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

    /**
     * Adds XP to this pet's "currentXp" and checks if it crosses the threshold for next level(s).
     * - If we have leftover XP, we keep leveling up until we either run out of leftover or hit maxLevel.
     * - If we EXACTLY match a threshold, we level up and set xp=0 for the new level.
     */
    public void addXp(int amount) {
        // If no XP added or already at max level, do nothing.
        if (amount <= 0 || level >= maxLevel) {
            return;
        }

        currentXp += amount;

        // Keep leveling up as long as we cross the threshold for the next level
        while (level < maxLevel) {
            int nextLevel = level + 1;
            Integer xpNeeded = xpTable.get(nextLevel);
            if (xpNeeded == null) {
                // If next level is not in xpTable, we can't proceed
                DebugLogger.getInstance().log(Level.WARNING,
                        "Pet [" + id + "] xp_table missing for level " + nextLevel
                                + ". Using fallback 999999999 => no further leveling possible!");
                xpNeeded = 999999999;
            }

            // If we have enough XP to go up at least 1 level:
            if (currentXp >= xpNeeded) {
                // Subtract what's needed to reach the new level
                currentXp -= xpNeeded;
                level++;

                // If we are now at max level, clamp XP to 0
                if (level >= maxLevel) {
                    level = maxLevel;
                    currentXp = 0;
                    break;
                }
            } else {
                // Not enough XP for the next level => done leveling for now
                break;
            }
        }
    }

    /**
     * @return How many more XP are needed from currentXp to reach (level+1).
     *         Returns 0 if pet is at max level or if xp_table is missing that next level.
     */
    public int getXpToNextLevel() {
        if (level >= maxLevel) {
            return 0;
        }
        Integer xpNeeded = xpTable.get(level + 1);
        if (xpNeeded == null) {
            return 0;
        }
        return Math.max(xpNeeded - currentXp, 0);
    }

    /**
     * The overhead name: color-coded rarity + ownerName + petName + level
     */
    public String getFancyName(String ownerName) {
        return "[" + rarity.getColor() + rarity.name() + ChatColor.RESET + "] "
                + ownerName + "'s " + name + " [LVL" + level + "]";
    }
}
