package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
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

/**
 * Manages loading, saving, and usage of Regions in /plugins/MysticRPG/regions/regions.yml
 */
public class RegionManager {

    private static final String REGIONS_FILE_PATH = "regions/regions.yml";

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final WorldManager worldManager;

    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, RegionSetup> setupMap = new HashMap<>();
    private final Map<UUID, String> viewingRegion = new HashMap<>();
    private final Map<UUID, Region> lastRegion = new HashMap<>();

    private DynamicConfig regionConfig;

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

        loadRegions();
        registerCommands();
        registerEvents();
        startBorderTask();
    }

    private void loadRegions() {
        DynamicConfigManager.loadConfig(REGIONS_FILE_PATH, REGIONS_FILE_PATH);
        regionConfig = DynamicConfigManager.getConfig(REGIONS_FILE_PATH);
        if (regionConfig == null) {
            DebugLogger.getInstance().error("Failed to load region config: " + REGIONS_FILE_PATH);
            return;
        }

        // "regions" is presumably a sub-map of ID->region data
        Object regionsObj = regionConfig.get("regions");
        if (!(regionsObj instanceof Map<?,?> regionsMap)) {
            DebugLogger.getInstance().log("No regions to load or 'regions' not a map.");
            return;
        }

        for (Map.Entry<?,?> e : regionsMap.entrySet()) {
            String id = String.valueOf(e.getKey());
            if (e.getValue() instanceof Map<?,?> rMap) {
                Region r = new Region(id);

                // pos1
                Object pos1Obj = rMap.get("pos1");
                if (pos1Obj instanceof Map<?,?> p1Map) {
                    r.setPos1(loadLocation(p1Map));
                }
                // pos2
                Object pos2Obj = rMap.get("pos2");
                if (pos2Obj instanceof Map<?,?> p2Map) {
                    r.setPos2(loadLocation(p2Map));
                }

                // flags: rMap->"flags" -> map of string->bool
                Object flagsObj = rMap.get("flags");
                if (flagsObj instanceof Map<?,?> flMap) {
                    for (Map.Entry<?,?> fl : flMap.entrySet()) {
                        String key = String.valueOf(fl.getKey());
                        boolean val = parseBoolean(fl.getValue(), false);
                        r.setFlag(key, val);
                    }
                }

                // effects: rMap->"effects" -> map of effectName->amplifier
                Object effObj = rMap.get("effects");
                if (effObj instanceof Map<?,?> effMap) {
                    for (Map.Entry<?,?> eff : effMap.entrySet()) {
                        String effName = eff.getKey().toString().toUpperCase(Locale.ROOT);
                        PotionEffectType pet = PotionEffectType.getByName(effName);
                        if (pet != null) {
                            int amp = parseInt(eff.getValue(), 0);
                            r.addEffect(pet, amp);
                        }
                    }
                } else if (effObj instanceof List<?> oldList) {
                    // old format
                    for (Object eff : oldList) {
                        String effStr = String.valueOf(eff).toUpperCase(Locale.ROOT);
                        PotionEffectType pet = PotionEffectType.getByName(effStr);
                        if (pet != null) {
                            r.addEffect(pet, 0);
                        }
                    }
                }

                // title
                String title = parseString(rMap.get("title"), null);
                String subtitle = parseString(rMap.get("subtitle"), null);
                if (title != null) {
                    r.setTitle(title, subtitle);
                }

                regions.put(id, r);
                DebugLogger.getInstance().log("Loaded region: " + id);
            }
        }
    }

    private void saveRegions() {
        // Clear "regions" in config
        regionConfig.set("regions", null);

        Map<String, Object> newRegionsMap = new HashMap<>();
        for (Map.Entry<String, Region> e : regions.entrySet()) {
            String id = e.getKey();
            Region r = e.getValue();

            Map<String, Object> rMap = new HashMap<>();

            // pos1
            if (r.getPos1() != null) {
                Map<String,Object> p1 = new HashMap<>();
                p1.put("world", r.getPos1().getWorld().getName());
                p1.put("x", r.getPos1().getBlockX());
                p1.put("y", r.getPos1().getBlockY());
                p1.put("z", r.getPos1().getBlockZ());
                rMap.put("pos1", p1);
            }
            // pos2
            if (r.getPos2() != null) {
                Map<String,Object> p2 = new HashMap<>();
                p2.put("world", r.getPos2().getWorld().getName());
                p2.put("x", r.getPos2().getBlockX());
                p2.put("y", r.getPos2().getBlockY());
                p2.put("z", r.getPos2().getBlockZ());
                rMap.put("pos2", p2);
            }

            // flags
            if (!r.getFlags().isEmpty()) {
                Map<String,Object> flMap = new HashMap<>();
                for (Map.Entry<String, Boolean> fl : r.getFlags().entrySet()) {
                    flMap.put(fl.getKey(), fl.getValue());
                }
                rMap.put("flags", flMap);
            }

            // effects
            if (!r.getEffects().isEmpty()) {
                Map<String,Object> effMap = new HashMap<>();
                for (Map.Entry<PotionEffectType,Integer> eff : r.getEffects().entrySet()) {
                    effMap.put(eff.getKey().getName(), eff.getValue());
                }
                rMap.put("effects", effMap);
            }

            // title
            if (r.getTitle() != null) {
                rMap.put("title", r.getTitle());
                if (r.getSubtitle() != null) {
                    rMap.put("subtitle", r.getSubtitle());
                }
            }

            newRegionsMap.put(id, rMap);
        }

        regionConfig.set("regions", newRegionsMap);
        regionConfig.saveIfNeeded();
    }

    private Location loadLocation(Map<?,?> map) {
        String worldName = parseString(map.get("world"), null);
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        int x = parseInt(map.get("x"), 0);
        int y = parseInt(map.get("y"), 0);
        int z = parseInt(map.get("z"), 0);
        return new Location(w, x, y, z);
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
                        .withArguments(new StringArgument("flag").replaceSuggestions(ArgumentSuggestions.strings("break", "pvp", "place")))
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
                            saveRegions();
                        }))
                .withSubcommand(new CommandAPICommand("seteffect")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("effect"))
                        .withArguments(new IntegerArgument("amplifier", 0, 10))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String eff = ((String) args.get("effect")).toUpperCase(Locale.ROOT);
                            int amplifier = (int) args.get("amplifier");
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
                            r.addEffect(pet, amplifier);
                            player.sendMessage("Added effect " + eff + " with amplifier " + amplifier + " to region " + id);
                            saveRegions();
                        }))
                .withSubcommand(new CommandAPICommand("removeeffect")
                        .withArguments(new StringArgument("id"))
                        .withArguments(new StringArgument("effect"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String eff = ((String) args.get("effect")).toUpperCase(Locale.ROOT);
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
                            saveRegions();
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
                            saveRegions();
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
                            saveRegions();
                        }))
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
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            if (regions.remove(id) != null) {
                                player.sendMessage("Removed region " + id);
                                saveRegions();
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

            if (event.getHand() == null || event.getHand() != EquipmentSlot.HAND) return;
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
                saveRegions();
            }
        }, org.bukkit.event.EventPriority.HIGHEST);

        eventManager.registerEvent(PlayerMoveEvent.class, event -> {
            Player p = event.getPlayer();
            Region current = getRegionAt(p.getLocation());
            Region last = lastRegion.get(p.getUniqueId());
            if (current != last) {
                if (last != null) {
                    for (PotionEffectType pe : last.getEffects().keySet()) {
                        p.removePotionEffect(pe);
                    }
                }
                if (current != null) {
                    for (Map.Entry<PotionEffectType, Integer> eff : current.getEffects().entrySet()) {
                        p.addPotionEffect(new PotionEffect(eff.getKey(), Integer.MAX_VALUE, eff.getValue(), true, false));
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
            for (Map.Entry<UUID, String> e : viewingRegion.entrySet()) {
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
        if (p1 == null || p2 == null) return;
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

    private int parseInt(Object val, int fallback) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object val, boolean fallback) {
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        if (val instanceof String) {
            String s = ((String) val).toLowerCase(Locale.ROOT);
            if (s.equals("true") || s.equals("yes") || s.equals("1")) return true;
            if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
        }
        return fallback;
    }

    private String parseString(Object val, String fallback) {
        return (val != null) ? val.toString() : fallback;
    }
}
