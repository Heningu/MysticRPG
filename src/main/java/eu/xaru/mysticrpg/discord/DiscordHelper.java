package eu.xaru.mysticrpg.discord;

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

public class DiscordHelper extends ListenerAdapter {

    private final DiscordModule discordModule;
    private JDA jda;

    // Map to store code to UUID mappings
    private final Map<String, UUID> codeToUUIDMap = new ConcurrentHashMap<>();

    // Map to store Discord ID to UUID mappings for reverse lookup
    private final Map<Long, UUID> discordIdToUUIDMap = new ConcurrentHashMap<>();

    public DiscordHelper(DiscordModule discordModule) {
        this.discordModule = discordModule;
    }

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

            String guildId = Bukkit.getPluginManager().getPlugin("MysticRPG").getConfig().getString("discordGuildId");
            if (guildId == null || guildId.isEmpty()) {
                DebugLogger.getInstance().error("Discord guild ID is not configured.");
                return;
            }

            jda.getGuildById(guildId).updateCommands().addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("link", "Link your Discord account with Minecraft.")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code", "Your unique linking code", true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("me", "View your linked Minecraft account information.")
            ).queue(success -> DebugLogger.getInstance().log(Level.INFO, "Slash commands registered successfully.", 0),
                    failure -> DebugLogger.getInstance().error("Failed to register slash commands: " + failure.getMessage()));
        } catch (InterruptedException e) {
            DebugLogger.getInstance().error("Discord bot initialization interrupted:", e);
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownBot() {
        if (jda != null) {
            // Use shutdownNow() to ensure no lingering tasks remain
            jda.shutdownNow();
            DebugLogger.getInstance().log(Level.INFO, "Discord bot shut down.", 0);
        }
    }

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
        }, 20 * 60 * 5); // Expires after 5 minutes

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
        }
    }

    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        String code = event.getOption("code").getAsString();
        long discordId = event.getUser().getIdLong();

        UUID playerUUID = getPlayerUUIDByCode(code);
        if (playerUUID != null) {
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
            // Not in memory, load from DB by discordId
            discordModule.getPlayerDataCache().loadPlayerDataByDiscordId(discordId, new Callback<PlayerData>() {
                @Override
                public void onSuccess(PlayerData playerData) {
                    UUID loadedUUID = UUID.fromString(playerData.getUuid());
                    discordIdToUUIDMap.put(discordId, loadedUUID);
                    sendEmbeddedPlayerInfo(event, loadedUUID, playerData);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    event.reply("You have not linked a Discord account with any Minecraft account. Use `/discord link <code>` to link your accounts.")
                            .queue();
                    DebugLogger.getInstance().log(Level.WARNING, "Discord ID " + discordId + " attempted to use /me without linking.", 0);
                }
            });
        } else {
            // Already in memory, try cache or load by UUID
            PlayerData playerData = discordModule.getPlayerDataCache().getCachedPlayerData(playerUUID);
            if (playerData != null) {
                sendEmbeddedPlayerInfo(event, playerUUID, playerData);
            } else {
                discordModule.getPlayerDataCache().loadPlayerData(playerUUID, new Callback<PlayerData>() {
                    @Override
                    public void onSuccess(PlayerData loadedData) {
                        sendEmbeddedPlayerInfo(event, playerUUID, loadedData);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        event.reply("Your Minecraft account data is not available. Please ensure you are online or have played before.")
                                .queue();
                        DebugLogger.getInstance().log(Level.WARNING, "PlayerData not found for UUID: " + playerUUID + " linked to Discord ID: " + discordId, 0);
                    }
                });
            }
        }
    }

    /**
     * Attempt to get the player's username from the server's offline player data.
     */
    private String getUsernameFromUUID(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }

    /**
     * Sends an embedded message with the player's information: username, level, balance.
     */
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
