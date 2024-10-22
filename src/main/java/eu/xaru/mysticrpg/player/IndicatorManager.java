package eu.xaru.mysticrpg.player;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class IndicatorManager implements IBaseModule {

    private final JavaPlugin plugin;
    private DebugLoggerModule logger;

    public IndicatorManager() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            Bukkit.getLogger().severe("DebugLoggerModule not found! IndicatorManager cannot function without it.");
            return;
        }
        logger.log(Level.INFO, "IndicatorManager initialized", 0);
    }

    @Override
    public void start() {
        // No additional start logic needed
    }

    @Override
    public void stop() {
        // Optionally, remove all holograms on stop
        // This depends on whether you want to keep existing holograms when the plugin stops
    }

    @Override
    public void unload() {
        // Clean up resources if necessary
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);
    }

    /**
     * Displays a damage indicator hologram at the specified location.
     *
     * @param location The location where the hologram will appear.
     * @param damage   The amount of damage to display.
     */
    public void showDamageIndicator(Location location, double damage) {
        if (damage <= 0) return; // Do not display zero or negative damage

        String damageText = ChatColor.translateAlternateColorCodes('&', String.format(Locale.US, "&c-%.1f", damage));

        // Create a unique name for the hologram
        String holoName = "damage_" + UUID.randomUUID();

        try {
            // Create the hologram with the damage text
            Hologram hologram = DHAPI.createHologram(holoName, location, List.of(damageText));
            DHAPI.setHologramLines(hologram, List.of(damageText));

            // Animate the hologram moving upwards and remove it after 3 seconds
            new BukkitRunnable() {
                int ticksLived = 0;

                @Override
                public void run() {
                    ticksLived++;

                    // Move the hologram upwards every tick
                    if (ticksLived <= 60) { // 3 seconds at 20 ticks per second
                        Location newLocation = hologram.getLocation().clone().add(0, 0.03, 0); // Move up by 0.03 blocks per tick
                        DHAPI.moveHologram(hologram, newLocation);
                    }

                    if (ticksLived >= 60) {
                        DHAPI.removeHologram(holoName);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 1, 1); // Start after 1 tick and run every tick
        } catch (Exception e) {
            logger.error("Error showing damage indicator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Displays an XP indicator hologram at the specified location.
     *
     * @param location The location where the hologram will appear.
     * @param xpAmount The amount of XP to display.
     */
    public void showXPIndicator(Location location, int xpAmount) {
        if (xpAmount <= 0) return; // Do not display zero or negative XP

        String xpText = ChatColor.translateAlternateColorCodes('&', String.format(Locale.US, "&a+%d XP", xpAmount));

        // Create a unique name for the hologram
        String holoName = "xp_" + UUID.randomUUID();

        try {
            // Create the hologram with the XP text
            Hologram hologram = DHAPI.createHologram(holoName, location, List.of(xpText));
            DHAPI.setHologramLines(hologram, List.of(xpText));

            // Schedule hologram removal after 3 seconds
            new BukkitRunnable() {
                int ticksLived = 0;

                @Override
                public void run() {
                    ticksLived++;

                    if (ticksLived >= 60) { // 3 seconds at 20 ticks per second
                        DHAPI.removeHologram(holoName);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 1, 1); // Start after 1 tick and run every tick
        } catch (Exception e) {
            logger.error("Error showing XP indicator: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
