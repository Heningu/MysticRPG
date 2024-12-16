package eu.xaru.mysticrpg.player.stats;

import java.util.Random;

/**
 * Utility class for calculating the effects of stats.
 * Formulas can be tweaked for balance.
 */
public class StatCalculations {

    private static final Random random = new Random();

    /**
     * Defense formula example:
     * DamageReduction = Defense / (Defense + 100)
     * FinalDamage = IncomingDamage * (1 - DamageReduction)
     */
    public static double calculateDamageTaken(double incomingDamage, double defense) {
        double damageReduction = defense / (defense + 100.0);
        return incomingDamage * (1 - damageReduction);
    }

    /**
     * Strength increases damage output.
     * Example: finalDamage = baseDamage * (1 + Strength * 0.01)
     */
    public static double calculatePhysicalDamage(double baseDamage, double strength) {
        return baseDamage * (1 + (strength * 0.01));
    }

    /**
     * Intelligence could increase Mana and Magic Damage.
     * Example for magic damage: finalMagicDamage = baseMagicDamage * (1 + Intelligence * 0.01)
     */
    public static double calculateMagicDamage(double baseMagicDamage, double intelligence) {
        return baseMagicDamage * (1 + (intelligence * 0.01));
    }

    /**
     * Crit chance and damage:
     * If random < critChance%, then finalDamage = damage * (1 + critDamage * 0.01)
     */
    public static double calculateCritDamage(double damage, double critChance, double critDamage) {
        double roll = random.nextDouble() * 100.0;
        if (roll < critChance) {
            return damage * (1 + critDamage * 0.01);
        }
        return damage;
    }

    /**
     * Movement speed cap:
     * Example: max 50% increase.
     */
    public static double capMovementSpeed(double movementSpeed) {
        return Math.min(movementSpeed, 50.0); // cap at +50%
    }

    /**
     * Health regen: after a certain cooldown (managed elsewhere),
     * player regenerates HealthRegen value per second or tick.
     * Adjust logic as needed.
     */
    public static double calculateHealthRegen(double healthRegenStat) {
        return healthRegenStat; // Just return the amount per regen interval
    }

    // Attack Speed, for example, could reduce cooldown between hits.
    // If base attack cooldown is 1.0s, AttackSpeed = 20 means 20% faster = 0.8s cooldown.
    public static double calculateAttackCooldown(double baseCooldown, double attackSpeed) {
        double speedMultiplier = 1.0 - (attackSpeed * 0.01);
        // Limit not to go below 0.5s, for example
        double finalCooldown = baseCooldown * Math.max(0.5, speedMultiplier);
        return finalCooldown;
    }
}
