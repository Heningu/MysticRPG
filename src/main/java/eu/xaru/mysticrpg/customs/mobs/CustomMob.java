package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;

public class CustomMob {

    // Fields
    private final String id;
    private final String name;
    private final EntityType entityType;
    private final double health;
    private final int level;
    private final int experienceReward;
    private final double currencyReward;
    private final Map<String, Integer> customAttributes;
    private final List<String> assignedAreas;
    private final Map<String, AreaSettings> areaSettingsMap;
    private final List<DropItem> drops;

    // New fields for base attributes
    private final double baseDamage;
    private final double baseArmor;
    private final double movementSpeed;

    // Equipment
    private final Equipment equipment;

    public CustomMob(String id, String name, EntityType entityType, double health, int level, int experienceReward,
                     double currencyReward, Map<String, Integer> customAttributes, List<String> assignedAreas,
                     Map<String, AreaSettings> areaSettingsMap, List<DropItem> drops,
                     double baseDamage, double baseArmor, double movementSpeed, Equipment equipment) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
        this.health = health;
        this.level = level;
        this.experienceReward = experienceReward;
        this.currencyReward = currencyReward;
        this.customAttributes = customAttributes;
        this.assignedAreas = assignedAreas;
        this.areaSettingsMap = areaSettingsMap;
        this.drops = drops;
        this.baseDamage = baseDamage;
        this.baseArmor = baseArmor;
        this.movementSpeed = movementSpeed;
        this.equipment = equipment;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getHealth() {
        return health;
    }

    public int getLevel() {
        return level;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public double getCurrencyReward() {
        return currencyReward;
    }

    public Map<String, Integer> getCustomAttributes() {
        return customAttributes;
    }

    public List<String> getAssignedAreas() {
        return assignedAreas;
    }

    public AreaSettings getAreaSettings(String areaName) {
        return areaSettingsMap.get(areaName);
    }

    public List<DropItem> getDrops() {
        return drops;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getBaseArmor() {
        return baseArmor;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    // Nested classes
    public static class AreaSettings {
        private final int maxAmount;
        private final int respawnAfterSeconds;
        private final boolean respawnIfAllDead;

        public AreaSettings(int maxAmount, int respawnAfterSeconds, boolean respawnIfAllDead) {
            this.maxAmount = maxAmount;
            this.respawnAfterSeconds = respawnAfterSeconds;
            this.respawnIfAllDead = respawnIfAllDead;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        public int getRespawnAfterSeconds() {
            return respawnAfterSeconds;
        }

        public boolean isRespawnIfAllDead() {
            return respawnIfAllDead;
        }
    }

    public static class DropItem {
        private final String type; // "custom_item" or "material"
        private final String id;   // For custom items
        private final String material; // For materials
        private final int amount;
        private final double chance;

        public DropItem(String type, String id, String material, int amount, double chance) {
            this.type = type;
            this.id = id;
            this.material = material;
            this.amount = amount;
            this.chance = chance;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public double getChance() {
            return chance;
        }
    }

    public static class Equipment {
        private final String helmet;
        private final String chestplate;
        private final String leggings;
        private final String boots;
        private final String weapon;

        public Equipment(String helmet, String chestplate, String leggings, String boots, String weapon) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            this.weapon = weapon;
        }

        public String getHelmet() {
            return helmet;
        }

        public String getChestplate() {
            return chestplate;
        }

        public String getLeggings() {
            return leggings;
        }

        public String getBoots() {
            return boots;
        }

        public String getWeapon() {
            return weapon;
        }
    }
}
