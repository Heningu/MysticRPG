package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final WorldManager worldManager;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, RegionSetup> setupMap = new HashMap<>();
    private final Map<UUID, String> viewingRegion = new HashMap<>();
    private final Map<UUID, Region> lastRegion = new HashMap<>();

    private File regionsFile;
    private YamlConfiguration regionsConfig;

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
        File regionsFolder = new File(plugin.getDataFolder(), "regions");
        if (!regionsFolder.exists()) regionsFolder.mkdirs();

        regionsFile = new File(regionsFolder, "regions.yml");
        if (!regionsFile.exists()) {
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);

        ConfigurationSection sec = regionsConfig.getConfigurationSection("regions");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection rSec = sec.getConfigurationSection(id);
                if (rSec == null) continue;

                Region r = new Region(id);
                if (rSec.contains("pos1")) {
                    r.setPos1(loadLocation(rSec.getConfigurationSection("pos1")));
                }
                if (rSec.contains("pos2")) {
                    r.setPos2(loadLocation(rSec.getConfigurationSection("pos2")));
                }

                if (rSec.contains("flags")) {
                    ConfigurationSection fSec = rSec.getConfigurationSection("flags");
                    for (String f : fSec.getKeys(false)) {
                        boolean val = fSec.getBoolean(f);
                        r.setFlag(f, val);
                    }
                }

                // effects now stored as a map effectName -> amplifier
                if (rSec.contains("effects")) {
                    // Check if it's a section (map) or a list (old format)
                    if (rSec.isConfigurationSection("effects")) {
                        ConfigurationSection effSec = rSec.getConfigurationSection("effects");
                        for (String effName : effSec.getKeys(false)) {
                            PotionEffectType pet = PotionEffectType.getByName(effName.toUpperCase());
                            if (pet != null) {
                                int amp = effSec.getInt(effName, 0);
                                r.addEffect(pet, amp);
                            }
                        }
                    } else {
                        // Old format: list of effects without amplifier
                        List<String> effList = rSec.getStringList("effects");
                        for (String eff : effList) {
                            PotionEffectType pet = PotionEffectType.getByName(eff.toUpperCase());
                            if (pet != null) {
                                r.addEffect(pet, 0); // default amplifier 0
                            }
                        }
                    }
                }

                if (rSec.contains("title")) {
                    String title = rSec.getString("title");
                    String subtitle = rSec.getString("subtitle");
                    r.setTitle(title, subtitle);
                }

                regions.put(id, r);
                DebugLogger.getInstance().log("Loaded region: " + id);
            }
        } else {
            DebugLogger.getInstance().log("No regions to load.");
        }
    }

    private Location loadLocation(ConfigurationSection sec) {
        String worldName = sec.getString("world");
        World w = Bukkit.getWorld(worldName);
        int x = sec.getInt("x");
        int y = sec.getInt("y");
        int z = sec.getInt("z");
        return new Location(w,x,y,z);
    }

    private void saveRegions() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("regions");

        for (Map.Entry<String, Region> e : regions.entrySet()) {
            String id = e.getKey();
            Region r = e.getValue();
            ConfigurationSection rSec = sec.createSection(id);

            if (r.getPos1() != null) {
                ConfigurationSection p1 = rSec.createSection("pos1");
                saveLocation(r.getPos1(), p1);
            }

            if (r.getPos2() != null) {
                ConfigurationSection p2 = rSec.createSection("pos2");
                saveLocation(r.getPos2(), p2);
            }

            if (!r.getFlags().isEmpty()) {
                ConfigurationSection fSec = rSec.createSection("flags");
                for (Map.Entry<String, Boolean> fl : r.getFlags().entrySet()) {
                    fSec.set(fl.getKey(), fl.getValue());
                }
            }

            if (!r.getEffects().isEmpty()) {
                // Store effects as a section: effectName -> amplifier
                ConfigurationSection effSec = rSec.createSection("effects");
                for (Map.Entry<PotionEffectType,Integer> eff : r.getEffects().entrySet()) {
                    effSec.set(eff.getKey().getName(), eff.getValue());
                }
            }

            if (r.getTitle() != null) {
                rSec.set("title", r.getTitle());
                rSec.set("subtitle", r.getSubtitle());
            }
        }

        try {
            config.save(regionsFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveLocation(Location loc, ConfigurationSection sec) {
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getBlockX());
        sec.set("y", loc.getBlockY());
        sec.set("z", loc.getBlockZ());
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
                        .withArguments(new StringArgument("flag")
                                .replaceSuggestions(ArgumentSuggestions.strings("break","pvp","place"))
                        )
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
                        // Add amplifier argument
                        .withArguments(new IntegerArgument("amplifier",0,10))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            String eff = ((String) args.get("effect")).toUpperCase();
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
                    for (Map.Entry<PotionEffectType,Integer> eff : current.getEffects().entrySet()) {
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
        if (p1 == null || p2 == null || !p1.getWorld().equals(p.getWorld())) return;
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
