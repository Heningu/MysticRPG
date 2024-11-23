package eu.xaru.mysticrpg.pets;

import eu.xaru.mysticrpg.utils.DebugLoggerModule;
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
    private final DebugLoggerModule logger;

    private final Map<String, Pet> petConfigurations = new HashMap<>();
    private final Map<UUID, Set<String>> playerOwnedPets = new HashMap<>();
    private final Map<UUID, PetInstance> playerEquippedPets = new HashMap<>();

    public PetHelper(JavaPlugin plugin, DebugLoggerModule logger) {
        this.plugin = plugin;
        this.logger = logger;

        loadPetConfigurations();

        // Schedule a repeating task to update pet positions and animations
        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePets, 0L, 1L); // every tick for smoother movement
    }

    private void loadPetConfigurations() {
        File petFolder = new File(plugin.getDataFolder(), "pets");
        if (!petFolder.exists() && !petFolder.mkdirs()) {
            logger.error("Failed to create pets folder.");
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
                        logger.error("Pet ID is missing in file: " + file.getName());
                        continue;
                    }
                    if (petName == null || petName.isEmpty()) {
                        logger.error("Pet name is missing in file: " + file.getName());
                        continue;
                    }

                    String displayItem = config.getString("display_item", "ZOMBIE_HEAD");

                    Pet pet = new Pet(petId, petName, displayItem);
                    petConfigurations.put(petId, pet);

                    logger.log(Level.INFO, "Loaded pet configuration: " + petId, 0);
                } catch (Exception e) {
                    logger.error("Failed to load pet configuration from file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public void givePet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }

        playerOwnedPets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(petId);
        player.sendMessage(Utils.getInstance().$("You have received pet: " + pet.getName()));
    }

    public void equipPet(Player player, String petId) {
        Pet pet = petConfigurations.get(petId);
        if (pet == null) {
            player.sendMessage(Utils.getInstance().$("Pet not found: " + petId));
            return;
        }

        Set<String> ownedPets = playerOwnedPets.get(player.getUniqueId());
        if (ownedPets == null || !ownedPets.contains(petId)) {
            player.sendMessage(Utils.getInstance().$("You do not own pet: " + pet.getName()));
            return;
        }

        // Unequip current pet if any
        unequipPet(player);

        // Spawn pet entity
        PetInstance petInstance = spawnPetEntity(player, pet);

        // Store equipped pet
        playerEquippedPets.put(player.getUniqueId(), petInstance);

        player.sendMessage(Utils.getInstance().$("You have equipped pet: " + pet.getName()));
    }

    public void unequipPet(Player player) {
        PetInstance petInstance = playerEquippedPets.remove(player.getUniqueId());
        if (petInstance != null) {
            // Remove pet entity
            petInstance.getPetEntity().remove();

            player.sendMessage(Utils.getInstance().$("You have unequipped your pet."));
        } else {
            player.sendMessage(Utils.getInstance().$("You have no pet equipped."));
        }
    }

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

    private Vector getRightDirection(float yaw) {
        // Convert yaw to radians and adjust
        double yawRad = Math.toRadians(-yaw);
        return new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));
    }

    public String[] getAvailablePetIds() {
        return petConfigurations.keySet().toArray(new String[0]);
    }

    public String[] getOwnedPetIds(Player player) {
        Set<String> ownedPets = playerOwnedPets.getOrDefault(player.getUniqueId(), Collections.emptySet());
        return ownedPets.toArray(new String[0]);
    }

    public void listPets(Player player) {
        player.sendMessage(Utils.getInstance().$("Available Pets:"));
        for (Pet pet : petConfigurations.values()) {
            player.sendMessage(Utils.getInstance().$("- " + pet.getId() + ": " + pet.getName()));
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
