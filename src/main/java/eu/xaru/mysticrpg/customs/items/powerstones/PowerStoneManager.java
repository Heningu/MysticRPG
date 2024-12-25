package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.customs.items.effects.DeconstructEffect;
import eu.xaru.mysticrpg.customs.items.effects.Effect;
import eu.xaru.mysticrpg.customs.items.effects.FieryEffect;
import eu.xaru.mysticrpg.customs.items.effects.GreedEffect;   // Import GreedEffect
import eu.xaru.mysticrpg.customs.items.effects.EnlightenedEffect; // Import EnlightenedEffect if you have it
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class PowerStoneManager {

    private final Map<String, PowerStone> powerStones = new HashMap<>();
    private final Map<String, Effect> effects = new HashMap<>();

    private final JavaPlugin plugin;

    public PowerStoneManager() {
        plugin = JavaPlugin.getPlugin(MysticCore.class);
        // Initialize effects
        initializeEffects();
        // Load power stones from YAML files
        loadPowerStones();
    }

    private void initializeEffects() {
        // Register default effects
        registerEffect(new FieryEffect());
        registerEffect(new DeconstructEffect());

        // Register your GREED and ENLIGHTENED effects here
        registerEffect(new GreedEffect());
        registerEffect(new EnlightenedEffect()); // if you are also using enlightened
    }

    private void loadPowerStones() {
        File powerStonesFolder = new File(plugin.getDataFolder(), "custom/items/powerstones");
        if (!powerStonesFolder.exists() && !powerStonesFolder.mkdirs()) {
            DebugLogger.getInstance().severe("Failed to create powerstones folder.");
            return;
        }

        File[] files = powerStonesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    PowerStoneConfigLoader loader = new PowerStoneConfigLoader(file);
                    PowerStone powerStone = loader.loadPowerStone();
                    registerPowerStone(powerStone);
                    DebugLogger.getInstance().log(Level.INFO, "Loaded power stone: " + powerStone.getId());
                } catch (Exception e) {
                    DebugLogger.getInstance().severe("Failed to load power stone from file " + file.getName() + ":", e);
                }
            }
        }
    }

    public void registerPowerStone(PowerStone powerStone) {
        powerStones.put(powerStone.getId(), powerStone);
    }

    public void registerEffect(Effect effect) {
        effects.put(effect.getName().toUpperCase(), effect);
    }

    public PowerStone getPowerStone(String id) {
        return powerStones.get(id);
    }

    public Effect getEffect(String name) {
        return effects.get(name.toUpperCase());
    }

    public Map<String, PowerStone> getAllPowerStones() {
        return powerStones;
    }

    public Map<String, Effect> getAllEffects() {
        return effects;
    }
}
