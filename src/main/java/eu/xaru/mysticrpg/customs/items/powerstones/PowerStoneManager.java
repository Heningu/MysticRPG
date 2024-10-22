package eu.xaru.mysticrpg.customs.items.powerstones;

import eu.xaru.mysticrpg.customs.items.effects.DeconstructEffect;
import eu.xaru.mysticrpg.customs.items.effects.Effect;
import eu.xaru.mysticrpg.customs.items.effects.FieryEffect;
import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        // Register effects
        registerEffect(new FieryEffect());
        registerEffect(new DeconstructEffect());
        // Add more effects as needed
    }

    private void loadPowerStones() {
        File powerStonesFolder = new File(plugin.getDataFolder(), "custom/powerstones");
        if (!powerStonesFolder.exists() && !powerStonesFolder.mkdirs()) {
            Bukkit.getLogger().severe("Failed to create powerstones folder.");
            return;
        }

        File[] files = powerStonesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    PowerStoneConfigLoader loader = new PowerStoneConfigLoader(file);
                    PowerStone powerStone = loader.loadPowerStone();
                    registerPowerStone(powerStone);
                    Bukkit.getLogger().log(Level.INFO, "Loaded power stone: " + powerStone.getId());
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to load power stone from file " + file.getName() + ": " + e.getMessage());
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
