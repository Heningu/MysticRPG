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

/**
 * PetHelper that ALWAYS reads/writes level/XP from file,
 * ignoring any leftover in-memory version.
 */
public class PetHelper implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, Pet> petConfigurations = new HashMap<>();
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        PetFileStorage.init(plugin);

        // Load known pet definitions from PetRegistry
        for (Pet p : PetRegistry.getAllPets()) {
            petConfigurations.put(p.getId(), p);
            DebugLogger.getInstance().log(Level.INFO, "Registered pet: " + p.getId(), 0);
        }

        // Update pets each tick (for overhead name holograms, etc.)
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
     * This does not do any file-based XP logic; it just adds ownership to PlayerData.
     */
    public void givePet(Player player, String petId) {
        Pet basePet = petConfigurations.get(petId);
        if (basePet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded."));
            return;
        }
        if (data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You already own this pet: " + basePet.getName()));
            return;
        }
        data.getOwnedPets().add(petId);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have received pet: " + basePet.getName()));
            }

            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to receive pet: " + basePet.getName()));
            }
        });
    }

    /**
     * Equip a pet by ID, ignoring any in-memory data.
     * We always read its level/xp from file to ensure correctness.
     */
    public void equipPet(Player player, String petId) {
        Pet basePet = petConfigurations.get(petId);
        if (basePet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded."));
            return;
        }
        if (!data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You do not own pet: " + basePet.getName()));
            return;
        }

        // Unequip any previously equipped pet
        unequipPet(player);

        // 1) Read the player's pet data from file
        var fileData = PetFileStorage.loadPlayerPets(player);
        // 2) Overwrite the basePet's level/xp with what's in the file
        var storedProgress = fileData.get(petId);
        if (storedProgress != null) {
            basePet.setLevel(storedProgress.getLevel());
            basePet.setCurrentXp(storedProgress.getXp());
        } else {
            // If never saved before, presumably level=1 xp=0 from constructor
        }

        // 3) Apply stats/effects to the player
        PetStatManager.applyPetBonuses(player, basePet);

        // 4) Spawn the Wolf
        PetInstance instance = spawnPetEntity(player, basePet);
        playerEquippedPets.put(player.getUniqueId(), instance);

        // 5) Mark in PlayerData which pet is equipped
        data.setEquippedPet(petId);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), null);

        player.sendMessage(Utils.getInstance().$("You have equipped pet: " + basePet.getName()));
    }

    /**
     * Unequip the player's current pet, removing stats/effects,
     * then saving the final level/XP to file.
     */
    public void unequipPet(Player player) {
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        String eqPetId = data.getEquippedPet();
        if (eqPetId == null) return; // none equipped => nothing to do

        // 1) Remove from our internal map
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

        // 2) Overwrite the file data with the final level/xp from the in-memory Pet
        Pet oldPet = petConfigurations.get(eqPetId);
        if (oldPet != null) {
            // read file to ensure we don’t lose other pets’ data
            var fileData = PetFileStorage.loadPlayerPets(player);
            // update just eqPetId in that file
            fileData.put(eqPetId, new PetFileStorage.PetProgress(oldPet.getLevel(), oldPet.getCurrentXp()));
            // save back
            PetFileStorage.savePlayerPets(player, fileData);

            // remove stats/effects
            PetStatManager.removePetBonuses(player, oldPet);
        }

        // 3) Clear from PlayerData
        data.setEquippedPet(null);
        PlayerDataCache.getInstance().savePlayerData(player.getUniqueId(), null);

        player.sendMessage(Utils.getInstance().$("You have unequipped your pet."));
    }

    /**
     * Add XP to the player's equipped pet.
     * ALWAYS re-read from file first, apply the XP, then rewrite to file
     * so we never trust the old in-memory data.
     */
    public void addPetXp(Player player, int amount) {
        if (amount <= 0) return;

        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        String eqPetId = data.getEquippedPet();
        if (eqPetId == null) return; // no pet equipped => do nothing

        PetInstance inst = playerEquippedPets.get(player.getUniqueId());
        if (inst == null) return; // no Wolf? => do nothing

        // 1) Re-read from file, to get the "true" current xp/level
        var fileData = PetFileStorage.loadPlayerPets(player);

        // 2) Overwrite the in-memory Pet with the file data
        Pet pet = inst.getPet();
        PetFileStorage.PetProgress prior = fileData.get(eqPetId);
        if (prior != null) {
            pet.setLevel(prior.getLevel());
            pet.setCurrentXp(prior.getXp());
        }
        // else if there's no prior entry, we keep the constructor's level=1 xp=0

        int oldLvl = pet.getLevel();

        // 3) Actually add XP
        pet.addXp(amount);

        // 4) Save it back to file
        fileData.put(eqPetId, new PetFileStorage.PetProgress(pet.getLevel(), pet.getCurrentXp()));
        PetFileStorage.savePlayerPets(player, fileData);

        // 5) If level changed => update overhead hologram name
        if (pet.getLevel() != oldLvl && inst.getNameHologram() != null) {
            inst.getNameHologram().setCustomName(pet.getFancyName(player.getName()));
        }
    }

    /**
     * Spawns a ground-based Wolf with normal AI, plus an overhead name hologram.
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

        // normal Wolf AI + slightly faster movement
        wolf.setAI(true);
        if (wolf.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            wolf.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
        }

        PetInstance instance = new PetInstance(pet, wolf);

        // If you're using ModelEngine
        String modelId = pet.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            var modeledEntity = ModelHandler.applyModel(wolf, modelId);
            if (modeledEntity != null) {
                instance.setModeledEntity(modeledEntity);
                modeledEntity.setBaseEntityVisible(false);
            }
        }

        // overhead name
        ArmorStand holo = spawnNameHologram(pet.getFancyName(owner.getName()), wolf.getLocation(), 0.7);
        instance.setNameHologram(holo);

        return instance;
    }

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
     * Example logic for "firetick" or "phoenixwill" effects.
     */
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        PetInstance pi = playerEquippedPets.get(attacker.getUniqueId());
        if (pi == null) return;
        Pet pet = pi.getPet();
        if (pet == null) return;

        // If the pet has "firetick", apply fire ticks
        if (pet.getEffects().contains("firetick")) {
            Entity target = event.getEntity();
            if (target.getFireTicks() <= 0) {
                int lvl = pet.getLevel();
                int ticks = (lvl >= 10) ? 100 : (lvl >= 5 ? 80 : 40);
                target.setFireTicks(ticks);
            }
        }
        // "phoenixwill" effect is recognized but no special flight code
    }

    /**
     * Prevent the Wolf from taking damage.
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
     * Prevent the Wolf from targeting or being angry at anything.
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
     * Called every tick. We only reposition the overhead name if it exists.
     * No custom AI (since all are normal Wolves).
     */
    private void updatePets() {
        for (UUID playerId : playerEquippedPets.keySet()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) continue;

            PetInstance pi = playerEquippedPets.get(playerId);
            if (pi == null) continue;

            LivingEntity ent = pi.getPetEntity();
            if (ent.isDead()) continue;

            ArmorStand holo = pi.getNameHologram();
            if (holo != null && !holo.isDead()) {
                double offsetY = pi.getPet().getId().equalsIgnoreCase("phoenix") ? 1.5 : 0.7;
                holo.teleport(ent.getLocation().clone().add(0, offsetY, 0));
            }
        }
    }

    // Utility methods:

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
