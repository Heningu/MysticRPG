package eu.xaru.mysticrpg.customs.mobs;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.mobs.actions.Action;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.customs.mobs.actions.Condition;
import eu.xaru.mysticrpg.customs.mobs.actions.conditions.DistanceCondition;
import eu.xaru.mysticrpg.customs.mobs.actions.steps.*;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.MobGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
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
    
    private final Map<String, CustomMob> mobConfigurations = new HashMap<>();
    private MobManager mobManager;
    private MobGUI mobGUI;

    public CustomMobModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {
        
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule == null) {
            DebugLogger.getInstance().error("PartyModule is not loaded. CustomMobModule requires PartyModule as a dependency.");
            return;
        }


        loadMobConfigurations();

        EconomyModule economyModule = ModuleManager.getInstance().getModuleInstance(EconomyModule.class);
        if (economyModule == null) {
            DebugLogger.getInstance().error("EconomyModule is not loaded. CustomMobModule requires EconomyModule as a dependency.");
            return;
        }
        EconomyHelper economyHelper = economyModule.getEconomyHelper();

        // Initialize MobManager after loading mob configurations
        mobManager = new MobManager(plugin, mobConfigurations, economyHelper);

        registerCommands();

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        DebugLogger.getInstance().log(Level.INFO, "CustomMobModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "CustomMobModule started", 0);
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "CustomMobModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "CustomMobModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of( LevelModule.class, EconomyModule.class, PartyModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void loadMobConfigurations() {
        File mobFolder = new File(plugin.getDataFolder(), "custom/mobs");
        if (!mobFolder.exists() && !mobFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create mobs folder.");
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
                        DebugLogger.getInstance().error("Mob ID is missing in file: " + file.getName());
                        continue;
                    }
                    if (mobName == null || mobName.isEmpty()) {
                        DebugLogger.getInstance().error("Mob name is missing in file: " + file.getName());
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
                                DebugLogger.getInstance().error("Drop type is missing for mob: " + mobName);
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

                    // Load animations
                    AnimationConfig animationConfig = loadAnimations(config);

                    // Load actions
                    Map<String, List<Action>> actions = loadActions(config, animationConfig);

                    // Load BossBar configuration
                    CustomMob.BossBarConfig bossBarConfig = null;
                    if (config.contains("BossBar")) {
                        ConfigurationSection bossBarSection = config.getConfigurationSection("BossBar");
                        if (bossBarSection != null) {
                            boolean enabled = bossBarSection.getBoolean("Enabled", false);
                            String title = bossBarSection.getString("Title", mobName);
                            String colorName = bossBarSection.getString("Color", "RED");
                            BarColor color = BarColor.valueOf(colorName.toUpperCase());
                            double range = bossBarSection.getDouble("Range", 64.0);
                            String styleName = bossBarSection.getString("Style", "SEGMENTED_20");
                            BarStyle style = BarStyle.valueOf(styleName.toUpperCase());

                            // Append level to title
                            title = title + " [LVL " + level + "]";

                            bossBarConfig = new CustomMob.BossBarConfig(enabled, title, color, range, style);
                        }
                    }

                    // Create CustomMob instance with bossBarConfig
                    CustomMob customMob = new CustomMob(
                            mobId, mobName, entityType, health, level, experienceReward, currencyReward, customAttributes,
                            assignedAreas, areaSettingsMap, drops, baseDamage, baseArmor, movementSpeed, equipment,
                            modelId, actions, animationConfig, bossBarConfig
                    );

                    mobConfigurations.put(mobId, customMob);
                    DebugLogger.getInstance().log(Level.INFO, "Loaded mob configuration: " + mobId, 0);
                } catch (Exception e) {
                    DebugLogger.getInstance().error("Failed to load mob configuration from file " + file.getName() + ":", e);
                }
            }
        }
    }

    private AnimationConfig loadAnimations(FileConfiguration config) {
        AnimationConfig animationConfig = new AnimationConfig();

        if (config.contains("animations")) {
            ConfigurationSection animationsSection = config.getConfigurationSection("animations");
            if (animationsSection != null) {
                if (animationsSection.contains("idle")) {
                    animationConfig.setIdleAnimation(animationsSection.getString("idle"));
                }
                if (animationsSection.contains("walk")) {
                    animationConfig.setWalkAnimation(animationsSection.getString("walk"));
                }
                if (animationsSection.contains("attack")) {
                    animationConfig.setAttackAnimation(animationsSection.getString("attack"));
                }
                // Add more animations as needed
            }
        }

        return animationConfig;
    }

    private Map<String, List<Action>> loadActions(FileConfiguration config, AnimationConfig animationConfig) {
        Map<String, List<Action>> actions = new HashMap<>();

        if (config.contains("actions")) {
            List<String> actionEntries = config.getStringList("actions");
            for (String actionEntry : actionEntries) {
                // The format is: path ~ trigger
                String[] parts = actionEntry.split("~");
                if (parts.length == 2) {
                    String actionPath = parts[0].trim();
                    String trigger = parts[1].trim();

                    // Load the action file
                    Action action = loadActionFromFile(actionPath, animationConfig);
                    if (action != null) {
                        actions.computeIfAbsent(trigger, k -> new ArrayList<>()).add(action);
                    }
                } else {
                    DebugLogger.getInstance().error("Invalid action entry: " + actionEntry);
                }
            }
        }

        return actions;
    }

    private Action loadActionFromFile(String actionPath, AnimationConfig animationConfig) {
        File actionFile = new File(plugin.getDataFolder(), "custom/mobs/" + actionPath + ".yml");
        if (!actionFile.exists()) {
            DebugLogger.getInstance().error("Action file not found: " + actionFile.getPath());
            return null;
        }
        try {
            FileConfiguration actionConfig = YamlConfiguration.loadConfiguration(actionFile);
            ConfigurationSection actionSection = actionConfig.getConfigurationSection("action");
            if (actionSection == null) {
                DebugLogger.getInstance().error("Action section missing in file: " + actionFile.getPath());
                return null;
            }
            double cooldown = actionSection.getDouble("Cooldown", 0.0);
            List<String> targetConditionStrings = actionSection.getStringList("TargetConditions");
            List<String> doActionsStrings = actionSection.getStringList("do");

            // Parse target conditions
            List<Condition> targetConditions = new ArrayList<>();
            for (String conditionString : targetConditionStrings) {
                Condition condition = parseCondition(conditionString);
                if (condition != null) {
                    targetConditions.add(condition);
                }
            }

            // Parse action steps
            List<ActionStep> actionSteps = new ArrayList<>();
            for (String actionString : doActionsStrings) {
                ActionStep step = parseActionStep(actionString, animationConfig);
                if (step != null) {
                    actionSteps.add(step);
                }
            }

            return new Action(cooldown, targetConditions, actionSteps);
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to load action from file " + actionFile.getPath() + ":", e);
            return null;
        }
    }

    private Condition parseCondition(String conditionString) {
        String[] parts = conditionString.trim().split("\\s+", 2);
        if (parts.length != 2) {
            DebugLogger.getInstance().error("Invalid condition format: " + conditionString);
            return null;
        }
        String conditionType = parts[0].trim();
        String conditionParam = parts[1].trim();

        switch (conditionType.toLowerCase()) {
            case "distance":
                return new DistanceCondition(conditionParam);
            // Add more conditions here
            default:
                DebugLogger.getInstance().error("Unknown condition type: " + conditionType);
                return null;
        }
    }

    private ActionStep parseActionStep(String actionString, AnimationConfig animationConfig) {
        String[] parts = actionString.split(":", 2);
        String command = parts[0].trim();
        String parameter = parts.length > 1 ? parts[1].trim() : null;

        // Replace placeholders
        if (parameter != null) {
            parameter = replacePlaceholders(parameter, animationConfig);
        }

        switch (command.toLowerCase()) {
            case "animation":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for animation action.");
                    return null;
                }
                String animationName = parameter.replace("\"", "");
                return new AnimationActionStep(animationName);
            case "delay":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for delay action.");
                    return null;
                }
                double delaySeconds;
                try {
                    delaySeconds = Double.parseDouble(parameter);
                } catch (NumberFormatException e) {
                    DebugLogger.getInstance().error("Invalid number format for delay action: " + parameter);
                    return null;
                }
                return new DelayActionStep(delaySeconds);
            case "sound":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for sound action.");
                    return null;
                }
                return new SoundActionStep(parameter);
            case "damage":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for damage action.");
                    return null;
                }
                double damageAmount;
                try {
                    damageAmount = Double.parseDouble(parameter);
                } catch (NumberFormatException e) {
                    DebugLogger.getInstance().error("Invalid number format for damage action: " + parameter);
                    return null;
                }
                return new DamageActionStep(damageAmount);
            case "particles":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for particles action.");
                    return null;
                }
                String[] params = parameter.split("\\s+");
                if (params.length == 2) {
                    String particleName = params[0];
                    int count;
                    try {
                        count = Integer.parseInt(params[1]);
                    } catch (NumberFormatException e) {
                        DebugLogger.getInstance().error("Invalid particle count: " + params[1]);
                        return null;
                    }
                    return new ParticleActionStep(particleName, count);
                } else {
                    DebugLogger.getInstance().error("Invalid parameters for particles action.");
                    return null;
                }
            case "reset_to_default_animation":
                return new ResetAnimationActionStep(mobManager);
            default:
                DebugLogger.getInstance().error("Unknown action command: " + command);
                return null;
        }
    }

    private String replacePlaceholders(String input, AnimationConfig animationConfig) {
        return input
                .replace("%idle_animation%", animationConfig.getIdleAnimation())
                .replace("%walk_animation%", animationConfig.getWalkAnimation())
                .replace("%attack_animation%", animationConfig.getAttackAnimation());
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