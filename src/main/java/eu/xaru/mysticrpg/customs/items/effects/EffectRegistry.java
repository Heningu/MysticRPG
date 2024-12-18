package eu.xaru.mysticrpg.customs.items.effects;

import eu.xaru.mysticrpg.utils.DebugLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EffectRegistry {
    private static final Map<String, Effect> effects = new HashMap<>();

    public static void initializeEffects() {
        // Register all effects
        registerEffect(new FieryEffect());
        registerEffect(new DeconstructEffect());
        registerEffect(new GreedEffect());
        registerEffect(new EnlightenedEffect());
        registerEffect(new QuickStrikeEffect());
        registerEffect(new BloodthirstEffect());
        registerEffect(new VenomousEffect());
    }

    public static void registerEffect(Effect effect) {
        effects.put(effect.getName().toUpperCase(), effect);
        DebugLogger.getInstance().log(Level.INFO, "Registered effect: " + effect.getName());
    }

    public static Effect getEffect(String name) {
        return effects.get(name.toUpperCase());
    }
}
