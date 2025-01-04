package eu.xaru.mysticrpg.pets;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.stats.PlayerStatsManager;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        petHelper = new PetHelper(plugin);

        // If we want to initialize PetStatManager with StatsModule
        StatsModule statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        if (statsModule != null) {
            PlayerStatsManager statsManager = statsModule.getStatsManager();
            PetStatManager.init(statsManager);
        }

        registerCommands();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(petHelper, plugin);

     //   DebugLogger.getInstance().log(Level.INFO, "PetsModule initialized successfully.", 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

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
                                    if (info.sender() instanceof Player p) {
                                        return petHelper.getOwnedPetIds(p);
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // This loads the pet's level/XP from file into memory,
        // so if you do `equipPet(...)` or check data, it won't be reset to 1/0.
        PetFileStorage.loadPlayerPets(player);

        // If you also want to re-equip automatically:
        PlayerData data = PlayerDataCache.getInstance().getCachedPlayerData(player.getUniqueId());
        if (data != null) {
            String equippedPetId = data.getEquippedPet();
            if (equippedPetId != null) {
                // This will re-equip the same pet, restoring its level & XP from PetFileStorage
                petHelper.equipPet(player, equippedPetId);
                player.sendMessage(Utils.getInstance().$("Re-equipped your pet: " + equippedPetId));
            }
        }
    }

    public PetHelper getPetHelper() {
        return petHelper;
    }
}
