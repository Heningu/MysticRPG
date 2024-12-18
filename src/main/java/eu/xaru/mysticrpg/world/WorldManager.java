package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.admin.AdminModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class WorldManager {

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final Map<String, Boolean> globalFlags = new HashMap<>();
    private AdminModule adminModule;

    public WorldManager(JavaPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        globalFlags.put("break", false);
        globalFlags.put("pvp", false);
        globalFlags.put("place", false);

        // Get AdminModule now that dependencies are guaranteed
        adminModule = ModuleManager.getInstance().getModuleInstance(AdminModule.class);

        registerEvents();
        registerCommands();
    }

    private void registerEvents() {
        eventManager.registerEvent(BlockBreakEvent.class, event -> {
            DebugLogger.getInstance().log("BlockBreakEvent: Player=" + event.getPlayer().getName() +
                    " Block=" + event.getBlock().getType() + " Loc=" + formatLoc(event.getBlock().getLocation()));
            if (!playerBypasses(event.getPlayer()) && !isAllowed("break", event.getPlayer().getLocation())) {
                DebugLogger.getInstance().log("Block breaking disallowed for non-admin, cancelling event.");
                event.setCancelled(true);
            }
        }, EventPriority.HIGHEST);

        eventManager.registerEvent(BlockPlaceEvent.class, event -> {
            DebugLogger.getInstance().log("BlockPlaceEvent: Player=" + event.getPlayer().getName() +
                    " Block=" + event.getBlock().getType() + " Loc=" + formatLoc(event.getBlock().getLocation()));
            if (!playerBypasses(event.getPlayer()) && !isAllowed("place", event.getPlayer().getLocation())) {
                DebugLogger.getInstance().log("Block placing disallowed for non-admin, cancelling event.");
                event.setCancelled(true);
            }
        }, EventPriority.HIGHEST);

        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            Entity damager = event.getDamager();
            Entity victim = event.getEntity();
            if (damager instanceof Player dPlayer && victim instanceof Player vPlayer) {
                DebugLogger.getInstance().log("EntityDamageByEntityEvent (PVP): " +
                        dPlayer.getName() + " -> " + vPlayer.getName() +
                        " Loc=" + formatLoc(victim.getLocation()));
                if (!playerBypasses(dPlayer) && !isAllowed("pvp", victim.getLocation())) {
                    DebugLogger.getInstance().log("PVP disallowed for non-admin, cancelling event.");
                    event.setCancelled(true);
                }
            }
        }, EventPriority.HIGHEST);
    }

    private void registerCommands() {
        new CommandAPICommand("worldflag")
                .withPermission("mysticrpg.world.flag")
                .withArguments(new StringArgument("flag")
                        .replaceSuggestions(ArgumentSuggestions.strings("break", "pvp", "place"))
                )
                .withArguments(new BooleanArgument("value"))
                .executes((sender, args) -> {
                    String flag = (String) args.get("flag");
                    boolean value = (boolean) args.get("value");
                    globalFlags.put(flag.toLowerCase(), value);
                    sender.sendMessage("Global flag " + flag + " set to " + value);
                    DebugLogger.getInstance().log("Global flag " + flag + " set to " + value);
                })
                .register();
    }

    public boolean isAllowed(String flag, Location loc) {
        boolean allowed = globalFlags.getOrDefault(flag, false);
        DebugLogger.getInstance().log("Checking if allowed: " + flag + " at " + formatLoc(loc) + " -> " + allowed);
        return allowed;
    }

    private boolean playerBypasses(Player player) {
        if (adminModule != null && adminModule.isInAdminMode(player)) {
            DebugLogger.getInstance().log("Player " + player.getName() + " is admin, bypassing all flags.");
            return true;
        }
        return false;
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Map<String, Boolean> getGlobalFlags() {
        return globalFlags;
    }
}
