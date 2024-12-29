package eu.xaru.mysticrpg.pets;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.customs.mobs.ModelHandler;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Demonstrates using an ArmorStand for the "pet name" while the Wolf is invisible
 * and using a ModelEngine custom model.
 */
public class PetHelper implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, Pet> petConfigurations = new HashMap<>();
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPetConfigurations();
        // Update each tick
        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePets, 0L, 1L);
    }

    private void loadPetConfigurations() {
        File petFolder = new File(plugin.getDataFolder(), "pets");
        if (!petFolder.exists() && !petFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create pets folder.");
            return;
        }
        File[] files = petFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            DebugLogger.getInstance().log(Level.INFO, "No pet config files found in 'pets' folder.", 0);
            return;
        }
        for (File file : files) {
            try {
                String configName = "pets/" + file.getName();
                DynamicConfig config = DynamicConfigManager.getConfig(configName);
                if (config == null) {
                    config = DynamicConfigManager.loadConfig(configName);
                }
                String petId   = config.getString("id", null);
                String petName = config.getString("name", null);
                if (petId == null || petId.isEmpty()) {
                    DebugLogger.getInstance().error("Pet ID is missing in " + file.getName());
                    continue;
                }
                if (petName == null || petName.isEmpty()) {
                    DebugLogger.getInstance().error("Pet name is missing in " + file.getName());
                    continue;
                }
                String displayItem   = config.getString("display_item", "BONE");
                String modelId       = config.getString("model_id", "");
                String idleAnimation = config.getString("idle_animation", "");
                String walkAnimation = config.getString("walk_animation", "");
                Pet pet = new Pet(petId, petName, displayItem, modelId, idleAnimation, walkAnimation);
                petConfigurations.put(petId, pet);
                DebugLogger.getInstance().log(Level.INFO, "Loaded pet config: " + petId, 0);
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load pet config from " + file.getName(), e);
            }
        }
    }

    public void givePet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData data = cache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }
        if (data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You already own this pet: " + pet.getName()));
            return;
        }
        data.getOwnedPets().add(petId);
        cache.savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have received pet: " + pet.getName()));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " got pet: " + petId, 0);
            }
            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to receive pet: " + pet.getName()));
                DebugLogger.getInstance().error("Failed to save pet data for " + player.getName(), throwable);
            }
        });
    }

    public void equipPet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData data = cache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }
        if (!data.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You do not own pet: " + pet.getName()));
            return;
        }
        unequipPet(player);
        PetInstance instance = spawnPetEntity(player, pet);
        playerEquippedPets.put(player.getUniqueId(), instance);
        data.setEquippedPet(petId);
        cache.savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have equipped pet: " + pet.getName()));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " equipped pet: " + petId, 0);
            }
            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to equip pet: " + pet.getName()));
                DebugLogger.getInstance().error("Failed to save equipped pet for player " + player.getName(), throwable);
            }
        });
    }

    public void unequipPet(Player player) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData data = cache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }
        String equippedPetId = data.getEquippedPet();
        if (equippedPetId == null) {
            player.sendMessage(Utils.getInstance().$("You have no pet equipped."));
            return;
        }
        PetInstance instance = playerEquippedPets.remove(player.getUniqueId());
        if (instance != null) {
            if (instance.getNameHologram() != null) {
                instance.getNameHologram().remove();
                instance.getModeledEntity().destroy();
            }
            instance.getPetEntity().remove();
        }
        data.setEquippedPet(null);
        cache.savePlayerData(player.getUniqueId(), new Callback<>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have unequipped your pet."));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " unequipped pet.", 0);
            }
            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to unequip your pet."));
                DebugLogger.getInstance().error("Failed to save unequipped pet for " + player.getName(), throwable);
            }
        });
    }

    private PetInstance spawnPetEntity(Player owner, Pet pet) {
        Location loc = owner.getLocation();
        Wolf wolf = (Wolf) owner.getWorld().spawnEntity(loc, EntityType.WOLF);
        wolf.setTamed(true);
        wolf.setOwner(owner);
        wolf.setAngry(false);
        wolf.setInvulnerable(true);
        wolf.setPersistent(true);
        wolf.setSilent(true);
        wolf.setCollarColor(DyeColor.ORANGE);

        // Make Wolf invisible so we only see the ModelEngine model
        wolf.setInvisible(true);

        if (wolf.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            wolf.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
        }

        PetInstance instance = new PetInstance(pet, wolf);
        String modelId = pet.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            ModeledEntity me = ModelHandler.applyModel(wolf, modelId);
            if (me != null) {
                instance.setModeledEntity(me);
                me.setBaseEntityVisible(false);
            }
        }

        // Create an ArmorStand that will serve as the 'name' hologram
        ArmorStand nameHologram = spawnNameHologram(owner.getName() + "'s " + pet.getName(), wolf.getLocation());
        instance.setNameHologram(nameHologram);

        return instance;
    }

    /**
     * Spawns a small, invisible ArmorStand with a custom name shown.
     */
    private ArmorStand spawnNameHologram(String name, Location loc) {
        Location holoLoc = loc.clone().add(0, 0.7, 0); // slightly above wolf
        ArmorStand stand = loc.getWorld().spawn(holoLoc, ArmorStand.class, a -> {
            a.setCustomName(name);
            a.setCustomNameVisible(true);
            a.setMarker(true);
            a.setInvisible(true);
            a.setSmall(true);
            a.setBasePlate(false);
            a.setArms(false);
            a.setGravity(false);
        });
        return stand;
    }

    private void updatePets() {
        for (UUID playerId : playerEquippedPets.keySet()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                continue;
            }
            PetInstance pi = playerEquippedPets.get(playerId);
            LivingEntity ent = pi.getPetEntity();
            ArmorStand hologram = pi.getNameHologram();
            if (ent.isDead()) {
                continue;
            }
            double dist = ent.getLocation().distance(p.getLocation());
            if (dist > 3.0) {
                Vector dir = p.getLocation().toVector().subtract(ent.getLocation().toVector()).normalize();
                dir.setY(0);
                ent.setVelocity(dir.multiply(0.25));
            }
            float yaw = p.getLocation().getYaw();
            ent.setRotation(yaw, ent.getLocation().getPitch());

            // Reposition the name hologram just above the wolf's head
            if (hologram != null && !hologram.isDead()) {
                Location holoLoc = ent.getLocation().clone().add(0, 0.7, 0);
                hologram.teleport(holoLoc);
            }

            // ModelEngine animation logic
            String modelId = pi.getPet().getModelId();
            if (modelId != null && !modelId.isEmpty() && pi.getModeledEntity() != null) {
                boolean isWalking = dist > 3.0;
                String anim = isWalking
                        ? (pi.getPet().getWalkAnimation().isEmpty() ? "walk" : pi.getPet().getWalkAnimation())
                        : (pi.getPet().getIdleAnimation().isEmpty() ? "idle" : pi.getPet().getIdleAnimation());
                if (!anim.equals(pi.getCurrentAnimation())) {
                    pi.setCurrentAnimation(anim);
                    ModelHandler.playAnimation(ent, modelId, anim, 0.0, 0.0, 1.0, true);
                }
            }
        }
    }

    @EventHandler
    public void onPetDamage(EntityDamageEvent e) {
        for (PetInstance pi : playerEquippedPets.values()) {
            if (pi.getPetEntity().equals(e.getEntity())) {
                e.setCancelled(true);
                return;
            }
        }
    }

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unequipPet(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        unequipPet(event.getEntity());
    }

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
