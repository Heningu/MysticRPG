package eu.xaru.mysticrpg.player.leaderboards;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.discord.DiscordModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class LeaderBoardsModule implements IBaseModule {

    private MysticCore plugin;
    private EventManager eventManager;

    private DatabaseManager databaseManager;
    private LeaderBoardsHelper leaderBoardsHelper;

    @Override
    public void initialize() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            this.databaseManager = DatabaseManager.getInstance();
            if (this.databaseManager != null) {
                this.leaderBoardsHelper = new LeaderBoardsHelper(databaseManager);
               // DebugLogger.getInstance().log(Level.INFO, "LeaderBoardsHelper initialized successfully.", 0);

                // Set DiscordUpdateHandler if needed from DiscordModule
                this.leaderBoardsHelper.setDiscordUpdateHandler((type, topPlayers) -> {
                    // Update Discord embed if DiscordModule is available
                    DiscordModule discordModule = ModuleManager.getInstance().getModuleInstance(DiscordModule.class);
                    if (discordModule != null) {
                        discordModule.updateLeaderboardEmbed(type, topPlayers);
                    }
                });

            } else {
                DebugLogger.getInstance().log("DatabaseManager is not initialized in SaveModule. LeaderBoardsModule will not function.");
                throw new IllegalStateException("DatabaseManager is null.");
            }
        } else {
            DebugLogger.getInstance().error("SaveModule not initialized. LeaderBoardsModule cannot function without it.");
            throw new IllegalStateException("SaveModule is not loaded.");
        }

        DebugLogger.getInstance().log(Level.INFO, "Leaderboards init complete.", 0);
    }

    @Override
    public void start() {
        registerCommands();

        // Delay hologram loading to ensure DecentHolograms is ready
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (leaderBoardsHelper != null) {
                leaderBoardsHelper.loadHologramsFromFile();
                DebugLogger.getInstance().log(Level.INFO, "Holograms loaded from file after delay.", 0);
            }
        }, 120L); // 6 seconds delay (60 ticks), adjust if needed

        // Refresh all scoreboards every 1 min
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 1200L, 1200L);
    }

    private void updateAllScoreboards() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
        if (leaderBoardsHelper != null) {
            leaderBoardsHelper.saveHologramsToFile();
        }
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void registerCommands() {
        new CommandAPICommand("leaderboards")
                .withAliases("lb")
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
                .withSubcommand(new CommandAPICommand("holospawn")
                        .withArguments(new StringArgument("ID"))
                        .withArguments(new StringArgument("type")
                                .replaceSuggestions(ArgumentSuggestions.strings("LEVEL", "RICH")))
                        .withPermission("mysticrpg.leaderboards.holospawn")
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("ID");
                            String typeStr = (String) args.get("type");
                            LeaderboardType type = LeaderboardType.valueOf(typeStr.toUpperCase(Locale.ROOT));

                            Location location = player.getLocation();
                            try {
                                leaderBoardsHelper.spawnHologram(id, location, type, true);
                                player.sendMessage(ChatColor.GREEN + "Hologram '" + id + "' (" + type + ") spawned at your location.");
                                leaderBoardsHelper.saveHologramsToFile();
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(ChatColor.RED + "Error spawning hologram:" + e.getMessage());
                                DebugLogger.getInstance().error("Error spawning hologram '" + id + "':", e);
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "An unexpected error occurred while spawning the hologram.");
                                DebugLogger.getInstance().error("Unexpected error spawning hologram '" + id + "':", e);
                            }
                        }))
                .withSubcommand(new CommandAPICommand("holoremove")
                        .withArguments(new StringArgument("ID")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    if (leaderBoardsHelper != null) {
                                        return leaderBoardsHelper.getAllHologramIds().toArray(new String[0]);
                                    }
                                    return new String[0];
                                })))
                        .withPermission("mysticrpg.leaderboards.holoremove")
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("ID");
                            try {
                                leaderBoardsHelper.removeHologram(id);
                                player.sendMessage(ChatColor.GREEN + "Hologram '" + id + "' has been removed.");
                                leaderBoardsHelper.saveHologramsToFile();
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

    public void cleanup() {
        if (leaderBoardsHelper != null) {
            leaderBoardsHelper.saveHologramsToFile();
            DebugLogger.getInstance().log(Level.INFO, "Holograms saved during cleanup.", 0);
        }
    }
}
