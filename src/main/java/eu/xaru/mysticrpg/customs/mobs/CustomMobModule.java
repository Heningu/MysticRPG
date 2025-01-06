package eu.xaru.mysticrpg.customs.mobs;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.admin.MobGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * CustomMobModule is responsible for loading custom mob YAML files from
 * "plugins/MysticRPG/custom/mobs/*.yml", storing them in a mobConfigurations map,
 * then allowing you to spawn them in-game via commands.
 */
public class CustomMobModule implements IBaseModule, Listener {

    private final JavaPlugin plugin;
    private final Map<String, CustomMob> mobConfigurations = new HashMap<>();
    private MobManager mobManager;

    public CustomMobModule() {
        // We load the plugin reference from MysticCore
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        // Check dependencies
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule == null) {
            DebugLogger.getInstance().error("PartyModule not found. CustomMobModule requires PartyModule!");
            return;
        }
        EconomyModule econMod = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (econMod == null) {
            DebugLogger.getInstance().error("EconomyModule not found. CustomMobModule requires EconomyModule!");
            return;
        }
        EconomyHelper economyHelper = econMod.getEconomyHelper();

        // Load from standard YAML approach
        loadMobConfigurations();

        // Create manager
        mobManager = new MobManager(plugin, mobConfigurations, economyHelper);
        registerCommands();

