//
// DiscordModule.java
//

package eu.xaru.mysticrpg.discord;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leaderboards.LeaderboardType;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.storage.database.DatabaseManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Main Discord module that orchestrates linking logic, leaderboard updates, etc.
 * Refactored to remove DynamicConfig usage, using plugin's config directly.
 */
public class DiscordModule implements IBaseModule, Listener {

    private final JavaPlugin plugin;

    private DiscordHelper discordHelper;
    private PlayerDataCache playerDataCache;
    private DatabaseManager databaseManager;

    private final Map<LeaderboardType, LeaderboardMessageData> leaderboardEmbeds = new HashMap<>();

    public DiscordModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public PlayerDataCache getPlayerDataCache() {
        return this.playerDataCache;
    }

    @Override
    public void initialize() {
        // Rely on SaveModule for DB, caching, etc.
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule != null) {
            playerDataCache = PlayerDataCache.getInstance();
            databaseManager = DatabaseManager.getInstance();
        }

        if (playerDataCache == null || databaseManager == null) {
            DebugLogger.getInstance().error("SaveModule / PlayerDataCache / DatabaseManager not initialized. DiscordModule cannot function.");
            return;
        }

        discordHelper = new DiscordHelper(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        // Start the Discord Bot
        discordHelper.startBot();

        // Optionally register /leaderboard command with the guild if desired
        String guildId = plugin.getConfig().getString("discordGuildId", "");
        if (guildId != null && !guildId.isEmpty() && discordHelper.getJDA() != null) {
            discordHelper.getJDA().getGuildById(guildId)
                    .updateCommands()
                    .addCommands(
                            net.dv8tion.jda.api.interactions.commands.build.Commands.slash("leaderboard", "Show a specific leaderboard")
                                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                                            "type", "Type of leaderboard (LEVEL/RICH)", true)
                    ).queue();
        }
    }

    @Override
    public void stop() {
        discordHelper.shutdownBot();
    }

    @Override
    public void unload() {
        // no special logic
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Once we have a code + discordId, link them in the DB so the player
     * data includes the discordId. (This is invoked by DiscordHelper).
     */
    public void handleDiscordLinking(long discordId, String code) {
        UUID playerUUID = discordHelper.getPlayerUUIDByCode(code);
        if (playerUUID != null) {
            boolean alreadyLinked = playerDataCache.getAllCachedPlayerUUIDs().stream().anyMatch(uuid -> {
                PlayerData data = playerDataCache.getCachedPlayerData(uuid);
                return data != null && data.getDiscordId() != null && data.getDiscordId().equals(discordId);
            });

            if (alreadyLinked) {
                DebugLogger.getInstance().error("Discord ID " + discordId + " is already linked to another Minecraft account.");
                return;
            }

            PlayerData playerData = playerDataCache.getCachedPlayerData(playerUUID);
            if (playerData != null) {
                playerData.setDiscordId(discordId);
                playerDataCache.savePlayerData(playerUUID, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            player.sendMessage("Your Discord account has been successfully linked!");
                        }
                        discordHelper.getDiscordIdToUUIDMap().put(discordId, playerUUID);
                        DebugLogger.getInstance().log(Level.INFO, "Linked Discord ID " + discordId + " with player UUID " + playerUUID, 0);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        DebugLogger.getInstance().error("Failed saving Discord linking for player UUID " + playerUUID, throwable);
                    }
                });
            } else {
                DebugLogger.getInstance().error("No player found for linking code: " + code);
            }
        } else {
            DebugLogger.getInstance().error("Invalid or expired linking code: " + code);
        }
    }

    /**
     * Called by a background job or something to update the embed if it exists.
     */
    public void updateLeaderboardEmbed(LeaderboardType type, List<PlayerData> topPlayers) {
        LeaderboardMessageData msgData = leaderboardEmbeds.get(type);
        if (msgData == null) return; // no existing embed

        EmbedBuilder embed = buildLeaderboardEmbed(type, topPlayers);

        if (discordHelper.getJDA() == null) {
            DebugLogger.getInstance().error("JDA is null, cannot update leaderboard embed.");
            return;
        }

        discordHelper.getJDA().getTextChannelById(msgData.channelId)
                .retrieveMessageById(msgData.messageId)
                .queue(message -> message.editMessageEmbeds(embed.build()).queue(),
                        failure -> DebugLogger.getInstance().error("Failed updating leaderboard embed message: " + failure.getMessage()));
    }

    /**
     * Create a brand new leaderboard embed for the given slash command invocation.
     */
    public void createLeaderboardEmbed(LeaderboardType type, SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(); // ephemeral "thinking"

        databaseManager.getPlayerRepository().loadAll(new Callback<List<PlayerData>>() {
            @Override
            public void onSuccess(List<PlayerData> allPlayers) {
                List<PlayerData> sortedPlayers = sortPlayers(allPlayers, type);

                EmbedBuilder embed = buildLeaderboardEmbed(type, sortedPlayers);

                // Send to the channel
                event.getChannel().sendMessageEmbeds(embed.build()).queue(message -> {
                    long channelId = message.getChannel().getIdLong();
                    long messageId = message.getIdLong();

                    // track it for updates
                    leaderboardEmbeds.put(type, new LeaderboardMessageData(channelId, messageId));
                    DebugLogger.getInstance().log(Level.INFO, "Created leaderboard embed for " + type + " at messageId=" + messageId, 0);

                    // remove ephemeral waiting message
                    event.getHook().deleteOriginal().queue();
                }, failure -> {
                    event.getHook().editOriginal("Failed to create leaderboard message in channel.").queue();
                    DebugLogger.getInstance().error("Failed sending leaderboard embed message: " + failure.getMessage());
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                event.getHook().editOriginal("Failed to fetch leaderboard data.").queue();
                DebugLogger.getInstance().error("Failed to fetch leaderboard data for Discord embed:", throwable);
            }
        });
    }

    private List<PlayerData> sortPlayers(List<PlayerData> allPlayers, LeaderboardType type) {
        if (type == LeaderboardType.LEVEL) {
            return allPlayers.stream()
                    .sorted(Comparator.comparingInt(PlayerData::getLevel).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        } else {
            // RICH => sort by bankGold
            return allPlayers.stream()
                    .sorted((p1, p2) -> Integer.compare(p2.getBankGold(), p1.getBankGold()))
                    .limit(5)
                    .collect(Collectors.toList());
        }
    }

    private EmbedBuilder buildLeaderboardEmbed(LeaderboardType type, List<PlayerData> topPlayers) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(type == LeaderboardType.LEVEL ? "Top Level Players" : "Top Rich Players");
        embed.setColor(Color.GREEN);

        int rank = 1;
        for (PlayerData pd : topPlayers) {
            UUID playerUUID = UUID.fromString(pd.getUuid());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
            String playerName = (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null)
                    ? offlinePlayer.getName() : "Unknown";

            if (type == LeaderboardType.LEVEL) {
                embed.addField("#" + rank, playerName + " - Level " + pd.getLevel(), false);
            } else {
                embed.addField("#" + rank, playerName + " - Gold " + pd.getBankGold(), false);
            }
            rank++;
        }

        // If fewer than 5 players, fill in blank spots
        for (; rank <= 5; rank++) {
            embed.addField("#" + rank, "N/A", false);
        }

        return embed;
    }

    private static class LeaderboardMessageData {
        long channelId;
        long messageId;
        LeaderboardMessageData(long channelId, long messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
    }
}
