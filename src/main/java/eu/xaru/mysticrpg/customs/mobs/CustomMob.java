package eu.xaru.mysticrpg.customs.mobs;

import eu.xaru.mysticrpg.customs.mobs.actions.Action;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
    private AnimationConfig animationConfig;
    private BossBarConfig bossBarConfig;



    // New fields for base attributes
    private final double baseDamage;
    private final double baseArmor;
    private final double movementSpeed;

    // Equipment
    private final Equipment equipment;

    // New field for model ID
    private final String modelId;

    // New field for actions
    private final Map<String, List<Action>> actions;

    public CustomMob(String id, String name, EntityType entityType, double health, int level, int experienceReward,
                     double currencyReward, Map<String, Integer> customAttributes, List<String> assignedAreas,
                     Map<String, AreaSettings> areaSettingsMap, List<DropItem> drops,
                     double baseDamage, double baseArmor, double movementSpeed, Equipment equipment,
                     String modelId, Map<String, List<Action>> actions, AnimationConfig animationConfig, BossBarConfig bossBarConfig) {
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
        this.modelId = modelId;
        this.actions = actions;
        this.animationConfig = animationConfig != null ? animationConfig : new AnimationConfig();
        this.bossBarConfig = bossBarConfig;


    }

    // Getter and Setter for bossBarConfig
    public BossBarConfig getBossBarConfig() {
        return bossBarConfig;
    }

    public void setBossBarConfig(BossBarConfig bossBarConfig) {
        this.bossBarConfig = bossBarConfig;
    }

    public AnimationConfig getAnimationConfig() {
        return animationConfig;
    }

    public void setAnimationConfig(AnimationConfig animationConfig) {
        this.animationConfig = animationConfig;
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

    public String getModelId() {
        return modelId;
    }

    public Map<String, List<Action>> getActions() {
        return actions;
    }

    // Inner class for BossBar configuration
    public static class BossBarConfig {
        private boolean enabled;
        private String title;
        private BarColor color;
        private double range;
        private BarStyle style;

        public BossBarConfig(boolean enabled, String title, BarColor color, double range, BarStyle style) {
            this.enabled = enabled;
            this.title = title;
            this.color = color;
            this.range = range;
            this.style = style;
        }

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public String getTitle() {
            return title;
        }

        public BarColor getColor() {
            return color;
        }

        public double getRange() {
            return range;
        }

        public BarStyle getStyle() {
            return style;
        }
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