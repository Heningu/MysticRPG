package eu.xaru.mysticrpg.discord;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * DiscordModule handles Discord-related functionalities, including linking and unlinking Discord accounts,
 * and providing administrative controls over these actions.
 */
public class DiscordModule implements IBaseModule, Listener {

    private final JavaPlugin plugin;
    private DebugLoggerModule logger;
    private DiscordHelper discordHelper;
    private PlayerDataCache playerDataCache;

    public DiscordModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    // Ensure this method is public
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Retrieves the PlayerDataCache instance.
     *
     * @return The PlayerDataCache.
     */
    public PlayerDataCache getPlayerDataCache() {
        return this.playerDataCache;
    }

    @Override
    public void initialize() {
        // Initialize logger
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            Bukkit.getLogger().severe("DebugLoggerModule not initialized. DiscordModule cannot function without it.");
            return;
        }

        // Initialize DiscordHelper
        discordHelper = new DiscordHelper(logger, this);

        // Initialize PlayerDataCache
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            playerDataCache = saveModule.getPlayerDataCache();
        }

        if (playerDataCache == null) {
            logger.error("SaveModule or PlayerDataCache not initialized. DiscordModule cannot function without it.");
            return;
        }

        // Register commands
        registerCommands();

        // Register event listeners if needed
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        logger.log(Level.INFO, "DiscordModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        // Start Discord bot or any other startup logic
        discordHelper.startBot();
        logger.log(Level.INFO, "DiscordModule started.", 0);
    }

    @Override
    public void stop() {
        // Shutdown Discord bot or any other cleanup logic
        discordHelper.shutdownBot();
        logger.log(Level.INFO, "DiscordModule stopped.", 0);
    }

    @Override
    public void unload() {
        // Additional unload logic if necessary
        logger.log(Level.INFO, "DiscordModule unloaded.", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class, DebugLoggerModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers all Discord-related commands, including link, unlink, and check.
     * The unlink command is enhanced to allow admins to unlink other players.
     */
    private void registerCommands() {
        // Main command: /discord
        new CommandAPICommand("discord")
                // Subcommand: /discord link
                .withSubcommand(new CommandAPICommand("link")
                        .executesPlayer((player, args) -> {
                            String code = discordHelper.generateUniqueCode(player.getUniqueId());
                            if (code != null) {
                                player.sendMessage(Utils.getInstance().$("Use this code to link your Discord account: " + code));
                                // Optionally, send the code via other means (e.g., GUI)
                                logger.log(Level.INFO, "Generated linking code for player " + player.getName() + ": " + code, 0);
                            } else {
                                player.sendMessage(Utils.getInstance().$("Failed to generate a linking code. Please try again later."));
                                logger.error("Failed to generate linking code for player " + player.getName());
                            }
                        }))
                // Subcommand: /discord unlink (unlink self)
                .withSubcommand(new CommandAPICommand("unlink")
                        .executesPlayer((player, args) -> {
                            UUID playerUUID = player.getUniqueId();
                            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
                            if (playerData != null && playerData.getDiscordId() != null) {
                                playerData.setDiscordId(null);
                                playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        player.sendMessage(Utils.getInstance().$("Your Discord account has been unlinked."));
                                        logger.log(Level.INFO, "Unlinked Discord ID for player " + player.getName(), 0);
                                    }

                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        player.sendMessage(Utils.getInstance().$("Failed to unlink your Discord account. Please try again later."));
                                        logger.error("Failed to unlink Discord ID for player " + player.getName() + ": " + throwable.getMessage());
                                    }
                                });
                            } else {
                                player.sendMessage(Utils.getInstance().$("You do not have a Discord account linked."));
                            }
                        }))
                // Subcommand: /discord unlink <username> (unlink others)
                .withSubcommand(new CommandAPICommand("unlink")
                        .withArguments(new dev.jorel.commandapi.arguments.StringArgument("username"))
                        .executesPlayer((player, args) -> {
                            String targetUsername = (String) args.get("username");
                            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("Please provide a valid username."));
                                return;
                            }
                            final String finalTargetUsername = targetUsername.trim();

                            // Player is attempting to unlink another player
                            if (player.hasPermission("mysticrpg.discord.unlink.others")) {
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(finalTargetUsername);
                                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                                    player.sendMessage(Utils.getInstance().$("User \"" + finalTargetUsername + "\" does not exist."));
                                    return;
                                }
                                UUID targetUUID = offlinePlayer.getUniqueId();
                                PlayerData targetPlayerData = playerDataCache.getCachedPlayerData(targetUUID);
                                if (targetPlayerData != null && targetPlayerData.getDiscordId() != null) {
                                    targetPlayerData.setDiscordId(null);
                                    playerDataCache.savePlayerData(targetUUID, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            player.sendMessage(Utils.getInstance().$("Discord account for user \"" + finalTargetUsername + "\" has been unlinked."));
                                            logger.log(Level.INFO, "Unlinked Discord ID for player " + finalTargetUsername + " by admin " + player.getName(), 0);
                                            // Notify the target player if online
                                            Player targetPlayer = Bukkit.getPlayer(targetUUID);
                                            if (targetPlayer != null && targetPlayer.isOnline()) {
                                                targetPlayer.sendMessage(Utils.getInstance().$("Your Discord account has been unlinked by an administrator."));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            player.sendMessage(Utils.getInstance().$("Failed to unlink Discord account for user \"" + finalTargetUsername + "\". Please try again later."));
                                            logger.error("Failed to unlink Discord ID for player " + finalTargetUsername + ": " + throwable.getMessage());
                                        }
                                    });
                                } else {
                                    player.sendMessage(Utils.getInstance().$("User \"" + finalTargetUsername + "\" does not have a Discord account linked."));
                                }
                            } else {
                                player.sendMessage(Utils.getInstance().$("You do not have permission to unlink other players."));
                                logger.log(Level.WARNING, "Player " + player.getName() + " attempted to unlink other players without permission.", 0);
                            }
                        }))
                // Subcommand: /discord check "USERNAME"
                .withSubcommand(new CommandAPICommand("check")
                        .withArguments(new dev.jorel.commandapi.arguments.StringArgument("username"))
                        .executesPlayer((player, args) -> {
                            String username = (String) args.get("username");
                            if (username == null || username.trim().isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("Please provide a valid username."));
                                return;
                            }
                            username = username.trim();

                            // Attempt to get the OfflinePlayer
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);

                            // Check if the player has played before or is online
                            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                                player.sendMessage(Utils.getInstance().$("User \"" + username + "\" does not exist."));
                                return;
                            }

                            UUID uuid = offlinePlayer.getUniqueId();

                            PlayerData playerData = playerDataCache.getCachedPlayerData(uuid);
                            if (playerData != null && playerData.getDiscordId() != null) {
                                player.sendMessage(Utils.getInstance().$("USERNAME: \"" + username + "\""));
                                player.sendMessage(Utils.getInstance().$("UUID: \"" + uuid.toString() + "\""));
                                player.sendMessage(Utils.getInstance().$("DISCORD_ID: \"" + playerData.getDiscordId() + "\""));
                            } else {
                                player.sendMessage(Utils.getInstance().$("User \"" + username + "\" is not linked with Discord."));
                            }
                        }))
                .register();
    }

    /**
     * Method to handle linking from Discord bot.
     * This should be called by DiscordHelper when a successful link is performed.
     *
     * @param discordId The Discord user ID.
     * @param code      The unique linking code.
     */
    public void handleDiscordLinking(long discordId, String code) {
        UUID playerUUID = discordHelper.getPlayerUUIDByCode(code);
        if (playerUUID != null) {
            // Check if the Discord ID is already linked to another player
            boolean alreadyLinked = playerDataCache.getAllCachedPlayerUUIDs().stream()
                    .anyMatch(uuid -> {
                        PlayerData data = playerDataCache.getCachedPlayerData(uuid);
                        return data != null && data.getDiscordId() != null && data.getDiscordId().equals(discordId);
                    });

            if (alreadyLinked) {
                logger.error("Discord ID " + discordId + " is already linked to another Minecraft account.");
                // Optionally, notify the Discord user via the bot
                return;
            }

            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
            if (playerData != null) {
                playerData.setDiscordId(discordId);
                playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Utils.getInstance().$("Your Discord account has been successfully linked!"));
                        }
                        // Update discordIdToUUIDMap
                        discordHelper.getDiscordIdToUUIDMap().put(discordId, playerUUID);
                        logger.log(Level.INFO, "Linked Discord ID " + discordId + " with player UUID " + playerUUID, 0);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("Failed to save Discord linking for player UUID " + playerUUID + ": " + throwable.getMessage());
                    }
                });
            } else {
                logger.error("No player found for linking code: " + code);
            }
        } else {
            logger.error("Invalid or expired linking code: " + code);
        }
    }
}
