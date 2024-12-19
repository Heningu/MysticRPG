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
    private RegionManager regionManager;

    public WorldManager(JavaPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        globalFlags.put("break", false);
        globalFlags.put("pvp", false);
        globalFlags.put("place", false);

        adminModule = ModuleManager.getInstance().getModuleInstance(AdminModule.class);

        registerEvents();
        registerCommands();
    }

    public void setRegionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    private void registerEvents() {
        eventManager.registerEvent(BlockBreakEvent.class, event -> {
            Location blockLoc = event.getBlock().getLocation();
            DebugLogger.getInstance().log("BlockBreakEvent: Player=" + event.getPlayer().getName() +
                    " Block=" + event.getBlock().getType() + " Loc=" + formatLoc(blockLoc));
            if (!isMainWorld(blockLoc)) {
                // Not in "world", allow by default
                DebugLogger.getInstance().log("Not in main world, allowing break by default.");
                return;
            }
            if (!playerBypasses(event.getPlayer()) && !isAllowed("break", blockLoc)) {
                DebugLogger.getInstance().log("Block breaking disallowed for non-admin, cancelling event.");
                event.setCancelled(true);
            }
        }, EventPriority.HIGHEST);

        eventManager.registerEvent(BlockPlaceEvent.class, event -> {
            Location blockLoc = event.getBlock().getLocation();
            DebugLogger.getInstance().log("BlockPlaceEvent: Player=" + event.getPlayer().getName() +
                    " Block=" + event.getBlock().getType() + " Loc=" + formatLoc(blockLoc));
            if (!isMainWorld(blockLoc)) {
                // Not in "world", allow by default
                DebugLogger.getInstance().log("Not in main world, allowing place by default.");
                return;
            }
            if (!playerBypasses(event.getPlayer()) && !isAllowed("place", blockLoc)) {
                DebugLogger.getInstance().log("Block placing disallowed for non-admin, cancelling event.");
                event.setCancelled(true);
            }
        }, EventPriority.HIGHEST);

        eventManager.registerEvent(EntityDamageByEntityEvent.class, event -> {
            Entity damager = event.getDamager();
            Entity victim = event.getEntity();
            if (damager instanceof Player dPlayer && victim instanceof Player vPlayer) {
                Location victimLoc = victim.getLocation();
                DebugLogger.getInstance().log("EntityDamageByEntityEvent (PVP): " +
                        dPlayer.getName() + " -> " + vPlayer.getName() +
                        " Loc=" + formatLoc(victimLoc));

                if (!isMainWorld(victimLoc)) {
                    // Not in "world", allow by default
                    DebugLogger.getInstance().log("Not in main world, allowing PVP by default.");
                    return;
                }

                if (!playerBypasses(dPlayer) && !isAllowed("pvp", victimLoc)) {
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

    /**
     * Checks if action is allowed at a given location. If in main world, we apply global/region logic.
     * If not in the main world "world", always return true (allowed).
     */
    public boolean isAllowed(String flag, Location loc) {
        // If not main world, allow by default
        if (!isMainWorld(loc)) {
            DebugLogger.getInstance().log("Location not in main world, " + flag + " allowed by default at " + formatLoc(loc));
            return true;
        }

        if (regionManager != null) {
            Region r = regionManager.getRegionAt(loc);
            if (r != null) {
                Boolean regionVal = r.getFlag(flag);
                if (regionVal != null) {
                    DebugLogger.getInstance().log("Region override: " + flag + " = " + regionVal + " in region " + r.getId());
                    return regionVal;
                }
                DebugLogger.getInstance().log("Region found but no specific flag override for " + flag);
            } else {
                DebugLogger.getInstance().log("No region at " + formatLoc(loc));
            }
        }

        boolean allowed = globalFlags.getOrDefault(flag, false);
        DebugLogger.getInstance().log("No region override, global: " + flag + " = " + allowed + " at " + formatLoc(loc));
        return allowed;
    }

    private boolean playerBypasses(Player player) {
        if (adminModule != null && adminModule.isInAdminMode(player)) {
            DebugLogger.getInstance().log("Player " + player.getName() + " is admin, bypassing all flags.");
            return true;
        }
        return false;
    }

    private boolean isMainWorld(Location loc) {
        return loc.getWorld() != null && loc.getWorld().getName().equals("world");
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Map<String, Boolean> getGlobalFlags() {
        return globalFlags;
    }
}
