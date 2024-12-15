package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

public class PetHelper implements Listener {
    private final JavaPlugin plugin;

    private final Map<String, Pet> petConfigurations = new HashMap<>();
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin) {
        this.plugin = plugin;

        loadPetConfigurations();

        // Schedule a repeating task to update pet positions and animations
        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePets, 0L, 1L); // every tick for smoother movement
    }

    /**
     * Loads pet configurations from YAML files located in the plugin's data folder.
     */
    private void loadPetConfigurations() {
        File petFolder = new File(plugin.getDataFolder(), "pets");
        if (!petFolder.exists() && !petFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create pets folder.");
            return;
        }

        File[] files = petFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String petId = config.getString("id");
                    String petName = config.getString("name");
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
                    DebugLogger.getInstance().error("Failed to load pet configuration from file " + file.getName() + ":", e);
                }
            }
        }
    }

    /**
     * Grants a pet to a player and saves the updated data to the database.
     *
     * @param player The player receiving the pet.
     * @param petId  The ID of the pet to grant.
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

        if (playerData.getOwnedPets().contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You already own this pet: " + pet.getName()));
            return;
        }

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
     * Equips a pet for a player if they own it and saves the equipped pet to the database.
     *
     * @param player The player equipping the pet.
     * @param petId  The ID of the pet to equip.
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

        // Unequip current pet if any
        unequipPet(player);

        // Spawn pet entity
        PetInstance petInstance = spawnPetEntity(player, pet);

        // Store equipped pet
        playerEquippedPets.put(player.getUniqueId(), petInstance);

        // Optionally, track equipped pet in PlayerData
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
     * Unequips the currently equipped pet for a player and updates the database.
     *
     * @param player The player unequipping their pet.
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

        PetInstance petInstance = playerEquippedPets.remove(player.getUniqueId());
        if (petInstance != null) {
            // Remove pet entity
            petInstance.getPetEntity().remove();
        }

        // Clear equipped pet in PlayerData
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
     * Retrieves a pet by its ID.
     *
     * @param petId The ID of the pet to retrieve.
     * @return The Pet object if it exists, null otherwise.
     */
    public Pet getPetById(String petId) {
        return petConfigurations.get(petId);
    }

    /**
     * Spawns an ArmorStand entity representing the pet near the player.
     *
     * @param player The player to spawn the pet for.
     * @param pet    The pet to spawn.
     * @return The instance of the spawned pet.
     */
    private PetInstance spawnPetEntity(Player player, Pet pet) {
        Location loc = player.getLocation().clone().add(0, 0.5, 0); // Adjust height as needed

        // Create the ArmorStand for the pet
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
            ItemStack headItem = new ItemStack(Material.matchMaterial(pet.getDisplayItem()));
            armorStand.getEquipment().setHelmet(headItem);
        });

        return new PetInstance(pet, petEntity);
    }

    /**
     * Updates the positions and animations of all equipped pets.
     */
    private void updatePets() {
        for (UUID playerId : playerEquippedPets.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                PetInstance petInstance = playerEquippedPets.get(playerId);
                ArmorStand petEntity = petInstance.getPetEntity();

                // Calculate the desired position for the pet
                Location playerLoc = player.getLocation().clone();

                // Offset the pet's position to be next to the player
                Vector right = getRightDirection(playerLoc.getYaw());
                Vector offset = right.multiply(1.5); // Pet is 1.5 blocks to the right
                Location petLoc = playerLoc.clone().add(offset);
                petLoc.setY(playerLoc.getY() + 0.5); // Adjust height as needed

                // Get current pet position
                Location currentPetLoc = petEntity.getLocation();

                // Calculate movement vector for smooth movement
                Vector direction = petLoc.toVector().subtract(currentPetLoc.toVector());
                double distance = direction.length();
                if (distance > 0.05) {
                    direction.normalize();
                    // Adjust speed as needed
                    double speed = 0.2; // You can adjust this value
                    Vector velocity = direction.multiply(speed);
                    petEntity.teleport(currentPetLoc.add(velocity));
                }

                // Animate up and down movement using sine wave
                double animationPhase = petInstance.getAnimationPhase();
                double deltaY = Math.sin(animationPhase) * 0.1; // Amplitude of 0.1 blocks
                petEntity.teleport(petEntity.getLocation().add(0, deltaY, 0));
                petInstance.setAnimationPhase(animationPhase + 0.1); // Adjust increment for speed

                // Rotate the ArmorStand to face the same direction as the player
                petEntity.setRotation(playerLoc.getYaw(), petEntity.getLocation().getPitch());

                // Optional: Tilt the head slightly
                petEntity.setHeadPose(new EulerAngle(0, 0, 0));

                // Ensure the pet doesn't get inside the player by maintaining a minimum distance
                if (distance < 1.0) {
                    Vector pushBack = currentPetLoc.toVector().subtract(playerLoc.toVector()).normalize().multiply(0.5);
                    petEntity.teleport(currentPetLoc.add(pushBack));
                }
            }
        }
    }

    /**
     * Calculates the right direction vector based on the player's yaw.
     *
     * @param yaw The yaw of the player.
     * @return A Vector pointing to the player's right.
     */
    private Vector getRightDirection(float yaw) {
        // Convert yaw to radians and adjust
        double yawRad = Math.toRadians(-yaw);
        return new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));
    }

    /**
     * Retrieves all available pet IDs.
     *
     * @return An array of available pet IDs.
     */
    public String[] getAvailablePetIds() {
        return petConfigurations.keySet().toArray(new String[0]);
    }

    /**
     * Retrieves all pet IDs owned by a specific player.
     *
     * @param player The player whose owned pets are to be retrieved.
     * @return An array of owned pet IDs.
     */
    public String[] getOwnedPetIds(Player player) {
        PlayerDataCache cache = PlayerDataCache.getInstance();
        PlayerData playerData = cache.getCachedPlayerData(player.getUniqueId());
        if (playerData != null) {
            Set<String> ownedPets = playerData.getOwnedPets();
            if(ownedPets == null) {
                ownedPets = new HashSet<>();
            }
            return ownedPets.toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    /**
     * Lists all available pets to the player, indicating which ones they own.
     *
     * @param player The player to list pets for.
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

    /**
     * Automatically unequips a player's pet when they quit the server.
     *
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unequipPet(event.getPlayer());
    }

    /**
     * Automatically unequips a player's pet when they die.
     *
     * @param event The PlayerDeathEvent.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        unequipPet(event.getEntity());
    }
}
