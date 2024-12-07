package eu.xaru.mysticrpg.discord;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.utils.DebugLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * DiscordHelper manages the Discord bot's functionalities, including handling slash commands
 * and maintaining mappings between Discord IDs and Minecraft UUIDs.
 */
public class DiscordHelper extends ListenerAdapter {

    
    private final DiscordModule discordModule;
    private JDA jda;

    // Map to store code to UUID mappings
    private final Map<String, UUID> codeToUUIDMap = new ConcurrentHashMap<>();

    // Map to store Discord ID to UUID mappings for reverse lookup
    private final Map<Long, UUID> discordIdToUUIDMap = new ConcurrentHashMap<>();

    /**
     * Constructor for DiscordHelper.
     *
             The DebugLoggerModule for logging.
     * @param discordModule The DiscordModule instance for interacting with other modules.
     */
    public DiscordHelper( DiscordModule discordModule) {
 
        this.discordModule = discordModule;
    }

    /**
     * Starts the Discord bot.
     */
    public void startBot() {
        String botToken = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("discordBotToken");
        if (botToken == null || botToken.isEmpty()) {
            DebugLogger.getInstance().error("Discord bot token is not configured.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .setActivity(net.dv8tion.jda.api.entities.Activity.playing("Linking Accounts"))
                    .build();
            jda.awaitReady();
            DebugLogger.getInstance().log(Level.INFO, "Discord bot connected and ready.", 0);

            // Register slash commands to a specific guild for faster updates during development
            String guildId = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("discordGuildId");
            if (guildId == null || guildId.isEmpty()) {
                DebugLogger.getInstance().error("Discord guild ID is not configured.");
                return;
            }

            // Register /link and /me commands
            jda.getGuildById(guildId).updateCommands().addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("link", "Link your Discord account with Minecraft.")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code", "Your unique linking code", true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("me", "View your linked Minecraft account information.")
            ).queue(success -> DebugLogger.getInstance().log(Level.INFO, "Slash commands registered successfully.", 0),
                    failure -> DebugLogger.getInstance().error("Failed to register slash commands: " + failure.getMessage()));
        } catch (InterruptedException e) {
            DebugLogger.getInstance().error("Discord bot initialization interrupted:", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }

    /**
     * Shuts down the Discord bot gracefully.
     */
    public void shutdownBot() {
        if (jda != null) {
            jda.shutdown();
            DebugLogger.getInstance().log(Level.INFO, "Discord bot shut down.", 0);
        }
    }

    /**
     * Generates a unique 6-character code consisting of letters and numbers.
     *
     * @param playerUUID The UUID of the player requesting the code.
     * @return The generated unique code.
     */
    public String generateUniqueCode(UUID playerUUID) {
        String code;
        do {
            code = generateRandomCode(6);
        } while (codeToUUIDMap.containsKey(code));

        codeToUUIDMap.put(code, playerUUID);

        // Make 'code' effectively final by not modifying it after this point
        final String finalCode = code;

        // Schedule the task with the finalCode
        Bukkit.getScheduler().runTaskLaterAsynchronously(discordModule.getPlugin(), () -> {
            codeToUUIDMap.remove(finalCode);
            DebugLogger.getInstance().log(Level.INFO, "Linking code expired and removed: " + finalCode, 0);
        }, 20 * 60 * 5); // Expires after 5 minutes

        return code;
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length The length of the code.
     * @return The generated code.
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            codeBuilder.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return codeBuilder.toString();
    }

    /**
     * Retrieves the player UUID associated with a given code.
     *
     * @param code The linking code.
     * @return The player's UUID, or null if not found.
     */
    public UUID getPlayerUUIDByCode(String code) {
        return codeToUUIDMap.get(code);
    }

    /**
     * Retrieves the Discord ID to UUID mapping.
     *
     * @return The map of Discord IDs to UUIDs.
     */
    public Map<Long, UUID> getDiscordIdToUUIDMap() {
        return discordIdToUUIDMap;
    }

    /**
     * Handles the /link command from Discord.
     *
     * @param event The SlashCommandInteractionEvent.
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("link")) {
            String code = event.getOption("code").getAsString();
            long discordId = event.getUser().getIdLong();

            UUID playerUUID = getPlayerUUIDByCode(code);
            if (playerUUID != null) {
                // Link Discord ID to Player UUID
                discordModule.handleDiscordLinking(discordId, code);
                // Remove the code as it's used
                codeToUUIDMap.remove(code);
                // Update discordIdToUUIDMap
                discordIdToUUIDMap.put(discordId, playerUUID);
                event.reply("Your Discord account has been successfully linked!").setEphemeral(true).queue();
                DebugLogger.getInstance().log(Level.INFO, "Discord ID " + discordId + " linked with player UUID " + playerUUID, 0);
            } else {
                event.reply("Invalid or expired linking code. Please try again.").setEphemeral(true).queue();
                DebugLogger.getInstance().log(Level.WARNING, "Failed linking attempt with invalid code: " + code + " by Discord ID: " + discordId, 0);
            }
        } else if (event.getName().equals("me")) {
            long discordId = event.getUser().getIdLong();
            UUID playerUUID = discordIdToUUIDMap.get(discordId);

            if (playerUUID != null) {
                PlayerData playerData = discordModule.getPlayerDataCache().getCachedPlayerData(playerUUID);
                if (playerData != null) {
                    // Attempt to get the player's username from the server if online
                    Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                    String username;
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        username = onlinePlayer.getName();
                    } else {
                        // If offline, set username as "Unknown"
                        username = "Unknown";
                    }

                    String uuidString = playerUUID.toString();

                    // Since "when it was linked" is not stored, we cannot display it
                    // Alternatively, if you decide to add a 'linkedAt' field, implement it here

                    String response = "Your linked Minecraft account:\n" +
                            "Username: " + username + "\n" +
                            "UUID: " + uuidString;

                    event.reply(response).setEphemeral(true).queue();
                } else {
                    event.reply("Your Minecraft account data is not available. Please ensure you are online or have played before.").setEphemeral(true).queue();
                    DebugLogger.getInstance().log(Level.WARNING, "PlayerData not found for UUID: " + playerUUID + " linked to Discord ID: " + discordId, 0);
                }
            } else {
                event.reply("You have not linked a Discord account with any Minecraft account. Use `/discord link <code>` to link your accounts.").setEphemeral(true).queue();
                DebugLogger.getInstance().log(Level.WARNING, "Discord ID " + discordId + " attempted to use /me without linking.", 0);
            }
        }
    }
}
