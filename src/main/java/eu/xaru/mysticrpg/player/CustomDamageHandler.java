package eu.xaru.mysticrpg.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.ui.ActionBarManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;

public class CustomDamageHandler implements IBaseModule {

    private PlayerDataCache playerDataCache;
    
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private JavaPlugin plugin;
    private ActionBarManager actionBarManager;

    @Override
    public void initialize() {
        
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        if (saveModule != null) {
            playerDataCache = PlayerDataCache.getInstance();
        } else {
            DebugLogger.getInstance().error("SaveModule not initialized. CustomDamageHandler cannot function without it.");
            return;
        }

        if (playerDataCache == null) {
            DebugLogger.getInstance().error("PlayerDataCache not initialized. CustomDamageHandler cannot function without it.");
            return;
        }

        actionBarManager = new ActionBarManager((MysticCore) plugin, playerDataCache);

        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler initialized", 0);


        new CommandAPICommand("applydamage")
                .withPermission("mysticrpg.command.applydamage")
                .executesPlayer((player, args) -> {
                    if (args.count() == 0) {
                        player.sendMessage(ChatColor.RED + "Usage: /applydamage <amount>");
                        return;
                    }

                    Double damageObj = (Double) args.get(0);
                    if (damageObj == null) {
                        player.sendMessage(ChatColor.RED + "Invalid damage value!");
                        return;
                    }
                    applyCustomDamage(player, damageObj);
                })
                .register();

        new CommandAPICommand("heal")
                .withPermission("mysticrpg.command.heal")
                .executesPlayer((player, args) -> {
                    UUID playerUUID = player.getUniqueId();
                    PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

                    if (playerData == null) {
                        DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
                        return;
                    }

                    int maxHp = playerData.getAttributes().getOrDefault("HP", playerData.getAttributes().getOrDefault("max_hp", 20));
                    playerData.setCurrentHp(maxHp);
                    actionBarManager.updateActionBar(player);
                    player.sendMessage(ChatColor.GREEN + "You have been healed to full health.");
                })
                .register();

    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler started", 0);

        // Register EntityDamageByEntityEvent first to handle damage from mobs and players specifically
        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (event.isCancelled()) return;

                DebugLogger.getInstance().log(Level.INFO, "EntityDamageByEntityEvent", 0);

                // Handle only entity-related damage here
                handleDamage(player, event, event.getFinalDamage());
                triggerHurtAnimation(player, true);

                // Cancel the event to prevent actual health damage
                event.setCancelled(true);
            }
        });

        // Register EntityDamageEvent for handling environmental damage and other non-entity damage types
        eventManager.registerEvent(EntityDamageEvent.class, event -> {
            if (event instanceof EntityDamageByEntityEvent) {
                // Skip processing as this is already handled in EntityDamageByEntityEvent
                return;
            }

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                EntityDamageEvent.DamageCause cause = event.getCause();
                DebugLogger.getInstance().log("EntityDamageEvent %s", cause);

                switch (cause) {
                    case FALL, FIRE_TICK, LAVA, DROWNING:
                        // Handle environmental damage
                        handleDamage(player, event, event.getFinalDamage());
                        triggerHurtAnimation(player, false);
                        break;
                    case ENTITY_ATTACK:
                        // Ignore this, as it is handled by EntityDamageByEntityEvent
                        break;
                    default:
                        // Cancel all other damage causes
                        event.setCancelled(true);
                        break;
                }
            }
        });

        // Health regeneration task
        new BukkitRunnable() {
            @Override
            public void run() {
                regenerateHealth();
            }
        }.runTaskTimer(plugin, 20, 20); // Schedule task to run every second (20 ticks)
    }

    private void handleDamage(Player player, EntityDamageEvent event, double damage) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

        if (playerData == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return;
        }

        // Deduct custom HP based on the actual damage
        int currentHp = playerData.getCurrentHp();
        int maxHp = playerData.getAttributes().getOrDefault("HP", 20);
        int customDamage = (int) Math.round(damage);

        // Adjust custom HP
        currentHp -= customDamage;
        if (currentHp < 0) currentHp = 0;
        playerData.setCurrentHp(currentHp);

        // Update the action bar after taking damage
        actionBarManager.updateActionBar(player);

        // Log the damage event
        DebugLogger.getInstance().log("Player " + player.getName() + " took " + damage + " damage. Current HP: " + currentHp);
        if (currentHp <= 0) {
            // Handle player death
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.sendMessage(Utils.getInstance().$("You Died"));
            DebugLogger.getInstance().log("Player " + player.getName() + " died and was teleported to spawn.");

            // Reset player's health to max for respawn
            playerData.setCurrentHp(maxHp);
        }

        // Record last damage time for regeneration purposes
        lastDamageTime.put(playerUUID, System.currentTimeMillis());

        // Cancel the actual event to prevent health loss from Minecraft's default system
        event.setCancelled(true);
    }

    // Helper method to trigger hurt animation manually
    private void triggerHurtAnimation(Player player, boolean isEntityDamage) {
        // Play hurt sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        // Display damage indicator particles
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 10);

        // Apply knockback only if the damage was caused by an entity
        if (isEntityDamage) {
            // Apply slight knockback to simulate damage (hurt effect)
            Vector knockback = player.getLocation().getDirection().multiply(-0.3);
            player.setVelocity(knockback);
        }
    }

    public void applyCustomDamage(Player player, double damage) {
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);

        if (playerData == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return;
        }

        // Deduct custom HP based on the actual damage
        int currentHp = playerData.getCurrentHp();
        int customDamage = (int) Math.round(damage);

        // Adjust custom HP
        currentHp -= customDamage;
        if (currentHp < 0) currentHp = 0;
        playerData.setCurrentHp(currentHp);

        // Update the action bar after taking damage
        actionBarManager.updateActionBar(player);

        // Log the damage event
        DebugLogger.getInstance().log("Player " + player.getName() + " took " + damage + " damage. Current HP: " + currentHp);
        if (currentHp <= 0) {
            // Handle player death
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.sendMessage(Utils.getInstance().$("You Died"));
            DebugLogger.getInstance().log("Player " + player.getName() + " died and was teleported to spawn.");

            // Reset player's health to max for respawn
            int maxHp = playerData.getAttributes().getOrDefault("HP", 20);
            playerData.setCurrentHp(maxHp);
        }

        // Record last damage time for regeneration purposes
        lastDamageTime.put(playerUUID, System.currentTimeMillis());
    }




    private void regenerateHealth() {
        Set<UUID> playerUUIDs = playerDataCache.getAllCachedPlayerUUIDs();
        for (UUID playerUUID : playerUUIDs) {
            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
            if (playerData == null) continue;

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) continue;

            int currentHp = playerData.getCurrentHp();
            int maxHp = playerData.getAttributes().getOrDefault("HP", 20);

            // Check if we need to regenerate
            if (currentHp < maxHp) {
                long lastDamage = lastDamageTime.getOrDefault(playerUUID, 0L);
                if (System.currentTimeMillis() - lastDamage >= 5000) {
                    // Start regenerating HP
                    currentHp += 1; // Regenerate 1 HP per second
                    if (currentHp > maxHp) currentHp = maxHp;
                    playerData.setCurrentHp(currentHp);
                    DebugLogger.getInstance().log("Regenerated 1 HP for player " + player.getName() + ". Current HP: " + currentHp);

                    // Update the action bar after regeneration
                    actionBarManager.updateActionBar(player);
                }
            }
        }
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }
}


