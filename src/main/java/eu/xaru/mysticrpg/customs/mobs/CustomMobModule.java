package eu.xaru.mysticrpg.customs.mobs;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.MobGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import eu.xaru.mysticrpg.economy.EconomyModule;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class CustomMobModule implements IBaseModule, Listener {

    private final JavaPlugin plugin;
    private DebugLoggerModule logger;
    private final Map<String, CustomMob> mobConfigurations = new HashMap<>();
    public MobManager mobManager;
    private MobGUI mobGUI;

    public CustomMobModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule == null) {
            logger.error("PartyModule is not loaded. CustomMobModule requires PartyModule as a dependency.");
            return;
        }

        if (logger == null) {
            Bukkit.getLogger().severe("DebugLoggerModule not initialized. CustomMobModule cannot function without it.");
            return;
        }

        loadMobConfigurations();

        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule == null) {
            logger.error("EconomyModule is not loaded. CustomMobModule requires EconomyModule as a dependency.");
            return;
        }
        EconomyHelper economyHelper = economyModule.getEconomyHelper();

        // Initialize MobManager after loading mob configurations
        mobManager = new MobManager(plugin, mobConfigurations, economyHelper);

        registerCommands();

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(new CustomMobDamageHandler(mobManager), plugin);

        logger.log(Level.INFO, "CustomMobModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "CustomMobModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "CustomMobModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "CustomMobModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class, LevelModule.class, EconomyModule.class, PartyModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void loadMobConfigurations() {
        File mobFolder = new File(plugin.getDataFolder(), "custom/mobs");
        if (!mobFolder.exists() && !mobFolder.mkdirs()) {
            logger.error("Failed to create mobs folder.");
            return;
        }

        File[] files = mobFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String mobId = config.getString("id");
                    String mobName = config.getString("name");
                    if (mobId == null || mobId.isEmpty()) {
                        logger.error("Mob ID is missing in file: " + file.getName());
                        continue;
                    }
                    if (mobName == null || mobName.isEmpty()) {
                        logger.error("Mob name is missing in file: " + file.getName());
                        continue;
                    }
                    EntityType entityType = EntityType.valueOf(config.getString("type", "ZOMBIE").toUpperCase());
                    double health = config.getDouble("health", 20.0);
                    int level = config.getInt("level", 1);

                    int experienceReward = config.getInt("experienceReward", 0);
                    double currencyReward = config.getDouble("currencyReward", 0.0);

                    // Read new attributes
                    double baseDamage = config.getDouble("damage", 2.0); // Default damage
                    double baseArmor = config.getDouble("armor", 0.0);   // Default armor
                    double movementSpeed = config.getDouble("movement_speed", 0.2); // Default movement speed

                    Map<String, Integer> customAttributes = new HashMap<>();
                    if (config.contains("customAttributes")) {
                        for (String key : config.getConfigurationSection("customAttributes").getKeys(false)) {
                            customAttributes.put(key, config.getInt("customAttributes." + key));
                        }
                    }

                    List<String> assignedAreas = config.getStringList("assigned_areas");

                    // Load area settings if any
                    Map<String, CustomMob.AreaSettings> areaSettingsMap = new HashMap<>();
                    if (config.contains("area_settings")) {
                        for (String areaKey : config.getConfigurationSection("area_settings").getKeys(false)) {
                            String path = "area_settings." + areaKey;
                            int maxAmount = config.getInt(path + ".max_amount", 1);
                            int respawnAfterSeconds = config.getInt(path + ".respawn_after_seconds", -1);
                            boolean respawnIfAllDead = config.getBoolean(path + ".respawn_if_all_dead", false);

                            CustomMob.AreaSettings areaSettings = new CustomMob.AreaSettings(maxAmount, respawnAfterSeconds, respawnIfAllDead);
                            areaSettingsMap.put(areaKey, areaSettings);
                        }
                    }

                    // Load drops
                    List<CustomMob.DropItem> drops = new ArrayList<>();
                    if (config.contains("drops")) {
                        List<Map<?, ?>> dropList = config.getMapList("drops");
                        for (Map<?, ?> dropConfig : dropList) {
                            String type = (String) dropConfig.get("type");
                            if (type == null) {
                                logger.error("Drop type is missing for mob: " + mobName);
                                continue;
                            }
                            String id = (String) dropConfig.get("id"); // For custom items
                            String material = (String) dropConfig.get("material"); // For materials
                            int amount = (dropConfig.get("amount") != null) ? (int) dropConfig.get("amount") : 1;
                            double chance = (dropConfig.get("chance") != null) ? ((Number) dropConfig.get("chance")).doubleValue() : 1.0;

                            CustomMob.DropItem dropItem = new CustomMob.DropItem(type, id, material, amount, chance);
                            drops.add(dropItem);
                        }
                    }

                    // Load equipment
                    CustomMob.Equipment equipment = null;
                    if (config.contains("equipment")) {
                        String helmet = config.getString("equipment.helmet.custom_item_id");
                        String chestplate = config.getString("equipment.chestplate.custom_item_id");
                        String leggings = config.getString("equipment.leggings.custom_item_id");
                        String boots = config.getString("equipment.boots.custom_item_id");
                        String weapon = config.getString("equipment.weapon.custom_item_id");

                        equipment = new CustomMob.Equipment(helmet, chestplate, leggings, boots, weapon);
                    }

                    // Read the model ID from the config
                    String modelId = config.getString("model_id");

                    CustomMob customMob = new CustomMob(
                            mobId, mobName, entityType, health, level, experienceReward, currencyReward, customAttributes,
                            assignedAreas, areaSettingsMap, drops, baseDamage, baseArmor, movementSpeed, equipment,
                            modelId // Pass the modelId to the constructor
                    );

                    mobConfigurations.put(mobId, customMob);
                    logger.log(Level.INFO, "Loaded mob configuration: " + mobId, 0);
                } catch (Exception e) {
                    logger.error("Failed to load mob configuration from file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    private void registerCommands() {
        new CommandAPICommand("custommob")
                .withPermission("mysticcore.custommob")
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            if (mobConfigurations.isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No mobs are loaded."));
                            } else {
                                player.sendMessage(Utils.getInstance().$("Loaded Mobs:"));
                                mobConfigurations.keySet().forEach(mobId -> player.sendMessage(Utils.getInstance().$("- " + mobId)));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("spawn")
                        .withArguments(new StringArgument("mobId").replaceSuggestions(ArgumentSuggestions.strings(mobConfigurations.keySet().toArray(new String[0]))))
                        .executesPlayer((player, args) -> {
                            String mobId = (String) args.get(0);
                            CustomMob customMob = mobConfigurations.get(mobId);

                            if (customMob == null) {
                                player.sendMessage(Utils.getInstance().$("Mob not found: " + mobId));
                                return;
                            }

                            Location location = player.getLocation();
                            mobManager.spawnMobAtLocation(customMob, location);

                            player.sendMessage(Utils.getInstance().$("Spawned mob: " + customMob.getName() + " at your location."));
                        }))
                .withSubcommand(new CommandAPICommand("gui")
                        .executesPlayer((player, args) -> {
                            MobGUI mobGUI = new MobGUI(mobManager);
                            mobGUI.openMobGUI(player);
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            loadMobConfigurations(); // Reload the mob configurations from YAML files
                            sender.sendMessage(Utils.getInstance().$("Mob configurations reloaded successfully."));
                        }))
                .register();
    }
}
