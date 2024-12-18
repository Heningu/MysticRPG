package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.managers.EventManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RegionManager {

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final WorldManager worldManager;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, RegionSetup> setupMap = new HashMap<>();
    private final Map<UUID, String> viewingRegion = new HashMap<>();
    private final Map<UUID, Region> lastRegion = new HashMap<>();

    static class RegionSetup {
        String id;
        Location pos1;
        Location pos2;
        RegionSetup(String id) {
            this.id = id;
        }
    }

    public RegionManager(JavaPlugin plugin, EventManager eventManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.worldManager = worldManager;
        registerCommands();
        registerEvents();
        startBorderTask();
    }

    private void registerCommands() {
        new CommandAPICommand("region")
                .withPermission("mysticrpg.region.setup")
                .withSubcommand(new CommandAPICommand("setup")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            if (regions.containsKey(id)) {
                                player.sendMessage("Region with that ID already exists.");
                                return;
                            }
                            setupMap.put(player.getUniqueId(), new RegionSetup(id));
                            player.sendMessage("You are now setting up region " + id + ". Click two distinct blocks to define corners.");
                        }))
                .register();

        CommandAPICommand regionsCmd = new CommandAPICommand("regions")
                .withPermission("mysticrpg.region.manage")
                .withSubcommand(new CommandAPICommand("see")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            if (viewingRegion.containsKey(player.getUniqueId()) && viewingRegion.get(player.getUniqueId()).equals(id)) {
                                viewingRegion.remove(player.getUniqueId());
                                player.sendMessage("No longer showing region " + id);
                            } else {
                                viewingRegion.put(player.getUniqueId(), id);
                                player.sendMessage("Showing region " + id);
                            }
                        }))
                .withSubcommand(new CommandAPICommand("flag")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("flag"))
                        .withArguments(new BooleanArgument("value"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String flag = (String) args.get("flag");
                            boolean val = (boolean) args.get("value");
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            r.setFlag(flag, val);
                            player.sendMessage("Set flag " + flag + " to " + val + " for region " + id);
                        }))
                .withSubcommand(new CommandAPICommand("seteffect")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("effect"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String eff = ((String) args.get("effect")).toUpperCase();
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            PotionEffectType pet = PotionEffectType.getByName(eff);
                            if (pet == null) {
                                player.sendMessage("No such effect");
                                return;
                            }
                            r.addEffect(pet);
                            player.sendMessage("Added effect " + eff + " to region " + id);
                        }))
                .withSubcommand(new CommandAPICommand("removeeffect")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("effect"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String eff = ((String) args.get("effect")).toUpperCase();
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            PotionEffectType pet = PotionEffectType.getByName(eff);
                            if (pet == null) {
                                player.sendMessage("No such effect");
                                return;
                            }
                            r.removeEffect(pet);
                            player.sendMessage("Removed effect " + eff + " from region " + id);
                        }))
                .withSubcommand(new CommandAPICommand("title")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("title"))
                        .withArguments(new StringArgument("subtitle"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String title = ((String) args.get("title")).replace("_", " ");
                            String subtitle = ((String) args.get("subtitle")).replace("_", " ");
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            r.setTitle(title, subtitle);
                            player.sendMessage("Set title for region " + id);
                        }))
                .withSubcommand(new CommandAPICommand("removetitle")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            Region r = regions.get(id);
                            if (r == null) {
                                player.sendMessage("No such region");
                                return;
                            }
                            r.removeTitle();
                            player.sendMessage("Removed title for region " + id);
                        }))
                // Add "list" command
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            if (regions.isEmpty()) {
                                player.sendMessage("No regions defined.");
                            } else {
                                player.sendMessage("Current regions:");
                                for (String regionId : regions.keySet()) {
                                    player.sendMessage("- " + regionId);
                                }
                            }
                        }))
                // Add "remove" command
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            if (regions.remove(id) != null) {
                                player.sendMessage("Removed region " + id);
                            } else {
                                player.sendMessage("No such region " + id);
                            }
                        }));

        regionsCmd.register();
    }

    private void registerEvents() {
        eventManager.registerEvent(PlayerInteractEvent.class, event -> {
            Player p = event.getPlayer();
            if (!setupMap.containsKey(p.getUniqueId())) return;

            // Only proceed if player right-clicked a block with main hand
            if (event.getHand() == null || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

            event.setCancelled(true);
            RegionSetup rs = setupMap.get(p.getUniqueId());

            if (rs.pos1 == null) {
                rs.pos1 = event.getClickedBlock().getLocation();
                p.sendMessage("First corner set. Click another block to set the second corner.");
                return;
            }

            if (rs.pos2 == null) {
                rs.pos2 = event.getClickedBlock().getLocation();
                Region r = new Region(rs.id);
                r.setPos1(rs.pos1);
                r.setPos2(rs.pos2);
                regions.put(rs.id, r);
                setupMap.remove(p.getUniqueId());
                p.sendMessage("Region " + rs.id + " created!");
            }
        }, org.bukkit.event.EventPriority.HIGHEST);

        eventManager.registerEvent(PlayerMoveEvent.class, event -> {
            Player p = event.getPlayer();
            Region current = getRegionAt(p.getLocation());
            Region last = lastRegion.get(p.getUniqueId());
            if (current != last) {
                if (last != null) {
                    for (PotionEffectType pe : last.getEffects()) {
                        p.removePotionEffect(pe);
                    }
                }
                if (current != null) {
                    for (PotionEffectType pe : current.getEffects()) {
                        p.addPotionEffect(new PotionEffect(pe, Integer.MAX_VALUE, 0, true, false));
                    }
                    if (current.getTitle() != null) {
                        p.sendTitle(current.getTitle(), current.getSubtitle() == null ? "" : current.getSubtitle(), 10, 70, 20);
                    }
                }
                lastRegion.put(p.getUniqueId(), current);
            }
        }, org.bukkit.event.EventPriority.MONITOR);
    }

    public Region getRegionAt(Location loc) {
        for (Region r : regions.values()) {
            if (r.contains(loc)) return r;
        }
        return null;
    }

    private void startBorderTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID,String> e : viewingRegion.entrySet()) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p == null || !p.isOnline()) continue;
                Region r = regions.get(e.getValue());
                if (r == null) continue;
                if (r.getPos1() == null || r.getPos2() == null) continue;
                drawBorder(p, r);
            }
        }, 20, 20);
    }

    private void drawBorder(Player p, Region r) {
        Location p1 = r.getPos1();
        Location p2 = r.getPos2();
        if (!p1.getWorld().equals(p.getWorld())) return;
        int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (int x = x1; x <= x2; x++) {
            spawnParticle(p, new Location(p1.getWorld(), x+0.5, y1, z1+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x+0.5, y2, z1+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x+0.5, y1, z2+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x+0.5, y2, z2+0.5));
        }
        for (int y = y1; y <= y2; y++) {
            spawnParticle(p, new Location(p1.getWorld(), x1+0.5, y, z1+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x2+0.5, y, z1+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x1+0.5, y, z2+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x2+0.5, y, z2+0.5));
        }
        for (int z = z1; z <= z2; z++) {
            spawnParticle(p, new Location(p1.getWorld(), x1+0.5, y1, z+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x1+0.5, y2, z+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x2+0.5, y1, z+0.5));
            spawnParticle(p, new Location(p1.getWorld(), x2+0.5, y2, z+0.5));
        }
    }

    private void spawnParticle(Player p, Location loc) {
        p.spawnParticle(Particle.FLAME, loc, 1, 0,0,0,0);
    }
}
