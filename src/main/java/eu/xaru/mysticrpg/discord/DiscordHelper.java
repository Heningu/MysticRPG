//
// DiscordHelper.java
//

package eu.xaru.mysticrpg.discord;

import eu.xaru.mysticrpg.player.leaderboards.LeaderboardType;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.utils.DebugLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles the Discord Bot logic: linking codes, slash commands, etc.
 * Refactored to rely on plugin.getConfig() for reading token/guildId
 * rather than using any dynamic config approach.
 */
public class DiscordHelper extends ListenerAdapter {

    private final DiscordModule discordModule;
    private JDA jda;

    // code -> UUID linking map
    private final Map<String, UUID> codeToUUIDMap = new ConcurrentHashMap<>();
    // discordId -> UUID linking map
    private final Map<Long, UUID> discordIdToUUIDMap = new ConcurrentHashMap<>();

    public DiscordHelper(DiscordModule discordModule) {
        this.discordModule = discordModule;
    }

    /**
     * Starts the bot using values from plugin's config.yml
     */
    public void startBot() {
        // Use the plugin's config
        String botToken = discordModule.getPlugin().getConfig().getString("discordBotToken", "");
        if (botToken == null || botToken.isEmpty()) {
            DebugLogger.getInstance().error("Discord bot token is not configured in config.yml");
            return;
        }

        try {
            jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .setActivity(net.dv8tion.jda.api.entities.Activity.playing("Linking Accounts"))
                    .build();
            jda.awaitReady();
            DebugLogger.getInstance().log(Level.INFO, "Discord bot connected and ready.", 0);

            String guildId = discordModule.getPlugin().getConfig().getString("discordGuildId", "");
            if (guildId == null || guildId.isEmpty()) {
                DebugLogger.getInstance().error("Discord guild ID is not configured in config.yml");
                return;
            }

            jda.getGuildById(guildId).updateCommands().addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("link", "Link your Discord account with Minecraft.")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code", "Your unique linking code", true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("me", "View your linked Minecraft account information.")
            ).queue(
                    success -> DebugLogger.getInstance().log(Level.INFO, "Slash commands registered successfully.", 0),
                    failure -> DebugLogger.getInstance().error("Failed to register slash commands: " + failure.getMessage())
            );
        } catch (InterruptedException e) {
            DebugLogger.getInstance().error("Discord bot initialization interrupted:", e);
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownBot() {
        if (jda != null) {
            jda.shutdownNow();
            DebugLogger.getInstance().log(Level.INFO, "Discord bot shut down.", 0);
        }
    }

    public JDA getJDA() {
        return jda;
    }

    /**
     * Generate a short code that the user can type in Discord to link accounts.
     */
    public String generateUniqueCode(UUID playerUUID) {
        String code;
        do {
            code = generateRandomCode(6);
        } while (codeToUUIDMap.containsKey(code));

        codeToUUIDMap.put(code, playerUUID);

        final String finalCode = code;
        Bukkit.getScheduler().runTaskLaterAsynchronously(discordModule.getPlugin(), () -> {
            codeToUUIDMap.remove(finalCode);
            DebugLogger.getInstance().log(Level.INFO, "Linking code expired and removed: " + finalCode, 0);
        }, 20 * 60 * 5); // expires in 5 min

        return code;
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            codeBuilder.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return codeBuilder.toString();
    }

    public UUID getPlayerUUIDByCode(String code) {
        return codeToUUIDMap.get(code);
    }

    public Map<Long, UUID> getDiscordIdToUUIDMap() {
        return discordIdToUUIDMap;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("link")) {
            handleLinkCommand(event);
        } else if (event.getName().equals("me")) {
            handleMeCommand(event);
        } else if (event.getName().equals("leaderboard")) {
            String typeStr = event.getOption("type").getAsString().toUpperCase();
            LeaderboardType type;
            try {
                type = LeaderboardType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                event.reply("Invalid leaderboard type. Use LEVEL or RICH.").queue();
                return;
            }

            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                event.reply("You do not have permission to use this command.").queue();
                return;
            }

            discordModule.createLeaderboardEmbed(type, event);
        }
    }

    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        String code = event.getOption("code").getAsString();
        long discordId = event.getUser().getIdLong();

        UUID playerUUID = getPlayerUUIDByCode(code);
        if (playerUUID != null) {
            // Forward to main DiscordModule logic
            discordModule.handleDiscordLinking(discordId, code);

            codeToUUIDMap.remove(code);
            discordIdToUUIDMap.put(discordId, playerUUID);

            event.reply("Your Discord account has been successfully linked!").queue();
            DebugLogger.getInstance().log(Level.INFO, "Discord ID " + discordId + " linked with player UUID " + playerUUID, 0);
        } else {
            event.reply("Invalid or expired linking code. Please try again.").queue();
            DebugLogger.getInstance().log(Level.WARNING, "Failed linking attempt with invalid code: " + code + " by Discord ID: " + discordId, 0);
        }
    }

    private void handleMeCommand(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        UUID playerUUID = discordIdToUUIDMap.get(discordId);

        if (playerUUID == null) {
            // attempt loading from DB
            discordModule.getPlayerDataCache().loadPlayerDataByDiscordId(discordId, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData playerData) {
                    UUID loadedUUID = UUID.fromString(playerData.getUuid());
                    discordIdToUUIDMap.put(discordId, loadedUUID);
                    sendEmbeddedPlayerInfo(event, loadedUUID, playerData);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    event.reply("You have not linked a Discord account with any Minecraft account. Use `/link <code>` to link your accounts.")
                            .queue();
                    DebugLogger.getInstance().log(Level.WARNING, "Discord ID " + discordId + " attempted to use /me without linking.", 0);
                }
            });
        } else {
            // data might already be in cache
            PlayerData playerData = discordModule.getPlayerDataCache().getCachedPlayerData(playerUUID);
            if (playerData != null) {
                sendEmbeddedPlayerInfo(event, playerUUID, playerData);
            } else {
                // load from DB
                discordModule.getPlayerDataCache().loadPlayerData(playerUUID, new Callback<PlayerData>() {
                    @Override
                    public void onSuccess(PlayerData loadedData) {
                        sendEmbeddedPlayerInfo(event, playerUUID, loadedData);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        event.reply("Your Minecraft account data is not available. Please ensure you are online or have played before.")
                                .queue();
                        DebugLogger.getInstance().log(Level.WARNING, "PlayerData not found for UUID: " + playerUUID
                                + " linked to Discord ID: " + discordId, 0);
                    }
                });
            }
        }
    }

    private String getUsernameFromUUID(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }

    private void sendEmbeddedPlayerInfo(SlashCommandInteractionEvent event, UUID playerUUID, PlayerData playerData) {
        String username = getUsernameFromUUID(playerUUID);
        int level = playerData.getLevel();
        int balance = playerData.getBankGold();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Minecraft Account Information");
        embed.setColor(Color.GREEN);
        embed.addField("Username", username, false);
        embed.addField("Level", String.valueOf(level), true);
        embed.addField("Balance", String.valueOf(balance), true);
        embed.setFooter("Requested by " + event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());

        event.replyEmbeds(embed.build()).queue();
    }
}

