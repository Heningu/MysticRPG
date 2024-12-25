package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages pet configurations, spawning, equipping, and updating animations.
 */
public class PetHelper implements Listener {

    private final JavaPlugin plugin;

    // Loaded pet configurations (petId -> Pet)
    private final Map<String, Pet> petConfigurations = new HashMap<>();
    // Track currently equipped pets for each player
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPetConfigurations();

        // Schedule a repeating task to update pet positions and animations
        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePets, 0L, 1L); // every tick for smoother movement
    }

    /**
     * Loads pet configurations from .yml files located in <pluginDataFolder>/pets/.
     * Uses DynamicConfig for each file.
     */
    private void loadPetConfigurations() {
        File petFolder = new File(plugin.getDataFolder(), "pets");
        if (!petFolder.exists() && !petFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create pets folder.");
            return;
        }

        File[] files = petFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            DebugLogger.getInstance().log(Level.INFO, "No pet configuration files found in 'pets' folder.", 0);
            return;
        }

        for (File file : files) {
            try {
                // Build a userFileName for this .yml
                String userFileName = "pets/" + file.getName();

                // Attempt to get or load the config
                DynamicConfig config = DynamicConfigManager.getConfig(userFileName);
                if (config == null) {
                    config = DynamicConfigManager.loadConfig(userFileName, userFileName);
                }

                // Read essential fields
                String petId   = config.getString("id", null);
                String petName = config.getString("name", null);

                if (petId == null || petId.isEmpty()) {
                    DebugLogger.getInstance().error("Pet ID is missing in file: " + file.getName());
                    continue;
                }
                if (petName == null || petName.isEmpty()) {
                    DebugLogger.getInstance().error("Pet name is missing in file: " + file.getName());
                    continue;
                }

                String displayItem = config.getString("display_item", "ZOMBIE_HEAD");

                Pet pet = new Pet(petId, petName, displayItem);
                petConfigurations.put(petId, pet);
                DebugLogger.getInstance().log(Level.INFO, "Loaded pet configuration: " + petId, 0);

            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load pet config from file " + file.getName() + ":", e);
            }
        }
    }

    /**
     * Grants a pet to a player and saves updated data to the DB (via PlayerDataCache).
     */
    public void givePet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }

        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }

        // If player already owns this pet, do nothing
        if (playerData.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You already own this pet: " + pet.getName()));
            return;
        }

        // Add pet to the player's owned set
        playerData.getOwnedPets().add(petId);
        cache.savePlayerData(player.getUniqueId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have received pet: " + pet.getName()));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " received pet: " + petId, 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to receive pet: " + pet.getName()));
                DebugLogger.getInstance().error("Failed to save pet data for player " + player.getName() + ": ", throwable);
            }
        });
    }

    /**
     * Equips a pet if the player owns it, spawns the entity, and saves new data.
     */
    public void equipPet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }

        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }

        if (!playerData.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You do not own pet: " + pet.getName()));
            return;
        }

        // Unequip any current pet
        unequipPet(player);

        // Spawn the new pet entity
        PetInstance petInstance = spawnPetEntity(player, pet);

        // Keep track of this pet
        playerEquippedPets.put(player.getUniqueId(), petInstance);

        // Save that the player has equipped this pet
        playerData.setEquippedPet(petId);
        cache.savePlayerData(player.getUniqueId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have equipped pet: " + pet.getName()));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " equipped pet: " + petId, 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to equip pet: " + pet.getName()));
                DebugLogger.getInstance().error("Failed to save equipped pet for player " + player.getName() + ": ", throwable);
            }
        });
    }

    /**
     * Unequips the currently equipped pet for a player, if any.
     */
    public void unequipPet(Player player) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }

        String equippedPetId = playerData.getEquippedPet();
        if (equippedPetId == null) {
            player.sendMessage(Utils.getInstance().$("You have no pet equipped."));
            return;
        }

        // Remove from the local map, remove the ArmorStand
        PetInstance petInstance = playerEquippedPets.remove(player.getUniqueId());
        if (petInstance != null) {
            petInstance.getPetEntity().remove();
        }

        // Clear in PlayerData
        playerData.setEquippedPet(null);
        cache.savePlayerData(player.getUniqueId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                player.sendMessage(Utils.getInstance().$("You have unequipped your pet."));
                DebugLogger.getInstance().log(Level.INFO, "Player " + player.getName() + " unequipped their pet.", 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                player.sendMessage(Utils.getInstance().$("Failed to unequip your pet."));
                DebugLogger.getInstance().error("Failed to save unequipped pet for player " + player.getName() + ": ", throwable);
            }
        });
    }

    /**
     * Returns the Pet object for the given ID, or null if none is loaded.
     */
    public Pet getPetById(String petId) {
        return petConfigurations.get(petId);
    }

    /**
     * Spawns an ArmorStand entity for the given pet near the player.
     */
    private PetInstance spawnPetEntity(Player player, Pet pet) {
        Location loc = player.getLocation().clone().add(0, 0.5, 0);

        // Create the ArmorStand
        ArmorStand petEntity = player.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setCustomName(pet.getName());
            armorStand.setCustomNameVisible(true);
            armorStand.setMarker(true);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setSmall(true);
            armorStand.setBasePlate(false);
            armorStand.setArms(false);
            armorStand.setSilent(true);

            // Equip the head item
            Material mat = Material.matchMaterial(pet.getDisplayItem().toUpperCase());
            if (mat != null) {
                ItemStack headItem = new ItemStack(mat);
                armorStand.getEquipment().setHelmet(headItem);
            }
        });

        return new PetInstance(pet, petEntity);
    }

    /**
     * Called every tick to smoothly update the positions and animations
     * of all pets currently equipped by players.
     */
    private void updatePets() {
        for (UUID playerId : playerEquippedPets.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            PetInstance petInstance = playerEquippedPets.get(playerId);
            ArmorStand petEntity = petInstance.getPetEntity();

            // Desired position is ~1.5 blocks to the right, offset in Y
            Location playerLoc = player.getLocation().clone();
            Vector right = getRightDirection(playerLoc.getYaw());
            Vector offset = right.multiply(1.5);
            Location petLoc = playerLoc.clone().add(offset).add(0, 0.5, 0);

            // Current pet position
            Location currentPetLoc = petEntity.getLocation();
            Vector direction = petLoc.toVector().subtract(currentPetLoc.toVector());
            double distance = direction.length();

            // Move if distance is large enough
            if (distance > 0.05) {
                direction.normalize();
                double speed = 0.2; // Adjust as needed
                Vector velocity = direction.multiply(speed);
                petEntity.teleport(currentPetLoc.add(velocity));
            }

            // Simple up-down bobbing animation
            double animationPhase = petInstance.getAnimationPhase();
            double deltaY = Math.sin(animationPhase) * 0.1;
            petEntity.teleport(petEntity.getLocation().add(0, deltaY, 0));
            petInstance.setAnimationPhase(animationPhase + 0.1);

            // Face same direction as player
            petEntity.setRotation(playerLoc.getYaw(), petEntity.getLocation().getPitch());

            // If pet is too close, push it out slightly
            if (distance < 1.0) {
                Vector pushBack = currentPetLoc.toVector().subtract(playerLoc.toVector()).normalize().multiply(0.5);
                petEntity.teleport(currentPetLoc.add(pushBack));
            }
        }
    }

    /**
     * Returns a Vector pointing to the right of the given yaw angle.
     */
    private Vector getRightDirection(float yaw) {
        // Convert yaw to radians
        double yawRad = Math.toRadians(-yaw);
        return new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));
    }

    /**
     * Returns all pet IDs known to this PetHelper.
     */
    public String[] getAvailablePetIds() {
        return petConfigurations.keySet().toArray(new String[0]);
    }

    /**
     * Returns all pet IDs that the player currently owns.
     */
    public String[] getOwnedPetIds(Player player) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData != null) {
            Set<String> ownedPets = playerData.getOwnedPets();
            if (ownedPets == null) {
                ownedPets = new HashSet<>();
            }
            return ownedPets.toArray(new String[0]);
        }
        return new String[0];
    }

    /**
     * Lists all available pets to the player, marking which they own.
     */
    public void listPets(Player player) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.getInstance().$("Your data is not loaded. Please try again."));
            return;
        }

        player.sendMessage(Utils.getInstance().$("Available Pets:"));
        for (Pet pet : petConfigurations.values()) {
            String owned = playerData.getOwnedPets().contains(pet.getId()) ? " (Owned)" : "";
            player.sendMessage(Utils.getInstance().$("- " + pet.getId() + ": " + pet.getName() + owned));
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
}
