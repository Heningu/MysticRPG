package eu.xaru.mysticrpg.pets.content.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple registry that maps effect IDs to IPetEffect objects.
 * Pet code can query this registry for effect descriptions, etc.
 */
public final class EffectRegistry {

    private static final Map<String, IPetEffect> REGISTRY = new HashMap<>();

    static {
        // Register known effects:
        registerEffect(new FireTickEffect());
        registerEffect(new PhoenixWillEffect());
        registerEffect(new ShamanBlessingEffect());

    }

    private EffectRegistry() {}

    public static void registerEffect(IPetEffect effect) {
        REGISTRY.put(effect.getId().toLowerCase(), effect);
    }

    /**
     * Returns the effect object for the given ID, or null if not found.
     */
    public static IPetEffect get(String effectId) {
        return REGISTRY.get(effectId.toLowerCase());
    }
}
