package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.customs.mobs.ModelHandler;
import eu.xaru.mysticrpg.pets.content.PetRegistry;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class PetHelper implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, Pet> petConfigurations = new HashMap<>();
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        PetFileStorage.init(plugin);

        // Load pets from our PetRegistry
        for (Pet p : PetRegistry.getAllPets()) {
            petConfigurations.put(p.getId(), p);
            DebugLogger.getInstance().log(Level.INFO, "Registered pet: " + p.getId(), 0);
        }

        // Update pets each tick
        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePets, 0L, 1L);
    }

    /**
     * Returns the PetInstance for the currently equipped pet (if any).
     */
    public PetInstance getEquippedPetInstance(Player player) {
        return playerEquippedPets.get(player.getUniqueId());
    }

    /**
     * Give a pet by ID to a player, if not owned already.
     */
    public void givePet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded."));
            return;
        }
        if (data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You already own this pet: " + pet.getName()));
            return;
        }
        data.getOwnedPets().add(petId);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have received pet: " + pet.getName()));
            }

            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to receive pet: " + pet.getName()));
            }
        });
    }

    /**
     * Equip a pet (by ID). All pets behave like ground-based wolves,
     * with normal Wolf AI & pathfinding.
     */
    public void equipPet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded."));
            return;
        }
        if (!data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You do not own pet: " + pet.getName()));
            return;
        }

        // Unequip any old pet first
        unequipPet(player);

        // Load saved level/xp from local file
        var petProgressMap = PetFileStorage.loadPlayerPets(player);
        var prog = petProgressMap.get(petId);
        if (prog != null) {
            pet.setLevel(prog.getLevel());
            pet.setCurrentXp(prog.getXp());
        }

        // Apply stats/effects
        PetStatManager.applyPetBonuses(player, pet);

        // Spawn entity
        PetInstance instance = spawnPetEntity(player, pet);
        playerEquippedPets.put(player.getUniqueId(), instance);

        // Mark in PlayerData
        data.setEquippedPet(petId);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), null);

        player.sendMessage(Utils.getInstance().$("You have equipped pet: " + pet.getName()));
    }

    /**
     * Unequip the player's current pet, removing stats/effects, model, etc.
     * Also saves the pet's level/xp so no data is lost.
     */
    public void unequipPet(Player player) {
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        String eqPetId = data.getEquippedPet();
        if (eqPetId == null) return; // none equipped

        // 1) Remove from the internal map
        PetInstance instance = playerEquippedPets.remove(player.getUniqueId());
        if (instance != null) {
            // remove model
            if (instance.getModeledEntity() != null) {
                instance.getModeledEntity().destroy();
            }
            // remove hologram
            if (instance.getNameHologram() != null) {
                instance.getNameHologram().remove();
            }
            // remove entity
            instance.getPetEntity().remove();
        }

        // 2) Save the current level/XP to file
        Pet oldPet = petConfigurations.get(eqPetId);
        if (oldPet != null) {
            var map = PetFileStorage.loadPlayerPets(player);
            map.put(eqPetId, new PetFileStorage.PetProgress(oldPet.getLevel(), oldPet.getCurrentXp()));
            PetFileStorage.savePlayerPets(player, map);

            // remove stats/effects
            PetStatManager.removePetBonuses(player, oldPet);
        }

        // 3) remove "equippedPet" from PlayerData
        data.setEquippedPet(null);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), null);

        player.sendMessage(Utils.getInstance().$("You have unequipped your pet."));
    }

    /**
     * Add XP to the currently equipped pet, then save to local file.
     * This is the correct approach to ensure leveling & leftover xp are handled.
     */
    public void addPetXp(Player player, int amount) {
        if (amount <= 0) return;
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;
        String eqId = data.getEquippedPet();
        if (eqId == null) return;

        PetInstance inst = playerEquippedPets.get(player.getUniqueId());
        if (inst == null) return;

        Pet pet = inst.getPet();
        int oldLvl = pet.getLevel();
        pet.addXp(amount);

        // Save updated
        var map = PetFileStorage.loadPlayerPets(player);
        map.put(eqId, new PetFileStorage.PetProgress(pet.getLevel(), pet.getCurrentXp()));
        PetFileStorage.savePlayerPets(player, map);

        // If level changed => update overhead name
        if (pet.getLevel() != oldLvl && inst.getNameHologram() != null) {
            inst.getNameHologram().setCustomName(pet.getFancyName(player.getName()));
        }
    }

    /**
     * Actually spawn the Wolf (or the entity). All pets = normal Wolf AI.
     */
    private PetInstance spawnPetEntity(Player owner, Pet pet) {
        Wolf wolf = (Wolf) owner.getWorld().spawnEntity(owner.getLocation(), EntityType.WOLF);
        wolf.setTamed(true);
        wolf.setOwner(owner);
        wolf.setAngry(false);
        wolf.setPersistent(true);
        wolf.setSilent(true);
        wolf.setCollarColor(DyeColor.ORANGE);
        wolf.setInvulnerable(true);
        wolf.setCanPickupItems(false);

        // If server version supports setCollidable(false):
        try {
            wolf.setCollidable(false);
        } catch (NoSuchMethodError ignored) {}

        // For all pets => normal Wolf AI + faster speed
        wolf.setAI(true);
        if (wolf.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            wolf.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
        }

        PetInstance instance = new PetInstance(pet, wolf);

        // If using ModelEngine
        String modelId = pet.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            var modeledEntity = ModelHandler.applyModel(wolf, modelId);
            if (modeledEntity != null) {
                instance.setModeledEntity(modeledEntity);
                modeledEntity.setBaseEntityVisible(false);
            }
        }

        // Overhead name hologram (use the same offset for all pets, e.g. 0.7)
        ArmorStand holo = spawnNameHologram(pet.getFancyName(owner.getName()), wolf.getLocation(), 0.7);
        instance.setNameHologram(holo);

        return instance;
    }

    /**
     * Summon a floating ArmorStand with a custom name at a given offset above the entity.
     */
    private ArmorStand spawnNameHologram(String name, Location loc, double offsetY) {
        return loc.getWorld().spawn(loc.clone().add(0, offsetY, 0), ArmorStand.class, a -> {
            a.setCustomName(name);
            a.setCustomNameVisible(true);
            a.setMarker(true);
            a.setInvisible(true);
            a.setSmall(true);
            a.setBasePlate(false);
            a.setArms(false);
            a.setGravity(false);
        });
    }

    /**
     * Example attack handler for "firetick" or "phoenixwill" (still possible if effect is defined).
     */
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        PetInstance pi = playerEquippedPets.get(attacker.getUniqueId());
        if (pi == null) return;
        Pet pet = pi.getPet();
        if (pet == null) return;

        // Example effect check: "firetick"
        if (pet.getEffects().contains("firetick")) {
            var target = event.getEntity();
            if (target.getFireTicks() <= 0) {
                int lvl = pet.getLevel();
                int ticks = (lvl >= 10) ? 100 : (lvl >= 5 ? 80 : 40);
                target.setFireTicks(ticks);
            }
        }
        // "phoenixwill" effect is still recognized if it's in the pet's effect list,
        // but we do not do any special "flying" code for phoenix.
    }

    /**
     * Prevent pet from taking damage.
     */
    @EventHandler
    public void onPetDamage(EntityDamageEvent e) {
        for (PetInstance pi : playerEquippedPets.values()) {
            if (pi.getPetEntity().equals(e.getEntity())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevent pet from targeting or being angry at anything.
     */
    @EventHandler
    public void onPetTarget(EntityTargetEvent e) {
        if (!(e.getEntity() instanceof Wolf wolf)) return;
        for (PetInstance pi : playerEquippedPets.values()) {
            if (pi.getPetEntity().equals(wolf)) {
                e.setCancelled(true);
                wolf.setAngry(false);
                wolf.setTarget(null);
                break;
            }
        }
    }

    /**
     * On player quit => forcibly unequip pet (and save).
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unequipPet(event.getPlayer());
    }

    /**
     * On player death => forcibly unequip pet (and save).
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        unequipPet(event.getEntity());
    }

    /**
     * Update logic each tick:
     * - Now all pets are ground-based, using default Wolf AI.
     *   We only update the overhead hologram's position.
     */
    private void updatePets() {
        for (UUID playerId : playerEquippedPets.keySet()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) continue;

            PetInstance pi = playerEquippedPets.get(playerId);
            if (pi == null) continue;

            LivingEntity ent = pi.getPetEntity();
            if (ent.isDead()) continue;

            // Overhead hologram => keep at ~0.7 above the entity
            ArmorStand holo = pi.getNameHologram();
            if (holo != null && !holo.isDead()) {
                holo.teleport(ent.getLocation().clone().add(0, 0.7, 0));
            }
        }
    }

    // Utility methods
    public Pet getPetById(String petId) {
        return petConfigurations.get(petId);
    }

    public String[] getAvailablePetIds() {
        return petConfigurations.keySet().toArray(new String[0]);
    }

    public String[] getOwnedPetIds(Player player) {
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data != null && data.getOwnedPets() != null) {
            return data.getOwnedPets().toArray(new String[0]);
        }
        return new String[0];
    }

    public void listPets(Player player) {
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }
        player.sendMessage(Utils.getInstance().$("Available Pets:"));
        for (Pet pet : petConfigurations.values()) {
            boolean hasIt = data.getOwnedPets().contains(pet.getId());
            player.sendMessage(Utils.getInstance().$("- " + pet.getId() + ": " + pet.getName() + (hasIt ? " (Owned)" : "")));
        }
    }
}
