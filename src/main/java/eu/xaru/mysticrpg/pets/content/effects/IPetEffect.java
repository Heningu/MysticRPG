package eu.xaru.mysticrpg.pets.content.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Unified interface for pet-based effects.
 */
public interface IPetEffect {

    /**
     * A unique string ID, e.g. "firetick", "phoenixwill".
     */
    String getId();

    /**
     * A short description for displaying in the pet’s lore.
     */
    String getDescription();

    /**
     * Called when the effect is applied (if relevant).
     * Some effects won't need logic here (like PhoenixWill’s
     * actual code might live in CustomDamageHandler).
     */
    default void apply(int petLevel, EntityDamageByEntityEvent event, Player attacker) {
        // no-op by default
    }
}
