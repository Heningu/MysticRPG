package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TitleManager is responsible for managing player prefixes and level suffixes.
 * It updates the player's display name in the format [PREFIX] USERNAME [LVL1].
 * Prefixes are automatically retrieved from LuckPerms and updated live.
 */
public class TitleManager implements Listener {

    // Reference to your custom ScoreboardManager
    private final ScoreboardManager scoreboardManager;

    // Reference to the main plugin instance
    private final JavaPlugin plugin;

    // LuckPerms instance
    private final LuckPerms luckPerms;

    // To prevent multiple scheduled tasks
    private final AtomicBoolean isScheduled = new AtomicBoolean(false);

    /**
     * Constructs a new TitleManager instance.
     * Initializes scoreboard management and integrates with LuckPerms for prefix retrieval.
     *
     * @param plugin            The main plugin instance.
     * @param scoreboardManager The ScoreboardManager instance.
     */
    public TitleManager(JavaPlugin plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;

        // Initialize LuckPerms
        this.luckPerms = LuckPermsProvider.get();

        // Register Bukkit event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register LuckPerms event listener for track changes
        registerLuckPermsListeners();

        // Schedule periodic prefix updates to catch any missed changes
        schedulePeriodicPrefixUpdates();
    }

    /**
     * Registers LuckPerms event listeners to handle live prefix updates.
     */
    private void registerLuckPermsListeners() {
        // Subscribe to UserTrackEvent to detect group changes via tracks
        luckPerms.getEventBus().subscribe(UserTrackEvent.class, event -> {
            // Check if the event is about a player being added or removed from a group in a track
            UUID uuid = event.getUser().getUniqueId();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Schedule the update on the main server thread to ensure thread safety
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getLogger().info("[MysticRPG] Detected group change for player " + player.getName() + ". Updating display name.");
                    updatePlayerDisplayName(player);
                });
            }
        });
    }

    /**
     * Schedules a periodic task to update all online players' display names.
     * This helps catch any prefix changes not captured by event listeners.
     */
    private void schedulePeriodicPrefixUpdates() {
        if (isScheduled.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerDisplayName(player);
                }
            }, 100L, 100L); // Runs every 5 seconds (100 ticks)
            Bukkit.getLogger().info("[MysticRPG] Scheduled periodic prefix updates every 5 seconds.");
        }
    }

    /**
     * Updates the player's display name with the current LuckPerms prefix and level suffix.
     *
     * @param player The player to update.
     */
    public void updatePlayerDisplayName(Player player) {
        PlayerDataCache playerDataCache = getPlayerDataCache();
        if (playerDataCache == null) {
            player.sendMessage(ChatColor.RED + "Error: PlayerDataCache not available.");
            Bukkit.getLogger().warning("[MysticRPG] PlayerDataCache is null. Cannot update display name for " + player.getName());
            return;
        }

        UUID uuid = player.getUniqueId();

        // Retrieve prefix from LuckPerms; default to empty string if not set
        String prefix = getPrefixFromLuckPerms(player);
        if (prefix == null) {
            prefix = "";
        }

        // Retrieve player's level; default to 1 if not found
        int level = playerDataCache.getPlayerLevel(uuid);
        String suffix = ChatColor.BLACK + " [" + ChatColor.GREEN + "LVL" + ChatColor.RED + level + ChatColor.BLACK + "]";

        // Logging for debugging
        Bukkit.getLogger().info("[MysticRPG] Updating display name for " + player.getName() + ": Prefix='" + prefix + "', Suffix='" + suffix + "'");

        // Update display name and player list name via scoreboard team
        Scoreboard scoreboard = scoreboardManager.getPlayerScoreboard(player);
        if (scoreboard == null) {
            player.sendMessage(ChatColor.RED + "Error: Player's scoreboard not found.");
            Bukkit.getLogger().warning("[MysticRPG] ScoreboardManager returned null for " + player.getName());
            return;
        }

        // Get or create a team for the player
        String teamName = "team_" + uuid.toString();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            try {
                team = scoreboard.registerNewTeam(teamName);
                Bukkit.getLogger().info("[MysticRPG] Created new team '" + teamName + "' for player " + player.getName());
            } catch (IllegalArgumentException e) {
                // Team already exists; retrieve it
                team = scoreboard.getTeam(teamName);
                Bukkit.getLogger().info("[MysticRPG] Retrieved existing team '" + teamName + "' for player " + player.getName());
            }
        }

        if (team != null) {
            // Ensure the team has the player's name as its entry
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
                Bukkit.getLogger().info("[MysticRPG] Added player " + player.getName() + " to team '" + teamName + "'");
            }

            // Set prefix and suffix with proper spacing
            if (!prefix.isEmpty()) {
                team.setPrefix(prefix + " "); // Add a space after the prefix
            } else {
                team.setPrefix(""); // No prefix
            }
            team.setSuffix(" " + suffix); // Always add space before suffix
        } else {
            player.sendMessage(ChatColor.RED + "Error: Could not create or retrieve scoreboard team.");
            Bukkit.getLogger().warning("[MysticRPG] Could not create or retrieve scoreboard team for " + player.getName());
        }

        // Update player list name (tab list)
        String displayName = prefix.isEmpty() ? (player.getName() + " " + suffix) : (prefix + " " + player.getName() + " " + suffix);
        player.setPlayerListName(displayName);

        Bukkit.getLogger().info("[MysticRPG] Set display name for " + player.getName() + " to '" + displayName + "'");
    }

    /**
     * Retrieves the prefix from LuckPerms for the specified player.
     *
     * @param player The player whose prefix is to be retrieved.
     * @return The prefix string, or an empty string if not set.
     */
    private String getPrefixFromLuckPerms(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            // User might not be cached; attempt to load
            user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
                Bukkit.getLogger().warning("[MysticRPG] Could not load LuckPerms user data for " + player.getName());
                return "";
            }
        }

        // Retrieve the highest priority prefix
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix == null) {
            return "";
        }

        // Translate color codes and return
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     * Retrieves the PlayerDataCache instance from the SaveModule.
     *
     * @return The PlayerDataCache instance, or null if not available.
     */
    private PlayerDataCache getPlayerDataCache() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            return saveModule.getPlayerDataCache();
        }
        return null;
    }

    /**
     * Cleans up all teams managed by this TitleManager.
     * This should be called during plugin disable to prevent lingering teams.
     */
    public void cleanup() {
        // Iterate through all online players and remove their teams
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Scoreboard scoreboard = scoreboardManager.getPlayerScoreboard(player);
            if (scoreboard != null) {
                String teamName = "team_" + uuid.toString();
                Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    team.removeEntry(player.getName());
                    team.unregister();
                    Bukkit.getLogger().info("[MysticRPG] Unregistered team '" + teamName + "' for player " + player.getName());
                }
            }
        }
    }

    /**
     * Handles player joining the server.
     *
     * @param event The PlayerJoinEvent.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Assign the player's specific scoreboard
        if (scoreboardManager != null) {
            scoreboardManager.createScoreboardForPlayer(player);
            scoreboardManager.updatePlayerScoreboard(player);
            Scoreboard playerScoreboard = scoreboardManager.getPlayerScoreboard(player);
            if (playerScoreboard != null) {
                player.setScoreboard(playerScoreboard);
            } else {
                Bukkit.getLogger().warning("[MysticRPG] Scoreboard for player " + player.getName() + " is null.");
            }
        }

        // Update display name with current level and prefix
        // Slight delay to ensure PlayerDataCache has loaded the player's level
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerDisplayName(player), 1L); // 1 tick delay
    }

    /**
     * Handles player quitting the server.
     *
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Remove the player's team to clean up
        Scoreboard scoreboard = scoreboardManager.getPlayerScoreboard(player);
        if (scoreboard != null) {
            String teamName = "team_" + uuid.toString();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
                team.unregister();
                Bukkit.getLogger().info("[MysticRPG] Unregistered team '" + teamName + "' for player " + player.getName());
            }
        }
    }

    /**
     * Handles player level up events.
     * Assumes that elsewhere in your plugin, this method is called when a player levels up.
     *
     * @param player The player who leveled up.
     */
    public void onPlayerLevelUp(Player player) {
        Bukkit.getLogger().info("[MysticRPG] Player " + player.getName() + " leveled up. Updating display name.");
        updatePlayerDisplayName(player);
    }
}