        // Register the event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        DebugLogger.getInstance().log(Level.INFO, "CustomMobModule initialized successfully.", 0);
    }

    @Override
    public void start() { }

    @Override
    public void stop()  { }

    @Override
    public void unload(){ }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        // We rely on LevelModule, EconomyModule, and PartyModule
        return List.of(LevelModule.class, EconomyModule.class, PartyModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        // Priority can be LOW if desired
        return EModulePriority.LOW;
    }

    /**
     * Loads all .yml files from "plugins/MysticRPG/custom/mobs" and parses them
     * into CustomMob objects, stored in mobConfigurations.
     */
    private void loadMobConfigurations() {
        mobConfigurations.clear();
        File folder = new File(plugin.getDataFolder(), "custom/mobs");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration yml = new YamlConfiguration();
            try {
                yml.load(file);

                String mobId = yml.getString("id", "missing_id");
                String mobName = yml.getString("name", "UnnamedMob");
                String typeStr = yml.getString("type", "ZOMBIE").toUpperCase(Locale.ROOT);
                EntityType et = EntityType.valueOf(typeStr);

                double health = yml.getDouble("health", 20.0);
                int level = yml.getInt("level", 1);
                int xpReward = yml.getInt("experienceReward", 0);
                int goldReward = yml.getInt("currencyReward", 0);

                double baseDmg = yml.getDouble("damage", 2.0);
                double baseArmor = yml.getDouble("armor", 0.0);
                double moveSpeed = yml.getDouble("movement_speed", 0.2);

                // customAttributes
                Map<String, Integer> customAttrs = new HashMap<>();
                if (yml.isConfigurationSection("customAttributes")) {
                    for (String attrKey : yml.getConfigurationSection("customAttributes").getKeys(false)) {
                        int val = yml.getInt("customAttributes." + attrKey, 0);
                        customAttrs.put(attrKey, val);
                    }
                }

                // assignedAreas
                List<String> assignedAreas = yml.getStringList("assigned_areas");

                // area_settings
                Map<String, CustomMob.AreaSettings> areaSettingsMap = new HashMap<>();
                if (yml.isConfigurationSection("area_settings")) {
                    for (String areaKey : yml.getConfigurationSection("area_settings").getKeys(false)) {
                        int maxAmt = yml.getInt("area_settings." + areaKey + ".max_amount", 1);
                        int respawnSecs = yml.getInt("area_settings." + areaKey + ".respawn_after_seconds", -1);
                        boolean respawnIfAllDead = yml.getBoolean("area_settings." + areaKey + ".respawn_if_all_dead", false);
                        areaSettingsMap.put(areaKey, new CustomMob.AreaSettings(maxAmt, respawnSecs, respawnIfAllDead));
                    }
                }

                // drops
                List<CustomMob.DropItem> drops = new ArrayList<>();
                if (yml.isList("drops")) {
                    for (Object o : yml.getList("drops")) {
                        if (o instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String,Object> dropMap = (Map<String,Object>) o;
                            String type = (String) dropMap.getOrDefault("type", "material");
                            String dId = (String) dropMap.getOrDefault("id", "");
                            String mat = (String) dropMap.getOrDefault("material", Material.DIRT.name());
                            int amt = (int) dropMap.getOrDefault("amount", 1);
                            double chance = ((Number) dropMap.getOrDefault("chance", 1.0)).doubleValue();

                            CustomMob.DropItem di = new CustomMob.DropItem(type, dId, mat, amt, chance);
                            drops.add(di);
                        }
                    }
                }

                // equipment
                CustomMob.Equipment eq = null;
                if (yml.isConfigurationSection("equipment")) {
                    String helmet    = yml.getString("equipment.helmet.custom_item_id", null);
                    String chestplate= yml.getString("equipment.chestplate.custom_item_id", null);
                    String leggings  = yml.getString("equipment.leggings.custom_item_id", null);
                    String boots     = yml.getString("equipment.boots.custom_item_id", null);
                    String weapon    = yml.getString("equipment.weapon.custom_item_id", null);
                    eq = new CustomMob.Equipment(helmet, chestplate, leggings, boots, weapon);
                }

                // model
                String modelId = yml.getString("model_id", "");

                // boss bar
                CustomMob.BossBarConfig bossBarConfig = null;
                if (yml.isConfigurationSection("BossBar")) {
                    boolean enabled = yml.getBoolean("BossBar.Enabled", false);
                    String bTitle = yml.getString("BossBar.Title", mobName);
                    String bColor = yml.getString("BossBar.Color", "RED");
                    double bRange = yml.getDouble("BossBar.Range", 64.0);
                    String bStyle = yml.getString("BossBar.Style", "SEGMENTED_20");

                    BarColor bc = BarColor.valueOf(bColor.toUpperCase(Locale.ROOT));
                    BarStyle bs = BarStyle.valueOf(bStyle.toUpperCase(Locale.ROOT));
                    bTitle = bTitle + " [LVL " + level + "]";
                    bossBarConfig = new CustomMob.BossBarConfig(enabled, bTitle, bc, bRange, bs);
                }

                // Create the CustomMob object
                CustomMob cMob = new CustomMob(
                        mobId, mobName, et, health, level,
                        xpReward, goldReward, customAttrs,
                        assignedAreas, areaSettingsMap, drops,
                        baseDmg, baseArmor, moveSpeed, eq,
                        modelId, bossBarConfig
                );

                mobConfigurations.put(mobId, cMob);
                DebugLogger.getInstance().log(Level.INFO, "Loaded mob config: " + mobId + " from " + file.getName(), 0);

            } catch (Exception ex) {
                DebugLogger.getInstance().error("Failed to load custom mob from " + file.getName(), ex);
            }
        }
        DebugLogger.getInstance().log(Level.INFO, "Finished loading " + mobConfigurations.size() + " custom mobs.", 0);
    }

    /**
     * Registers the /custommob command (and subcommands) using CommandAPI
     */
    private void registerCommands() {
        new CommandAPICommand("custommob")
                .withPermission("mysticcore.custommob")

                // /custommob list
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            if (mobConfigurations.isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No mobs loaded."));
                            } else {
                                player.sendMessage(Utils.getInstance().$("Loaded Mobs:"));
                                for (String mobId : mobConfigurations.keySet()) {
                                    player.sendMessage(Utils.getInstance().$("- " + mobId));
                                }
                            }
                        })
                )

                // /custommob spawn <mobId>
                .withSubcommand(new CommandAPICommand("spawn")
                        .withArguments(new StringArgument("mobId")
                                // Use strings(...) for suggestions
                                .replaceSuggestions(ArgumentSuggestions.strings(
                                        mobConfigurations.keySet().toArray(new String[0])
                                ))
                        )
                        .executesPlayer((player, args) -> {
                            String mobId = (String) args.get(0);
                            CustomMob customMob = mobConfigurations.get(mobId);
                            if (customMob == null) {
                                player.sendMessage(Utils.getInstance().$("Mob not found: " + mobId));
                                return;
                            }
                            mobManager.spawnMobAtLocation(customMob, player.getLocation());
                            player.sendMessage(Utils.getInstance().$("Spawned " + customMob.getName() + " at your location."));
                        })
                )

                // /custommob gui
                .withSubcommand(new CommandAPICommand("gui")
                        .executesPlayer((player, args) -> {
                            MobGUI gui = new MobGUI();
                            gui.openMobGUI(player);
                        })
                )

                // /custommob reload
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            loadMobConfigurations();
                            sender.sendMessage("Mob configurations reloaded successfully.");
                        })
                )

                // Finally register the base command
                .register();
    }

    /**
     * Gives external access to the MobManager,
     * so other modules can call customMobModule.getMobManager().
     */
    public MobManager getMobManager() {
        return mobManager;
    }
}
