package eu.xaru.mysticrpg.player.leaderboards;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LeaderBoardsHelper {

    private final DatabaseManager databaseManager;

    private final Map<String, HologramData> holograms = new HashMap<>();

    private File hologramsFile;
    private YamlConfiguration hologramsConfig;

    private DiscordUpdateHandler discordUpdateHandler;

    public LeaderBoardsHelper(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null.");
        }

        this.databaseManager = databaseManager;

        File leaderboardsFolder = new File(MysticCore.getInstance().getDataFolder(), "leaderboards");
        if (!leaderboardsFolder.exists()) {
            leaderboardsFolder.mkdirs();
        }

        hologramsFile = new File(leaderboardsFolder, "holograms.yml");
        if (!hologramsFile.exists()) {
            try {
                hologramsFile.createNewFile();
            } catch (IOException e) {
                DebugLogger.getInstance().error("Failed to create holograms.yml file:", e);
            }
        }

        hologramsConfig = new YamlConfiguration();
        try {
            hologramsConfig.load(hologramsFile);
        } catch (IOException | InvalidConfigurationException e) {
            DebugLogger.getInstance().error("Failed to load holograms.yml:", e);
        }
    }

    public void setDiscordUpdateHandler(DiscordUpdateHandler handler) {
        this.discordUpdateHandler = handler;
    }

    public void getTopLevelPlayers(int limit, Callback<List<PlayerData>> callback) {
        databaseManager.getPlayerRepository().loadAll(new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> allPlayers) {
                if (allPlayers.isEmpty()) {
                    callback.onSuccess(List.of());
                    return;
                }
                List<PlayerData> sortedPlayers = allPlayers.stream()
                        .sorted(Comparator.comparingInt(PlayerData::getLevel).reversed())
                        .limit(limit)
                        .collect(Collectors.toList());
                callback.onSuccess(sortedPlayers);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().error("Failed to load players for level leaderboards: ", throwable);
                callback.onFailure(throwable);
            }
        });
    }

    public void getTopRichPlayers(int limit, Callback<List<PlayerData>> callback) {
        databaseManager.getPlayerRepository().loadAll(new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> allPlayers) {
                if (allPlayers.isEmpty()) {
                    callback.onSuccess(List.of());
                    return;
                }
                List<PlayerData> sortedPlayers = allPlayers.stream()
                        .sorted((p1, p2) -> Integer.compare(p2.getBankGold(), p1.getBankGold()))
                        .limit(limit)
                        .collect(Collectors.toList());
                callback.onSuccess(sortedPlayers);
            }

            @Override
            public void onFailure(Throwable throwable) {
                DebugLogger.getInstance().error("Failed to load players for rich leaderboards: ", throwable);
                callback.onFailure(throwable);
            }
        });
    }

    public void spawnHologram(String id, Location location, LeaderboardType type, boolean applyOffset) {
        if (holograms.containsKey(id)) {
            throw new IllegalArgumentException("Hologram with ID '" + id + "' already exists.");
        }

        Location spawnLocation = location.clone();
        if (applyOffset) {
            // Only add the offset if this is a new hologram from the command
            spawnLocation.add(0, 2, 0);
        }

        Hologram hologram = DHAPI.createHologram(id, spawnLocation);

        if (hologram == null) {
            throw new IllegalArgumentException("Failed to create hologram with ID: " + id);
        }

        initializeHologramLines(id, hologram);
        updateHologramContent(id, hologram, type);

        BukkitTask updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHologramContent(id, hologram, type);
            }
        }.runTaskTimer(MysticCore.getInstance(), 600L, 600L);

        HologramData hologramData = new HologramData(id, hologram, updateTask, type, spawnLocation);
        holograms.put(id, hologramData);
        DebugLogger.getInstance().log(Level.INFO, "Hologram spawned with ID: " + id + " at " + spawnLocation, 0);

        // Save after spawning
        saveHologramsToFile();
    }


    public void removeHologram(String id) {
        HologramData hologramData = holograms.get(id);
        if (hologramData == null) {
            throw new IllegalArgumentException("No hologram found with ID: " + id);
        }

        hologramData.cancelUpdateTask();
        DHAPI.removeHologram(id);
        holograms.remove(id);

        DebugLogger.getInstance().log(Level.INFO, "Hologram with ID: " + id + " has been removed.", 0);

        // Save after removing
        saveHologramsToFile();
    }

    private void initializeHologramLines(String id, Hologram hologram) {
        List<String> initialLines = List.of(
                ChatColor.GREEN + "=== Leaderboard ===",
                ChatColor.YELLOW + "#1: ",
                ChatColor.YELLOW + "#2: ",
                ChatColor.YELLOW + "#3: ",
                ChatColor.YELLOW + "#4: ",
                ChatColor.YELLOW + "#5: ",
                ChatColor.GREEN + "=================="
        );

        for (String line : initialLines) {
            DHAPI.addHologramLine(hologram, line);
        }

        DebugLogger.getInstance().log(Level.INFO, "Initialized hologram '" + id + "' with 7 lines.", 0);
    }

    private void updateHologramContent(String id, Hologram hologram, LeaderboardType type) {
        Callback<List<PlayerData>> callback = new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> topPlayers) {
                try {
                    String title = (type == LeaderboardType.LEVEL) ? "=== Top Level ===" : "=== Top Rich ===";
                    DHAPI.setHologramLine(hologram, 0, ChatColor.GREEN + title);
                    DHAPI.setHologramLine(hologram, 6, ChatColor.GREEN + "==================");
                } catch (IllegalArgumentException e) {
                    DebugLogger.getInstance().error("Failed to set static lines for hologram '" + id + "':", e);
                }

                for (int i = 0; i < 5; i++) {
                    if (i < topPlayers.size()) {
                        PlayerData pd = topPlayers.get(i);
                        UUID playerUUID = UUID.fromString(pd.getUuid());
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                        String playerName = (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) ? offlinePlayer.getName() : "Unknown";

                        String lineContent;
                        if (type == LeaderboardType.LEVEL) {
                            lineContent = ChatColor.YELLOW + "#" + (i + 1) + ": " + playerName + " - Level " + pd.getLevel();
                        } else {
                            lineContent = ChatColor.YELLOW + "#" + (i + 1) + ": " + playerName + " - Gold " + pd.getBankGold();
                        }

                        try {
                            DHAPI.setHologramLine(hologram, i + 1, lineContent);
                        } catch (IllegalArgumentException e) {
                            DebugLogger.getInstance().error("Failed to set hologram line " + (i + 1) + " for hologram '" + id + "':", e);
                        }
                    } else {
                        try {
                            DHAPI.setHologramLine(hologram, i + 1, ChatColor.YELLOW + "#" + (i + 1) + ": N/A");
                        } catch (IllegalArgumentException e) {
                            DebugLogger.getInstance().error("Failed to set hologram line " + (i + 1) + " for hologram '" + id + "':", e);
                        }
                    }
                }

                DebugLogger.getInstance().log(Level.INFO, "Hologram with ID: " + id + " has been updated.", 0);

                // Update Discord embed if available
                if (discordUpdateHandler != null) {
                    discordUpdateHandler.updateDiscordEmbed(type, topPlayers);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                try {
                    DHAPI.setHologramLine(hologram, 1, ChatColor.RED + "Error fetching data.");
                } catch (IllegalArgumentException e) {
                    DebugLogger.getInstance().error("Failed to set error message for hologram '" + id + "':", e);
                }

                DebugLogger.getInstance().error("Failed to update hologram with ID: " + id + ". ", throwable);
            }
        };

        if (type == LeaderboardType.LEVEL) {
            getTopLevelPlayers(5, callback);
        } else {
            getTopRichPlayers(5, callback);
        }
    }

    public List<String> getAllHologramIds() {
        return new ArrayList<>(holograms.keySet());
    }

    public void removeAllHolograms() {
        for (String id : new ArrayList<>(holograms.keySet())) {
            try {
                removeHologram(id);
            } catch (IllegalArgumentException e) {
                DebugLogger.getInstance().error("Failed to remove hologram '" + id + "':", e);
            }
        }
    }

    public void saveHologramsToFile() {
        // Start fresh with a new YamlConfiguration to avoid stale data
        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.createSection("holograms");

        for (Map.Entry<String, HologramData> entry : holograms.entrySet()) {
            String id = entry.getKey();
            HologramData data = entry.getValue();
            String path = "holograms." + id;
            newConfig.set(path + ".world", data.location.getWorld().getName());
            newConfig.set(path + ".x", data.location.getX());
            newConfig.set(path + ".y", data.location.getY());
            newConfig.set(path + ".z", data.location.getZ());
            newConfig.set(path + ".type", data.type.toString());
        }

        try {
            newConfig.save(hologramsFile);
            DebugLogger.getInstance().log(Level.INFO, "Holograms saved to holograms.yml", 0);
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to save holograms.yml:", e);
        }
    }

    public void loadHologramsFromFile() {
        if (!hologramsFile.exists()) return;
        YamlConfiguration loadConfig = new YamlConfiguration();
        try {
            loadConfig.load(hologramsFile);
        } catch (IOException | InvalidConfigurationException e) {
            DebugLogger.getInstance().error("Failed to load holograms.yml:", e);
            return;
        }

        if (!loadConfig.isConfigurationSection("holograms")) {
            DebugLogger.getInstance().log(Level.INFO, "No holograms found in holograms.yml", 0);
            return;
        }

        for (String id : loadConfig.getConfigurationSection("holograms").getKeys(false)) {
            String worldName = loadConfig.getString("holograms." + id + ".world");
            double x = loadConfig.getDouble("holograms." + id + ".x");
            double y = loadConfig.getDouble("holograms." + id + ".y");
            double z = loadConfig.getDouble("holograms." + id + ".z");
            String typeStr = loadConfig.getString("holograms." + id + ".type", "LEVEL");
            LeaderboardType type = LeaderboardType.valueOf(typeStr);

            if (Bukkit.getWorld(worldName) == null) {
                DebugLogger.getInstance().error("World '" + worldName + "' not found for hologram '" + id + "'");
                continue;
            }

            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);

            try {
                // Spawn and save them into the `holograms` map
                spawnHologram(id, loc, type, false);
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to spawn hologram '" + id + "':", e);
            }
        }
        DebugLogger.getInstance().log(Level.INFO, "Holograms loaded from file.", 0);
    }

    public interface DiscordUpdateHandler {
        void updateDiscordEmbed(LeaderboardType type, List<PlayerData> topPlayers);
    }

    private static class HologramData {
        private final String id;
        private final Hologram hologram;
        private final BukkitTask updateTask;
        private final LeaderboardType type;
        private final Location location;

        public HologramData(String id, Hologram hologram, BukkitTask updateTask, LeaderboardType type, Location location) {
            this.id = id;
            this.hologram = hologram;
            this.updateTask = updateTask;
            this.type = type;
            this.location = location;
        }

        public void cancelUpdateTask() {
            if (updateTask != null && !updateTask.isCancelled()) {
                updateTask.cancel();
            }
        }
    }
}
