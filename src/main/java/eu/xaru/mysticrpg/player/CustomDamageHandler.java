package eu.xaru.mysticrpg.player;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.PlayerStats;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatCalculations;
import eu.xaru.mysticrpg.player.stats.StatType;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.player.stats.events.PlayerStatsChangedEvent;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import eu.xaru.mysticrpg.world.WorldModule;
import eu.xaru.mysticrpg.pets.PetEffectTracker;
import eu.xaru.mysticrpg.pets.PetHelper;
import eu.xaru.mysticrpg.pets.PetInstance;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

/**
 * CustomDamageHandler merges old functionality + new PhoenixWill logic + ShamanBlessing logic.
 */
public class CustomDamageHandler implements IBaseModule {

    private PlayerDataCache playerDataCache;
    private PlayerStatsManager statsManager;
    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private JavaPlugin plugin;
    private StatsModule statsModule;

    // PhoenixWill tracking (already existed)
    private final Set<UUID> phoenixUsed = new HashSet<>();
    private final Map<UUID, Long> phoenixImmortalUntil = new HashMap<>();

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        plugin = JavaPlugin.getPlugin(MysticCore.class);

        if (saveModule != null) {
            playerDataCache = PlayerDataCache.getInstance();
        } else {
            DebugLogger.getInstance().error("SaveModule not initialized. CustomDamageHandler cannot function.");
            return;
        }

        if (playerDataCache == null) {
            DebugLogger.getInstance().error("PlayerDataCache not initialized. CustomDamageHandler cannot function.");
            return;
        }

        this.statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        if (statsModule == null) {
            DebugLogger.getInstance().error("StatsModule not found. CustomDamageHandler cannot function properly.");
            return;
        }

        this.statsManager = statsModule.getStatsManager();

