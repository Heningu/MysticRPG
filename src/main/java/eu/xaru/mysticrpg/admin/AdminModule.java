package eu.xaru.mysticrpg.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.admin.AdminGUI;
import eu.xaru.mysticrpg.guis.admin.MobGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles administrative commands for managing game modes and inventories.
 */
public class AdminModule implements IBaseModule, Listener {

    private Set<UUID> adminPlayers = new HashSet<>();
    private File inventoryFolder;
    private File adminStateFile;
    private YamlConfiguration adminStateConfig;

    @Override
    public void initialize() {
        // Setup inventory folder
        inventoryFolder = new File(Bukkit.getPluginManager().getPlugin("MysticRPG").getDataFolder(), "admin_inventories");
        if (!inventoryFolder.exists()) {
            inventoryFolder.mkdirs();
        }

        // Setup admin state file
        adminStateFile = new File(Bukkit.getPluginManager().getPlugin("MysticRPG").getDataFolder(), "admin_states.yml");
        adminStateConfig = YamlConfiguration.loadConfiguration(adminStateFile);

        loadAdminStates(); // Load persisted admin states

        registerCommands();
        Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("MysticRPG"));

        DebugLogger.getInstance().log(Level.INFO, "AdminModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "AdminModule started", 0);
    }

    @Override
    public void stop() {
        saveAdminStates();
        DebugLogger.getInstance().log(Level.INFO, "AdminModule stopped", 0);
    }

    @Override
    public void unload() {
        saveAdminStates();
        DebugLogger.getInstance().log(Level.INFO, "AdminModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers administrative commands.
     */
    private void registerCommands() {
        // Admin toggle command
        new CommandAPICommand("admin")
                .withPermission("mysticrpg.admincmd.admin")
                .executesPlayer((player, args) -> {
                    if (adminPlayers.contains(player.getUniqueId())) {
                        // Player is currently admin, toggle off
                        exitAdminMode(player);
                    } else {
                        // Player is not admin, toggle on
                        enterAdminMode(player);
                    }
                })
                .withSubcommand(new CommandAPICommand("gui")
                        .executesPlayer((player, args) -> {
                            AdminGUI adminGUI = new AdminGUI();
                            adminGUI.openAdminGUI(player);
                        }))
                .register();

        // Game mode command with restrictions if in admin mode
        new CommandAPICommand("gm")
                .withPermission("mysticrpg.admincmd.gm")
                .withArguments(new IntegerArgument("mode", 0, 2))
                .executesPlayer((player, args) -> {
                    int mode = (int) args.get(0);

                    // If player is admin, they cannot go into survival (0)
                    if (adminPlayers.contains(player.getUniqueId()) && mode == 0) {
                        player.sendMessage("§cYou cannot go into Survival mode while in admin mode.");
                        return;
                    }

                    switch (mode) {
                        case 0 -> player.setGameMode(GameMode.SURVIVAL);
                        case 1 -> player.setGameMode(GameMode.CREATIVE);
                        case 2 -> player.setGameMode(GameMode.SPECTATOR);
                        default -> player.sendMessage("§cInvalid game mode. Use 0 (Survival), 1 (Creative), or 2 (Spectator).");
                    }
                    player.sendMessage("§aGame mode set to " + player.getGameMode().name());
                })
                .register();

        // Fly speed command (unchanged)
        new CommandAPICommand("flyspeed")
                .withPermission("mysticrpg.admincmd.flyspeed")
                .withArguments(new IntegerArgument("speed", 1, 10))
                .executesPlayer((player, args) -> {
                    int speed = (int) args.get(0);
                    float flySpeed = Math.min(speed / 10.0f, 10.0f); // Cap speed at maximum 10.0
                    player.setFlySpeed(flySpeed);
                    if (speed == 1) {
                        player.sendMessage("Fly speed set to default");
                    } else {
                        player.sendMessage("§aFly speed set to " + speed);
                    }
                })
                .register();
    }

    public void enterAdminMode(Player player) {
        UUID uuid = player.getUniqueId();
        // Save current survival inventory
        File survivalInvFile = new File(inventoryFolder, uuid.toString() + "-survival.yml");
        InventoryUtils.saveInventoryToFile(player, survivalInvFile);

        // Clear player's inventory and set to creative
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);

        // Load admin inventory if exists, else give them a fresh one
        File adminInvFile = new File(inventoryFolder, uuid.toString() + "-admin.yml");
        if (adminInvFile.exists()) {
            InventoryUtils.loadInventoryFromFile(player, adminInvFile);
        } else {
            // Optionally: give them an empty inventory or some default admin tools
            player.getInventory().clear();
        }

        adminPlayers.add(uuid);
        saveAdminStates();
        player.sendMessage("§aYou are now in admin mode. Your survival inventory has been saved.");
    }

    public void exitAdminMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Save current admin inventory
        File adminInvFile = new File(inventoryFolder, uuid.toString() + "-admin.yml");
        InventoryUtils.saveInventoryToFile(player, adminInvFile);

        // Restore survival inventory
        File survivalInvFile = new File(inventoryFolder, uuid.toString() + "-survival.yml");
        if (survivalInvFile.exists()) {
            InventoryUtils.loadInventoryFromFile(player, survivalInvFile);
        } else {
            // If no survival inv found, just clear
            player.getInventory().clear();
        }

        // Switch to survival mode
        player.setGameMode(GameMode.SURVIVAL);

        adminPlayers.remove(uuid);
        saveAdminStates();
        player.sendMessage("§cYou have exited admin mode and your survival inventory has been restored.");
    }

    public boolean isInAdminMode(Player player) {
        return adminPlayers.contains(player.getUniqueId());
    }



    /**
     * Apply admin mode state to a player without toggling.
     * This is used when a player rejoins and is already considered admin.
     */
    private void applyAdminModeState(Player player) {
        UUID uuid = player.getUniqueId();

        // Just ensure they are in creative and have their admin inventory
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);

        // Load existing admin inventory
        File adminInvFile = new File(inventoryFolder, uuid.toString() + "-admin.yml");
        if (adminInvFile.exists()) {
            InventoryUtils.loadInventoryFromFile(player, adminInvFile);
        } else {
            player.getInventory().clear();
        }
        player.sendMessage("§aYou are still in admin mode from before. Use /admin to exit.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (adminPlayers.contains(player.getUniqueId())) {
            // Player was in admin mode before disconnect/server restart
            // Reapply the admin mode without toggling inventories again
            applyAdminModeState(player);
        }
    }

    private void loadAdminStates() {
        List<String> admins = adminStateConfig.getStringList("admin_players");
        for (String uuidStr : admins) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                adminPlayers.add(uuid);
            } catch (IllegalArgumentException e) {
                // Invalid UUID in config, ignore
            }
        }
    }

    private void saveAdminStates() {
        List<String> admins = new ArrayList<>();
        for (UUID uuid : adminPlayers) {
            admins.add(uuid.toString());
        }
        adminStateConfig.set("admin_players", admins);
        try {
            adminStateConfig.save(adminStateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
