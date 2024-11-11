package eu.xaru.mysticrpg.npc;

import org.bukkit.entity.Player;

/**
 * Functional interface for handling NPC interactions.
 */
@FunctionalInterface
public interface OnInteractHandler {
    void handle(Player player);
}
