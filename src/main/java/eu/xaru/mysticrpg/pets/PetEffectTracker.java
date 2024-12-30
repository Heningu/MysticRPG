package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.customs.items.effects.EffectRegistry;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which "pet-based" effect names each player has.
 * Then in the damage code, we check if the attacker has these effects.
 */
public class PetEffectTracker {

    private static final Map<UUID, Set<String>> petEffectsMap = new ConcurrentHashMap<>();

    public static void addEffectsToPlayer(Player player, List<String> effects) {
        Set<String> current = petEffectsMap.getOrDefault(player.getUniqueId(), new HashSet<>());
        for (String eff : effects) {
            current.add(eff.toUpperCase());
        }
        petEffectsMap.put(player.getUniqueId(), current);
    }

    public static void removeEffectsFromPlayer(Player player, List<String> effects) {
        Set<String> current = petEffectsMap.get(player.getUniqueId());
        if (current == null) return;
        for (String eff : effects) {
            current.remove(eff.toUpperCase());
        }
        if (current.isEmpty()) {
            petEffectsMap.remove(player.getUniqueId());
        } else {
            petEffectsMap.put(player.getUniqueId(), current);
        }
    }

    public static Set<String> getEffects(Player player) {
        return petEffectsMap.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public static boolean hasEffect(Player player, String effectName) {
        return getEffects(player).contains(effectName.toUpperCase());
    }
}
