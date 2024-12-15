package eu.xaru.mysticrpg.social.friends;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.CustomInventoryManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * The main module class for managing the Friends system.
 * Handles initialization, command registration, and event handling.
 */
public class FriendsModule implements IBaseModule {

    private MysticCore plugin;
    private EventManager eventManager;
    
    private PlayerDataCache playerDataCache;
    private FriendsHelper friendsHelper;
    private PartyHelper partyHelper;

    // Map to track players' current pages in the Friends GUI
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // Map to track players' current pages in the Friend Requests GUI
    private final Map<UUID, Integer> friendRequestsPages = new HashMap<>();

    // Map to track players' selected friends for the Friend Options GUI
    private final Map<UUID, UUID> openFriendOptions = new HashMap<>();

    /**
     * Initializes the FriendsModule by setting up necessary components.
     */
    @Override
    public void initialize() {
        // Retrieve the main plugin instance
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        // Initialize the event manager
        this.eventManager = new EventManager(plugin);

        // Initialize the player data cache
        this.playerDataCache = PlayerDataCache.getInstance();
        // Initialize the friends helper
        this.friendsHelper = new FriendsHelper(playerDataCache);

        // Retrieve the PartyHelper from the PartyModule dependency
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule != null) {
            this.partyHelper = partyModule.getPartyHelper();
        } else {
            DebugLogger.getInstance().error("PartyModule not initialized. Party features in FriendsModule will not function.");
        }
    }

    /**
     * Starts the FriendsModule by registering commands and event handlers.
     */
    @Override
    public void start() {
        registerCommands();
    }

    /**
     * Stops the FriendsModule. Placeholder for any necessary cleanup.
     */
    @Override
    public void stop() {
        // Any necessary cleanup can be performed here
    }

    /**
     * Unloads the FriendsModule. Placeholder for any necessary unload actions.
     */
    @Override
    public void unload() {
        // Any necessary unload actions can be performed here
    }

    /**
     * Specifies the dependencies required by the FriendsModule.
     *
     * @return A list of module classes that FriendsModule depends on.
     */
    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( PartyModule.class);
    }

    /**
     * Specifies the priority level of the FriendsModule.
     *
     * @return The module priority.
     */
    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    /**
     * Registers the Friends commands using the CommandAPI.
     * Commands include adding, removing, accepting, and denying friends.
     */
    private void registerCommands() {
        new CommandAPICommand("friends")
                .withAliases("f")
                // /friends add <player>
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            try {
                                Player target = (Player) args.get("player");
                                friendsHelper.sendFriendRequest(player, target);
                            } catch (Exception e) {
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
                                DebugLogger.getInstance().error("Error executing /friends add command:", e);
                                e.printStackTrace();
                            }
                        }))
                // /friends remove <player>
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            try {
                                Player target = (Player) args.get("player");
                                friendsHelper.removeFriend(player, target);
                            } catch (Exception e) {
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
                                DebugLogger.getInstance().error("Error executing /friends remove command:", e);
                                e.printStackTrace();
                            }
                        }))
                // /friends accept <playerName>
                .withSubcommand(new CommandAPICommand("accept")
                        .withArguments(new StringArgument("playerName"))
                        .executesPlayer((player, args) -> {
                            try {
                                String senderName = (String) args.get("playerName");
                                friendsHelper.acceptFriendRequest(player, senderName);
                            } catch (Exception e) {
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
                                DebugLogger.getInstance().error("Error executing /friends accept command:", e);
                                e.printStackTrace();
                            }
                        }))
                // /friends deny <playerName>
                .withSubcommand(new CommandAPICommand("deny")
                        .withArguments(new StringArgument("playerName"))
                        .executesPlayer((player, args) -> {
                            try {
                                String senderName = (String) args.get("playerName");
                                friendsHelper.denyFriendRequest(player, senderName);
                            } catch (Exception e) {
                                player.sendMessage(Utils.getInstance().$("An error occurred while processing your request."));
                                DebugLogger.getInstance().error("Error executing /friends deny command:", e);
                                e.printStackTrace();
                            }
                        }))
                .register();
    }
}
