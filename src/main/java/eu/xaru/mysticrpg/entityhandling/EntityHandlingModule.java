package eu.xaru.mysticrpg.entityhandling;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * The EntityHandlingModule previously loaded/spawned persistent entities from "entities.yml"
 * after a short delay. Now, we comment out loadAndSpawnAll() so that we do NOT rely on that
 * for spawning old NPCs. Instead, we let the CustomNPCModule call npc.spawn() for each loaded NPC.
 */
public class EntityHandlingModule implements IBaseModule {

    private final JavaPlugin plugin;
    private EventManager eventManager;
    private EntityHandler entityHandler;

    public EntityHandlingModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        // Ensure SaveModule is loaded
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized. EntityHandlingModule requires SaveModule!");
        }
        this.entityHandler = EntityHandler.getInstance();

        // Setup an event manager if needed
        eventManager = new EventManager(plugin);
        registerCommands();

        DebugLogger.getInstance().log(Level.INFO, "EntityHandlingModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        registerEventHandlers();

        // We COMMENT OUT the old "loadAndSpawnAll()" call:
        /*
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DebugLogger.getInstance().log(Level.INFO,
                "Delayed spawn of saved entities after server fully started...", 0);

            entityHandler.loadAndSpawnAll();

        }, 60L); // 60 ticks = 3 seconds
        */

        DebugLogger.getInstance().log(Level.INFO,
                "EntityHandlingModule started (NOT spawning from entities.yml).", 0);
    }

    @Override
    public void stop() {
        // On shutdown, remove all stands & re-save
        entityHandler.shutdownCleanup();
        DebugLogger.getInstance().log(Level.INFO, "EntityHandlingModule stopped", 0);
    }

    @Override
    public void unload() {
        // Usually not needed if stop() does everything
    }

    /**
     * Example debug commands: /xaruentity list, /xaruentity delete <id>
     */
    private void registerCommands() {
        new CommandAPICommand("xaruentity")
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            entityHandler.listManagedEntities(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("entityId"))
                        .executesPlayer((player, args) -> {
                            String entityId = (String) args.get("entityId");
                            boolean removed = entityHandler.removeEntityById(entityId);
                            if (removed) {
                                player.sendMessage("Removed entity with ID: " + entityId);
                            } else {
                                player.sendMessage("No entity found with ID: " + entityId);
                            }
                        })
                )
                .register();
    }

    private void registerEventHandlers() {
        // Example on PlayerQuit
        eventManager.registerEvent(PlayerQuitEvent.class, event -> {
            // Possibly remove ephemeral entities for that player, etc.
        });
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        // We rely on SaveModule
        return List.of(SaveModule.class);
    }

    @Override
    public eu.xaru.mysticrpg.enums.EModulePriority getPriority() {
        return eu.xaru.mysticrpg.enums.EModulePriority.LOW;
    }
}