       // DebugLogger.getInstance().log(Level.INFO, "CustomDamageHandler initialized", 0);
    }

    @Override
    public void start() {

        // 1) Handle direct attacks
        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            if (!(event.getEntity() instanceof Player victim)) return;
            if (event.isCancelled()) return;

            // Check PVP flag (e.g. if pvp is allowed in this region)
            Entity damager = event.getDamager();
            if (damager instanceof Player attacker) {
                WorldModule wm = ModuleManager.getInstance().getModuleInstance(WorldModule.class);
                if (wm != null && !wm.getWorldManager().isAllowed("pvp", victim.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }

            // If victim is currently immortal from PhoenixWill => skip damage entirely
            if (isPhoenixImmortal(victim)) {
                event.setCancelled(true);
                return;
            }

            double rawDamage = event.getFinalDamage();
            double finalDamage = calculateDamage(damager, victim, rawDamage);

            // Apply the damage to our custom HP system (below), then cancel the normal event
            applyDamageAndEffects(victim, finalDamage, damager);
            event.setCancelled(true);
        });

        // 2) Handle environment damage (fall, lava, etc.)
        eventManager.registerEvent(EntityDamageEvent.class, event -> {
            if (event instanceof EntityDamageByEntityEvent) return;
            if (!(event.getEntity() instanceof Player victim)) return;
            if (event.isCancelled()) return;

            // skip damage if phoenix immortality is active
            if (isPhoenixImmortal(victim)) {
                event.setCancelled(true);
                return;
            }

            EntityDamageEvent.DamageCause cause = event.getCause();
            switch (cause) {
                case FALL, FIRE, FIRE_TICK, LAVA, DROWNING, SUFFOCATION, VOID, LIGHTNING, HOT_FLOOR -> {
                    double finalDamage = event.getFinalDamage();
                    applyDamageAndEffects(victim, finalDamage, null);
                    event.setCancelled(true);
                }
                default -> event.setCancelled(true);
            }
        });

        // Health regeneration loop, runs every second
        new BukkitRunnable() {
            @Override
            public void run() {
                regenerateHealth();
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Called manually if we want to apply custom damage from another source.
     */
    public void applyCustomDamage(Player player, double damage) {
        applyDamageAndEffects(player, damage, null);
    }

    /**
     * Checks if a player is currently immortal from PhoenixWill (2s timeframe).
     */
    private boolean isPhoenixImmortal(Player victim) {
        long now = System.currentTimeMillis();
        long until = phoenixImmortalUntil.getOrDefault(victim.getUniqueId(), 0L);
        return (now < until);
    }

    /**
     * Calculate final damage factoring in stats (strength, crit, defense, etc.).
     */
    private double calculateDamage(Entity damager, Player victim, double baseDamage) {
        PlayerStats victimStats = statsModule.recalculatePlayerStatsFor(victim);

        double finalDamage = baseDamage;
        if (damager instanceof Player attacker) {
            PlayerStats attackerStats = statsModule.recalculatePlayerStatsFor(attacker);
            double strength = attackerStats.getEffectiveStat(StatType.STRENGTH);
            double critChance = attackerStats.getEffectiveStat(StatType.CRIT_CHANCE);
            double critDamage = attackerStats.getEffectiveStat(StatType.CRIT_DAMAGE);

            finalDamage = StatCalculations.calculatePhysicalDamage(finalDamage, strength);
            finalDamage = StatCalculations.calculateCritDamage(finalDamage, critChance, critDamage);
        }

        double defense = victimStats.getEffectiveStat(StatType.DEFENSE);
        finalDamage = StatCalculations.calculateDamageTaken(finalDamage, defense);

        return finalDamage;
    }

    /**
     * Applies damage to the player's custom HP, and checks for PhoenixWill logic.
     */
    private void applyDamageAndEffects(Player victim, double damage, Entity damager) {
        UUID victimUUID = victim.getUniqueId();
        PlayerData victimData = playerDataCache.getCachedPlayerData(victimUUID);
        if (victimData == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + victim.getName());
            return;
        }

        PlayerStats victimStats = statsModule.recalculatePlayerStatsFor(victim);
        int maxHp = (int) victimStats.getEffectiveStat(StatType.HEALTH);

        int currentHp = victimData.getCurrentHp();
        int dealtDamage = (int) Math.round(damage);
        int newHp = currentHp - dealtDamage;
        if (newHp < 0) newHp = 0;
        if (newHp > maxHp) newHp = maxHp;

        // PhoenixWill check: if lethal and not used yet => set HP=1 & 2s immortality.
        boolean hasPhoenix = PetEffectTracker.hasEffect(victim, "phoenixwill");
        boolean usedAlready = phoenixUsed.contains(victimUUID);

        if (hasPhoenix && !usedAlready && newHp <= 0) {
            newHp = 1;
            phoenixImmortalUntil.put(victimUUID, System.currentTimeMillis() + 2000L); // 2s from now
            phoenixUsed.add(victimUUID);
            victim.sendMessage(ChatColor.GOLD + "[Phoenix Will] You survived a lethal blow! 2s invincibility.");
        }

        victimData.setCurrentHp(newHp);

        DebugLogger.getInstance().log("Player " + victim.getName() + " took " + damage + " damage. Current HP: " + newHp);

        if (newHp <= 0) {
            handleDeath(victim);
        } else {
            triggerHurtEffects(victim, damager);
        }

        lastDamageTime.put(victimUUID, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(victim));
    }

    /**
     * Called when a player's HP hits 0 (or below, but clamped to 0).
     */
    private void handleDeath(Player player) {
        // Reset PhoenixWill usage for next life.
        phoenixUsed.remove(player.getUniqueId());
        phoenixImmortalUntil.remove(player.getUniqueId());

        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        PlayerStats stats = statsModule.recalculatePlayerStatsFor(player);
        int maxHp = (int) stats.getEffectiveStat(StatType.HEALTH);

        // Teleport to spawn, e.g.
        Location spawnLocation = player.getWorld().getSpawnLocation();
        player.teleport(spawnLocation);
        player.sendMessage(Utils.getInstance().$("You Died"));
        DebugLogger.getInstance().log("Player " + player.getName() + " died and was teleported to spawn.");

        data.setCurrentHp(maxHp);

        // 50% gold penalty on death.
        EconomyModule econModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (econModule != null) {
            EconomyHelper economyHelper = econModule.getEconomyHelper();
            int held = economyHelper.getHeldGold(player);
            int lost = held / 2; // 50% loss
            if (lost > 0) {
                economyHelper.setHeldGold(player, held - lost);
                player.sendMessage(Utils.getInstance().$("You lost " + lost + " gold upon death."));
            }
        }

        Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
    }

    /**
     * Minor knockback/hurt visuals upon taking damage.
     */
    private void triggerHurtEffects(Player victim, Entity damager) {
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 10);

        if (damager != null && damager != victim) {
            Vector knockback = victim.getLocation().toVector()
                    .subtract(damager.getLocation().toVector())
                    .normalize().multiply(0.4);
            victim.setVelocity(knockback);
        }
    }

    /**
     * Periodic HP regeneration. Also includes immediate ShamanBlessing logic.
     * If "shamanblessing" is present, the player regenerates 2/3/5 HP each second
     * (depending on pet level) regardless of last damage time, stacking with normal regen.
     */
    private void regenerateHealth() {
        Set<UUID> playerUUIDs = playerDataCache.getAllCachedPlayerUUIDs();
        for (UUID pid : playerUUIDs) {
            PlayerData pData = playerDataCache.getCachedPlayerData(pid);
            if (pData == null) continue;

            Player player = Bukkit.getPlayer(pid);
            if (player == null || !player.isOnline()) continue;

            PlayerStats stats = statsModule.recalculatePlayerStatsFor(player);
            int maxHp = (int) stats.getEffectiveStat(StatType.HEALTH);

            int currentHp = pData.getCurrentHp();
            if (currentHp > maxHp) {
                currentHp = maxHp;
                pData.setCurrentHp(currentHp);
            }

            // 1) ShamanBlessing immediate HP each second (bypass 5s wait)
            if (PetEffectTracker.hasEffect(player, "shamanblessing")) {
                // We'll find the level of the shaman pet, so we know how much to heal.
                int shamanLevel = getEquippedPetLevel(player, "shaman");
                if (shamanLevel > 0 && currentHp < maxHp) {
                    int bonusHeal = 2;
                    if (shamanLevel >= 10) bonusHeal = 5;
                    else if (shamanLevel >= 5) bonusHeal = 3;

                    currentHp += bonusHeal;
                    if (currentHp > maxHp) currentHp = maxHp;
                    pData.setCurrentHp(currentHp);

                    DebugLogger.getInstance().log("ShamanBlessing healed " + bonusHeal + " HP for " + player.getName() + ". HP: " + currentHp);
                    Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
                }
            }

            // 2) Then do normal base regen if 5s passed since last damage.
            if (currentHp < maxHp) {
                long lastDmgTime = lastDamageTime.getOrDefault(pid, 0L);
                if (System.currentTimeMillis() - lastDmgTime >= 5000) {
                    double regen = stats.getEffectiveStat(StatType.HEALTH_REGEN);
                    int newHp = currentHp + (int) Math.round(regen);
                    if (newHp > maxHp) newHp = maxHp;
                    pData.setCurrentHp(newHp);

                    DebugLogger.getInstance().log("Regenerated " + regen + " HP for player " + player.getName() + ". HP: " + newHp);
                    Bukkit.getPluginManager().callEvent(new PlayerStatsChangedEvent(player));
                }
            }
        }
    }

    /**
     * Utility to retrieve the player's currently equipped pet's level if it matches `petId`.
     */
    private int getEquippedPetLevel(Player player, String petId) {
        // Access PetHelper to find the actual PetInstance, if any.
        eu.xaru.mysticrpg.pets.PetsModule petsModule = ModuleManager.getInstance().getModuleInstance(eu.xaru.mysticrpg.pets.PetsModule.class);
        if (petsModule == null) return 0;
        PetHelper helper = petsModule.getPetHelper();

        // Check if the player has that pet equipped.
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) return 0;
        String eqId = data.getEquippedPet();
        if (eqId == null) return 0;
        if (!eqId.equalsIgnoreCase(petId)) return 0;

        // If it's equipped, get the instance to find the current level.
        PetInstance pi = helper.getEquippedPetInstance(player);
        if (pi == null) return 0;

        return pi.getPet().getLevel();
    }

    // optionally you can add a "getEquippedPetInstance" in PetHelper if not existing.
}
