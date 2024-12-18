package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public interface Effect {
    String getName();

    void apply(EntityDamageByEntityEvent event, Player player);

    void apply(ItemStack itemStack, Player player);

    /**
     * Returns the XP multiplier provided by this effect.
     * Default is 1.0 (no change).
     */
    default double getXpMultiplier() {
        return 1.0;
    }

    /**
     * Returns the Gold multiplier provided by this effect.
     * Default is 1.0 (no change).
     */
    default double getGoldMultiplier() {
        return 1.0;
    }
}
