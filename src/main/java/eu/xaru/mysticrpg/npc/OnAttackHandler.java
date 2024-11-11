package eu.xaru.mysticrpg.npc;

import org.bukkit.entity.Player;


/**
 * Functional interface for handling NPC attacks.
 */
@FunctionalInterface
public interface OnAttackHandler {
    void handle(Player player);
}
