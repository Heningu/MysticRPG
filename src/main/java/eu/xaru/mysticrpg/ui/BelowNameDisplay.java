package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.titles.PlayerTitleManager;
import eu.xaru.mysticrpg.player.titles.PlayerTitleModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * BelowNameDisplay uses a single, shared scoreboard for all players.
 * This ensures that all players can see each other's health below their names.
 *
 * Conditions:
 * - HP > 0
 * - Player has a title
 *
 * If a player has no valid conditions, they just won't show proper health or will show default values,
 * but since 'health' criteria always shows HP as a number, they'll still appear with their numeric HP if their HP>0.
 */
public class BelowNameDisplay {

    private final JavaPlugin plugin;
    private final PlayerDataCache playerDataCache;
    private final PlayerTitleModule titleModule;

    // A single shared scoreboard for all players
    private Scoreboard sharedScoreboard;
    private Objective healthObj;

    // Keep track of players currently known to the scoreboard
    private final Set<Player> players = new HashSet<>();

    public BelowNameDisplay() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);

        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            this.playerDataCache = PlayerDataCache.getInstance();
        } else {
            this.playerDataCache = null;
            DebugLogger.getInstance().warning("[BelowNameDisplay] SaveModule not found. BelowNameDisplay may not function properly.");
        }

        this.titleModule = ModuleManager.getInstance().getModuleInstance(PlayerTitleModule.class);

        DebugLogger.getInstance().log(Level.INFO, "BelowNameDisplay (Shared Scoreboard) initialized successfully.", 0);

        initializeSharedScoreboard();
    }

    /**
     * Initialize the shared scoreboard and the healthBelowName objective.
     * We'll create the objective once, and as players join, we ensure they are added.
     */
    private void initializeSharedScoreboard() {
        sharedScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Create the healthBelowName objective that uses health criteria
        // The display name here is the title of the objective, not what appears under the name.
        // Under the name, players only see numeric health due to how Minecraft works.
        healthObj = sharedScoreboard.getObjective("healthBelowName");
        if (healthObj != null) {
            healthObj.unregister();
        }

        // Note: The actual text "HP [Title]" won't appear below the name, only numeric health.
        // The displayName sets the name of the objective, not the text below the player.
        // Players will see a numeric health value below each other's names.
        healthObj = sharedScoreboard.registerNewObjective("healthBelowName", "health", ChatColor.RED + "HP");
        healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    /**
     * Adds a player to the shared scoreboard and updates their health display.
     *
     * @param player The player joining
     */
    public void addPlayer(Player player) {
        players.add(player);
        player.setScoreboard(sharedScoreboard);
        DebugLogger.getInstance().debug("[BelowNameDisplay] Added player " + player.getName() + " to the shared below-name scoreboard.");
        updatePlayerHealthDisplay(player);
    }

    /**
     * Remove player from the scoreboard tracking on quit.
     * We could remove them from the scoreboard, but since it's shared, we just keep them out of set.
     *
     * @param player The departing player
     */
    public void removePlayer(Player player) {
        players.remove(player);
        DebugLogger.getInstance().debug("[BelowNameDisplay] Player " + player.getName() + " quit, no special cleanup required for shared scoreboard.");
        // Shared scoreboard remains. They just won't be online to see it.
    }

    /**
     * Update health display for all players.
     * Called when conditions may have changed (e.g., player got a title, changed HP).
     */
    public void updateAllPlayersHealthDisplay() {
        DebugLogger.getInstance().debug("[BelowNameDisplay] Updating health display for all players...");
        for (Player p : players) {
            updatePlayerHealthDisplay(p);
        }
    }

    /**
     * Updates the health display for a single player in the shared scoreboard.
     *
     * For each player on the server, the health objective will show their numeric health value below their name to others.
     * Titles will not appear below the name as custom text; Minecraft only displays numeric health. The objective's display name
     * doesn't influence the text below the name, only the numeric value is displayed.
     *
     * @param player The player to update
     */
    public void updatePlayerHealthDisplay(Player player) {
        if (playerDataCache == null) {
            DebugLogger.getInstance().debug("[BelowNameDisplay] playerDataCache is null. Cannot update " + player.getName());
            return;
        }

        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().debug("[BelowNameDisplay] No PlayerData for " + player.getName() + ". Cannot ensure HP conditions.");
            // Without PlayerData, we can't set custom HP. But since criteria=health, Minecraft handles numeric HP automatically.
            // Just ensure player scoreboard is assigned.
            player.setScoreboard(sharedScoreboard);
            return;
        }

        int currentHp = data.getCurrentHp();
        int maxHp = data.getAttributes().getOrDefault("HEALTH", 20);

        // Check if player has a title and HP > 0
        boolean hasTitle = false;
        if (titleModule != null) {
            PlayerTitleManager titleManager = titleModule.getTitleManager();
            if (titleManager.hasEquippedTitle(player)) {
                hasTitle = true;
            }
        }

        // If HP ≤ 0 or no title, they'll still appear, but HP won't vanish, it'll show their HP anyway
        // because the health criteria always shows numeric health if player is alive.
        // If you want to hide them completely if no title or HP ≤0, you'd need another approach.

        // Set player health to bounded HP so the displayed health matches their custom HP
        player.setMaxHealth(maxHp);
        double boundedHp = Math.max(0, Math.min(currentHp, maxHp));
        player.setHealth(boundedHp);
        DebugLogger.getInstance().debug("[BelowNameDisplay] Set " + player.getName() + " health to " + boundedHp + "/" + maxHp);

        // The shared scoreboard with health objective is already set to all players,
        // so everyone sees everyone's HP under their names.
        player.setScoreboard(sharedScoreboard);
    }

    /**
     * Refresh all below-name displays.
     */
    public void refreshAll() {
        DebugLogger.getInstance().debug("[BelowNameDisplay] Refreshing all below-name displays...");
        updateAllPlayersHealthDisplay();
        DebugLogger.getInstance().debug("[BelowNameDisplay] Done refreshing all below-name displays.");
    }
}
