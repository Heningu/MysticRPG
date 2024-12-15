package eu.xaru.mysticrpg.pets;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class PetsModule implements IBaseModule, Listener {
    private final JavaPlugin plugin;

    private PetHelper petHelper;

    public PetsModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        // Initialize PetHelper
        petHelper = new PetHelper(plugin);

        // Register commands
        registerCommands();

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(petHelper, plugin);

        DebugLogger.getInstance().log(Level.INFO, "PetsModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "PetsModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "PetsModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "PetsModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Registers all pet-related commands using CommandAPI.
     */
    private void registerCommands() {
        new CommandAPICommand("pets")
                .withPermission("mysticcore.pets")
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new StringArgument("petId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> petHelper.getAvailablePetIds())))
                        .executesPlayer((player, args) -> {
                            String petId = (String) args.get(0);
                            petHelper.givePet(player, petId);
                        }))
                .withSubcommand(new CommandAPICommand("equip")
                        .withArguments(new StringArgument("petId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    if (info.sender() instanceof Player player) {
                                        return petHelper.getOwnedPetIds(player);
                                    } else {
                                        return new String[0];
                                    }
                                })))
                        .executesPlayer((player, args) -> {
                            String petId = (String) args.get(0);
                            petHelper.equipPet(player, petId);
                        }))
                .withSubcommand(new CommandAPICommand("unequip")
                        .executesPlayer((player, args) -> {
                            petHelper.unequipPet(player);
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            petHelper.listPets(player);
                        }))
                .register();
    }

//    /**
//     * Handles player join events to ensure pet data is loaded and pets are equipped if necessary.
//     *
//     * @param event The PlayerJoinEvent.
//     */
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        PlayerDataCache cache = PlayerDataCache.getInstance();
//
//        // Load player data asynchronously
//        cache.loadPlayerData(player.getUniqueId(), new Callback<PlayerData>() {
//            @Override
//            public void onSuccess(PlayerData playerData) {
//                DebugLogger.getInstance().log(Level.INFO, "Loaded player data for " + player.getName(), 0);
//                String equippedPetId = playerData.getEquippedPet();
//                if (equippedPetId != null && !equippedPetId.isEmpty()) {
//                    // Equip the pet if it's owned
//                    petHelper.equipPet(player, equippedPetId);
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable throwable) {
//                DebugLogger.getInstance().error("Failed to load player data for " + player.getName(), throwable);
//                player.sendMessage(Utils.getInstance().$("Failed to load your pet data. Please contact an administrator."));
//            }
//        });
//    }


    public PetHelper getPetHelper() {
        return petHelper;
    }
}
