package eu.xaru.mysticrpg.player.leaderboards;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class for handling leaderboard-related operations and hologram management.
 */
public class LeaderBoardsHelper {

    private final DatabaseManager databaseManager;
    private final DebugLoggerModule logger;
    private final Map<String, HologramData> holograms = new HashMap<>();

    /**
     * Constructor for LeaderBoardsHelper.
     *
     * @param databaseManager The DatabaseManager instance for database interactions.
     * @param logger          The DebugLoggerModule for logging.
     */
    public LeaderBoardsHelper(DatabaseManager databaseManager, DebugLoggerModule logger) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null.");
        }
        if (logger == null) {
            throw new IllegalArgumentException("DebugLoggerModule cannot be null.");
        }
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Retrieves the top players based on their level.
     *
     * @param limit    The number of top players to retrieve.
     * @param callback The callback to handle the result.
     */
    public void getTopLevelPlayers(int limit, Callback<List<PlayerData>> callback) {
        databaseManager.loadAllPlayers(new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> allPlayers) {
                if (allPlayers.isEmpty()) {
                    callback.onSuccess(List.of());
                    return;
                }
                // Sort players by level in descending order
                List<PlayerData> sortedPlayers = allPlayers.stream()
                        .sorted(Comparator.comparingInt(PlayerData::getLevel).reversed())
                        .limit(limit)
                        .collect(Collectors.toList());
                callback.onSuccess(sortedPlayers);
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Failed to load all players for leaderboards: " + throwable.getMessage());
                callback.onFailure(throwable);
            }
        });
    }

    /**
     * Spawns a hologram at the given location with the specified ID.
     * The hologram is offset vertically to appear above the player's head.
     * Immediately populates the hologram with top 5 players' data.
     *
     * @param id       Unique identifier for the hologram.
     * @param location Location to spawn the hologram.
     */
    public void spawnHologram(String id, Location location) {
        if (holograms.containsKey(id)) {
            throw new IllegalArgumentException("Hologram with ID '" + id + "' already exists.");
        }

        // Offset the location upwards to appear above the player's head
        Location adjustedLocation = location.clone().add(0, 2, 0); // Adjust Y as needed

        // Create the hologram using DHAPI
        Hologram hologram = DHAPI.createHologram(id, adjustedLocation);

        if (hologram == null) {
            throw new IllegalArgumentException("Failed to create hologram with ID: " + id);
        }

        // Initialize hologram with 7 lines
        initializeHologramLines(id, hologram);

        // Immediately populate the hologram with data
        updateHologramContent(id, hologram);

        // Schedule periodic updates every 60 seconds
        BukkitTask updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHologramContent(id, hologram);
            }
        }.runTaskTimer(MysticCore.getInstance(), 600L, 600L); // 600L = 30 seconds

        // Store hologram data
        HologramData hologramData = new HologramData(id, hologram, updateTask);
        holograms.put(id, hologramData);

        logger.log(Level.INFO, "Hologram spawned with ID: " + id + " at " + adjustedLocation, 0);
    }

    /**
     * Removes the hologram with the specified ID.
     *
     * @param id Unique identifier for the hologram.
     */
    public void removeHologram(String id) {
        HologramData hologramData = holograms.get(id);
        if (hologramData == null) {
            throw new IllegalArgumentException("No hologram found with ID: " + id);
        }

        // Cancel the update task
        hologramData.cancelUpdateTask();

        // Remove the hologram using DHAPI by ID
        DHAPI.removeHologram(id);

        // Remove from the map
        holograms.remove(id);

        logger.log(Level.INFO, "Hologram with ID: " + id + " has been removed.", 0);
    }

    /**
     * Initializes the hologram with 7 predefined lines.
     *
     * @param id       Unique identifier for the hologram.
     * @param hologram The hologram instance to initialize.
     */
    private void initializeHologramLines(String id, Hologram hologram) {
        // Define the 7 lines
        List<String> initialLines = List.of(
                ChatColor.GREEN + "=== Leaderboard ===", // Line 0: Title
                ChatColor.YELLOW + "#1: ",               // Line 1: Top player
                ChatColor.YELLOW + "#2: ",               // Line 2
                ChatColor.YELLOW + "#3: ",               // Line 3
                ChatColor.YELLOW + "#4: ",               // Line 4
                ChatColor.YELLOW + "#5: ",               // Line 5
                ChatColor.GREEN + "=================="    // Line 6: Separator
        );

        // Add the 7 lines to the hologram
        for (String line : initialLines) {
            DHAPI.addHologramLine(hologram, line);
        }

        logger.log(Level.INFO, "Initialized hologram '" + id + "' with 7 lines.", 0);
    }

    /**
     * Updates the content of the specified hologram.
     * Populates the top 5 players' data into the hologram.
     *
     * @param id       Unique identifier for the hologram.
     * @param hologram The hologram instance to update.
     */
    private void updateHologramContent(String id, Hologram hologram) {
        getTopLevelPlayers(5, new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> topPlayers) {
                // Always set the title and separator
                try {
                    DHAPI.setHologramLine(hologram, 0, ChatColor.GREEN + "=== Leaderboard ===");
                    DHAPI.setHologramLine(hologram, 6, ChatColor.GREEN + "==================");
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to set static lines for hologram '" + id + "': " + e.getMessage());
                }

                // Update top 5 player lines
                for (int i = 0; i < 5; i++) {
                    if (i < topPlayers.size()) {
                        PlayerData pd = topPlayers.get(i);
                        UUID playerUUID = UUID.fromString(pd.getUuid());
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                        String playerName = (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) ? offlinePlayer.getName() : "Unknown";
                        String lineContent = ChatColor.YELLOW + "#" + (i + 1) + ": " + playerName + " - Level " + pd.getLevel();
                        try {
                            DHAPI.setHologramLine(hologram, i + 1, lineContent);
                        } catch (IllegalArgumentException e) {
                            logger.error("Failed to set hologram line " + (i + 1) + " for hologram '" + id + "': " + e.getMessage());
                        }
                    } else {
                        // If there are fewer than 5 players, display "N/A"
                        try {
                            DHAPI.setHologramLine(hologram, i + 1, ChatColor.YELLOW + "#" + (i + 1) + ": " + "N/A");
                        } catch (IllegalArgumentException e) {
                            logger.error("Failed to set hologram line " + (i + 1) + " for hologram '" + id + "': " + e.getMessage());
                        }
                    }
                }

                logger.log(Level.INFO, "Hologram with ID: " + id + " has been updated with top players.", 0);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // Display an error message on the first player line
                try {
                    DHAPI.setHologramLine(hologram, 1, ChatColor.RED + "Error fetching data.");
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to set error message for hologram '" + id + "': " + e.getMessage());
                }

                logger.error("Failed to update hologram with ID: " + id + ". " + throwable.getMessage());
            }
        });
    }

    /**
     * Retrieves a list of all current hologram IDs.
     *
     * @return List of hologram IDs.
     */
    public List<String> getAllHologramIds() {
        return new ArrayList<>(holograms.keySet());
    }

    /**
     * Removes all holograms managed by this helper.
     * Should be called during plugin disable to clean up.
     */
    public void removeAllHolograms() {
        for (String id : new ArrayList<>(holograms.keySet())) {
            try {
                removeHologram(id);
            } catch (IllegalArgumentException e) {
                logger.error("Failed to remove hologram '" + id + "': " + e.getMessage());
            }
        }
    }

    /**
     * Inner class to store hologram data.
     */
    private static class HologramData {
        private final String id;
        private final Hologram hologram;
        private final BukkitTask updateTask;

        public HologramData(String id, Hologram hologram, BukkitTask updateTask) {
            this.id = id;
            this.hologram = hologram;
            this.updateTask = updateTask;
        }

        public void cancelUpdateTask() {
            if (updateTask != null && !updateTask.isCancelled()) {
                updateTask.cancel();
            }
        }
    }
}
