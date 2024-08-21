package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.inventory.ItemStack;

public class MobConfiguration {
    private final String type;
    private final String name;
    private final int level;
    private final double health;
    private final double damage;
    private final double speed;
    private final double knockbackResistance;
    private final ItemStack helmet;
    private final ItemStack chestplate;
    private final ItemStack leggings;
    private final ItemStack boots;
    private final ItemStack weapon;
    private final ItemStack head;

    public MobConfiguration(String type, String name, int level, double health, double damage, double speed, double knockbackResistance,
                            ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots, ItemStack weapon, ItemStack head) {
        this.type = type;
        this.name = name;
        this.level = level;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.knockbackResistance = knockbackResistance;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        this.weapon = weapon;
        this.head = head;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public double getHealth() {
        return health;
    }

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public double getKnockbackResistance() {
        return knockbackResistance;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public ItemStack getWeapon() {
        return weapon;
    }

    public ItemStack getHead() {
        return head;
    }
}
