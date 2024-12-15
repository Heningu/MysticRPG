package eu.xaru.mysticrpg.player.leaderboards;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The main module class for managing Leaderboards.
 * Handles initialization, command registration, and event handling.
 */
public class LeaderBoardsModule implements IBaseModule {

    private MysticCore plugin;
    private EventManager eventManager;
    
    private DatabaseManager databaseManager;
    private LeaderBoardsHelper leaderBoardsHelper;

    /**
     * Initializes the LeaderBoardsModule by setting up necessary components.
     */
    @Override
    public void initialize() {
        // Retrieve the debug logger


        DebugLogger.getInstance().log(Level.INFO, "Initializing LeaderBoardsModule...", 0);

        // Retrieve the main plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        // Initialize the event manager
        this.eventManager = new EventManager(plugin);

        // Initialize the DatabaseManager from the SaveModule dependency
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            this.databaseManager = DatabaseManager.getInstance();
            if (this.databaseManager != null) {
                this.leaderBoardsHelper = new LeaderBoardsHelper(databaseManager);
                DebugLogger.getInstance().log(Level.INFO, "LeaderBoardsHelper initialized successfully.", 0);
            } else {
                DebugLogger.getInstance().log("DatabaseManager is not initialized in SaveModule. LeaderBoardsModule will not function.");
                throw new IllegalStateException("DatabaseManager is null in SaveModule.");
            }
        } else {
            DebugLogger.getInstance().error("SaveModule not initialized. LeaderBoardsModule will not function without it.");
            throw new IllegalStateException("SaveModule is not loaded.");
        }

        // Optionally load persisted holograms
        // leaderBoardsHelper.loadPersistedHolograms();

        DebugLogger.getInstance().log(Level.INFO, "LeaderBoardsModule initialization complete.", 0);
    }

    /**
     * Starts the LeaderBoardsModule by registering commands and event handlers.
     */
    @Override
    public void start() {
        registerCommands();
        // Currently, no event handlers are needed for the leaderboard functionality
    }

    /**
     * Stops the LeaderBoardsModule. Placeholder for any necessary cleanup.
     */
    @Override
    public void stop() {
        // Any necessary cleanup can be performed here
    }

    /**
     * Unloads the LeaderBoardsModule. Placeholder for any necessary unload actions.
     */
    @Override
    public void unload() {
        // Any necessary unload actions can be performed here
    }

    /**
     * Specifies the dependencies required by the LeaderBoardsModule.
     *
     * @return A list of module classes that LeaderBoardsModule depends on.
     */
    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( SaveModule.class);
    }

    /**
     * Specifies the priority level of the LeaderBoardsModule.
     *
     * @return The module priority.
     */
    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Registers the leaderboard commands using the CommandAPI.
     * Includes commands to display leaderboards and manage holograms.
     */
    private void registerCommands() {
        // Root command: /leaderboards or /lb
        new CommandAPICommand("leaderboards")
                .withAliases("lb")
                // Subcommand: /leaderboards (without additional arguments)
                .executesPlayer((player, args) -> {
                    player.sendMessage(ChatColor.GOLD + "Fetching top level players. Please wait...");
                    if (leaderBoardsHelper != null) {
                        leaderBoardsHelper.getTopLevelPlayers(10, new Callback<List<PlayerData>>() {
                            @Override
                            public void onSuccess(List<PlayerData> topPlayers) {
                                if (topPlayers.isEmpty()) {
                                    player.sendMessage(Utils.getInstance().$("No player data available."));
                                    return;
                                }
                                player.sendMessage(ChatColor.GREEN + "=== Top " + topPlayers.size() + " Players by Level ===");
                                int rank = 1;
                                for (PlayerData pd : topPlayers) {
                                    UUID playerUUID = UUID.fromString(pd.getUuid());
                                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                                    String playerName = (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) ? offlinePlayer.getName() : "Unknown";
                                    player.sendMessage(ChatColor.YELLOW + "#" + rank + ": " + playerName + " - Level " + pd.getLevel());
                                    rank++;
                                }
                                player.sendMessage(ChatColor.GREEN + "=== End of Leaderboards ===");
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                player.sendMessage(ChatColor.RED + "Failed to retrieve leaderboards. Please try again later.");
                                DebugLogger.getInstance().error("Error fetching leaderboards: ", throwable);
                            }
                        });
                    } else {
                        player.sendMessage(ChatColor.RED + "Leaderboard system is currently unavailable.");
                        DebugLogger.getInstance().error("LeaderBoardsHelper is null. Cannot fetch leaderboards.");
                    }
                })
                // Subcommand: /leaderboards holospawn <ID>
                .withSubcommand(new CommandAPICommand("holospawn")
                        .withArguments(new StringArgument("ID")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> leaderBoardsHelper.getAllHologramIds().toArray(new String[0]))))
                        .withPermission("mysticrpg.leaderboards.holospawn") // Permission check
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("ID");
                            Location location = player.getLocation();
                            try {
                                leaderBoardsHelper.spawnHologram(id, location);
                                player.sendMessage(ChatColor.GREEN + "Hologram '" + id + "' spawned at your location.");
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(ChatColor.RED + "Error spawning hologram:" + e.getMessage());
                                DebugLogger.getInstance().error("Error spawning hologram '" + id + "':", e);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An unexpected error occurred while spawning the hologram.");
                                DebugLogger.getInstance().error("Unexpected error spawning hologram '" + id + "':", e);
                            }
                        }))
                // Subcommand: /leaderboards holoremove <ID>
                .withSubcommand(new CommandAPICommand("holoremove")
                        .withArguments(new StringArgument("ID")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> leaderBoardsHelper.getAllHologramIds().toArray(new String[0]))))
                        .withPermission("mysticrpg.leaderboards.holoremove") // Permission check
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("ID");
                            try {
                                leaderBoardsHelper.removeHologram(id);
                                player.sendMessage(ChatColor.GREEN + "Hologram '" + id + "' has been removed.");
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(ChatColor.RED + "Error removing hologram:" + e.getMessage());
                                DebugLogger.getInstance().error("Error removing hologram '" + id + "':", e);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An unexpected error occurred while removing the hologram.");
                                DebugLogger.getInstance().error("Unexpected error removing hologram '" + id + "':", e);
                            }
                        }))
                .register();

        DebugLogger.getInstance().log(Level.INFO, "LeaderBoardsModule commands registered.", 0);
    }

    /**
     * Cleanup method to remove all holograms when the plugin is disabled.
     * Should be called from the plugin's onDisable method.
     */
    public void cleanup() {
        if (leaderBoardsHelper != null) {
            leaderBoardsHelper.removeAllHolograms();
            DebugLogger.getInstance().log(Level.INFO, "All holograms have been removed during cleanup.", 0);
        }
    }
}
