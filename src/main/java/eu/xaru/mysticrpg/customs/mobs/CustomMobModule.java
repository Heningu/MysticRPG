package eu.xaru.mysticrpg.customs.mobs;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.config.DynamicConfig;
import eu.xaru.mysticrpg.config.DynamicConfigManager;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.customs.mobs.actions.Action;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.customs.mobs.actions.Condition;
import eu.xaru.mysticrpg.customs.mobs.actions.conditions.DistanceCondition;
import eu.xaru.mysticrpg.customs.mobs.actions.steps.*;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.admin.MobGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

        mobManager = new MobManager(plugin, mobConfigurations, economyHelper);
        registerCommands();
        Bukkit.getPluginManager().registerEvents(this, plugin);

       // DebugLogger.getInstance().log(Level.INFO, "CustomMobModule initialized successfully.", 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(LevelModule.class, EconomyModule.class, PartyModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.LOW;
    }

    private void loadMobConfigurations() {
        File mobFolder = new File(plugin.getDataFolder(), "custom\\mobs");
        if (!mobFolder.exists() && !mobFolder.mkdirs()) {
            DebugLogger.getInstance().error("Failed to create mobs folder.");
            return;
        }

        File[] files = mobFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    DynamicConfig config = DynamicConfigManager.loadConfig(file);
                    if (config == null) {
                        DebugLogger.getInstance().error("Failed to load config for file: " + file.getName());
                        continue;
                    }

                    String mobId = config.getString("id", "-1");
                    String mobName = config.getString("name", "Unknown Mob");
                    if (mobId == null || mobId.isEmpty()) {
                        DebugLogger.getInstance().error("Mob ID is missing in file: " + file.getName());
                        continue;
                    }
                    if (mobName == null || mobName.isEmpty()) {
                        DebugLogger.getInstance().error("Mob name is missing in file: " + file.getName());
                        continue;
                    }
                    EntityType entityType = EntityType.valueOf(config.getString("type", "ZOMBIE").toUpperCase(Locale.ROOT));
                    double health = config.getDouble("health", 20.0);
                    int level = config.getInt("level", 1);

                    int experienceReward = config.getInt("experienceReward", 0);
                    int currencyReward = config.getInt("currencyReward", 0);

                    double baseDamage = config.getDouble("damage", 2.0);
                    double baseArmor = config.getDouble("armor", 0.0);
                    double movementSpeed = config.getDouble("movement_speed", 0.2);

                    // customAttributes -> key->int
                    Map<String, Integer> customAttributes = new HashMap<>();
                    Object customAttrObj = config.get("customAttributes");
                    if (customAttrObj instanceof Map<?,?> cMap) {
                        for (Map.Entry<?,?> e : cMap.entrySet()) {
                            String key = String.valueOf(e.getKey());
                            int val = parseInt(e.getValue(), 0);
                            customAttributes.put(key, val);
                        }
                    }

                    List<String> assignedAreas = config.getStringList("assigned_areas", new ArrayList<>());

                    // area_settings -> map of areaKey -> { max_amount, respawn_after_seconds, respawn_if_all_dead }
                    Map<String, CustomMob.AreaSettings> areaSettingsMap = new HashMap<>();
                    Object areaSetObj = config.get("area_settings");
                    if (areaSetObj instanceof Map<?,?> areaMap) {
                        for (Map.Entry<?,?> e : areaMap.entrySet()) {
                            String areaKey = String.valueOf(e.getKey());
                            Object areaVal = e.getValue();
                            if (areaVal instanceof Map<?,?> subMap) {
                                int maxAmount = parseInt(subMap.get("max_amount"), 1);
                                int respawnAfter = parseInt(subMap.get("respawn_after_seconds"), -1);
                                boolean respawnIfAllDead = parseBoolean(subMap.get("respawn_if_all_dead"), false);

                                CustomMob.AreaSettings areaSettings = new CustomMob.AreaSettings(maxAmount, respawnAfter, respawnIfAllDead);
                                areaSettingsMap.put(areaKey, areaSettings);
                            }
                        }
                    }

                    // drops -> list of maps
                    List<CustomMob.DropItem> drops = new ArrayList<>();
                    Object dropsObj = config.get("drops");
                    if (dropsObj instanceof List<?> rawList) {
                        for (Object dropItemObj : rawList) {
                            if (dropItemObj instanceof Map<?,?> dMap) {
                                String type = parseString(dMap.get("type"), null);
                                String id = parseString(dMap.get("id"), null);
                                String materialStr = parseString(dMap.get("material"), null);
                                int amount = parseInt(dMap.get("amount"), 1);
                                double chance = parseDouble(dMap.get("chance"), 1.0);

                                if (type == null) {
                                    DebugLogger.getInstance().error("Drop type is missing for mob: " + mobName);
                                    continue;
                                }
                                CustomMob.DropItem dropItem = new CustomMob.DropItem(type, id, materialStr, amount, chance);
                                drops.add(dropItem);
                            }
                        }
                    }

                    // equipment
                    CustomMob.Equipment equipment = null;
                    Object equipObj = config.get("equipment");
                    if (equipObj instanceof Map<?,?> eqMap) {
                        String helmet = parseString(getNested(eqMap, "helmet.custom_item_id"), null);
                        String chestplate = parseString(getNested(eqMap, "chestplate.custom_item_id"), null);
                        String leggings = parseString(getNested(eqMap, "leggings.custom_item_id"), null);
                        String boots = parseString(getNested(eqMap, "boots.custom_item_id"), null);
                        String weapon = parseString(getNested(eqMap, "weapon.custom_item_id"), null);

                        equipment = new CustomMob.Equipment(helmet, chestplate, leggings, boots, weapon);
                    }

                    // model_id
                    String modelId = config.getString("model_id", "INVALID_ID"+mobId);

                    // animations
                    AnimationConfig animationConfig = loadAnimations(config);

                    // actions
                    Map<String, List<Action>> actions = loadActions(config, animationConfig);

                    // boss bar
                    CustomMob.BossBarConfig bossBarConfig = null;
                    Object bossBarObj = config.get("BossBar");
                    if (bossBarObj instanceof Map<?,?> bossMap) {
                        boolean enabled = parseBoolean(bossMap.get("Enabled"), false);
                        String title = parseString(bossMap.get("Title"), mobName);
                        String colorName = parseString(bossMap.get("Color"), "RED");
                        double range = parseDouble(bossMap.get("Range"), 64.0);
                        String styleName = parseString(bossMap.get("Style"), "SEGMENTED_20");

                        BarColor color = BarColor.valueOf(colorName.toUpperCase(Locale.ROOT));
                        BarStyle style = BarStyle.valueOf(styleName.toUpperCase(Locale.ROOT));
                        // Append level to title
                        title = title + " [LVL " + level + "]";

                        bossBarConfig = new CustomMob.BossBarConfig(enabled, title, color, range, style);
                    }

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

    private AnimationConfig loadAnimations(DynamicConfig config) {
        AnimationConfig animationConfig = new AnimationConfig();
        Object animObj = config.get("animations");
        if (animObj instanceof Map<?,?> animationsMap) {
            // "idle", "walk", "attack"
            String idle = parseString(animationsMap.get("idle"), null);
            String walk = parseString(animationsMap.get("walk"), null);
            String attack = parseString(animationsMap.get("attack"), null);
            animationConfig.setIdleAnimation(idle);
            animationConfig.setWalkAnimation(walk);
            animationConfig.setAttackAnimation(attack);
        }
        return animationConfig;
    }

    private Map<String, List<Action>> loadActions(DynamicConfig config, AnimationConfig animationConfig) {
        Map<String, List<Action>> actions = new HashMap<>();
        List<String> actionEntries = config.getStringList("actions", new ArrayList<>());
        for (String actionEntry : actionEntries) {
            String[] parts = actionEntry.split("~");
            if (parts.length == 2) {
                String actionPath = parts[0].trim();
                String trigger = parts[1].trim();
                Action action = loadActionFromFile(actionPath, animationConfig);
                if (action != null) {
                    actions.computeIfAbsent(trigger, k -> new ArrayList<>()).add(action);
                }
            } else {
                DebugLogger.getInstance().error("Invalid action entry: " + actionEntry);
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

            DynamicConfig actionConfig = DynamicConfigManager.loadConfig(actionPath);
            if (actionConfig == null) {
                DebugLogger.getInstance().error("Failed to load action config: " + actionFile.getPath());
                return null;
            }

            int cooldown = actionConfig.getInt("cooldown", 0);
            List<String> targetConditionStrings = actionConfig.getStringList("target_conditions", new ArrayList<>());
            List<String> doActionsStrings = actionConfig.getStringList("do_actions", new ArrayList<>());

            // Parse target conditions
            List<Condition> targetConditions = new ArrayList<>();
            for (String conditionString : targetConditionStrings) {
                Condition condition = parseCondition(conditionString);
                if (condition != null) targetConditions.add(condition);
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
            default:
                DebugLogger.getInstance().error("Unknown condition type: " + conditionType);
                return null;
        }
    }

    private ActionStep parseActionStep(String actionString, AnimationConfig animationConfig) {
        String[] parts = actionString.split(":", 2);
        String command = parts[0].trim();
        String parameter = (parts.length > 1) ? parts[1].trim() : null;

        if (parameter != null) {
            parameter = replacePlaceholders(parameter, animationConfig);
        }

        switch (command.toLowerCase()) {
            case "animation":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for animation action.");
                    return null;
                }
                return new AnimationActionStep(parameter.replace("\"", ""));
            case "delay":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for delay action.");
                    return null;
                }
                double delaySeconds = parseDouble(parameter, 0.0);
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
                double damageAmount = parseDouble(parameter, 0.0);
                return new DamageActionStep(damageAmount);
            case "particles":
                if (parameter == null) {
                    DebugLogger.getInstance().error("Missing parameter for particles action.");
                    return null;
                }
                String[] params = parameter.split("\\s+");
                if (params.length == 2) {
                    String particleName = params[0];
                    int count = parseInt(params[1], 1);
                    return new ParticleActionStep(particleName, count);
                } else {
                    DebugLogger.getInstance().error("Invalid parameters for particles action: " + parameter);
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
                .replace("%idle_animation%", animationConfig.getIdleAnimation() == null ? "" : animationConfig.getIdleAnimation())
                .replace("%walk_animation%", animationConfig.getWalkAnimation() == null ? "" : animationConfig.getWalkAnimation())
                .replace("%attack_animation%", animationConfig.getAttackAnimation() == null ? "" : animationConfig.getAttackAnimation());
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
                        .withArguments(new StringArgument("mobId")
                                .replaceSuggestions(ArgumentSuggestions.strings(mobConfigurations.keySet().toArray(new String[0]))))
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
                            MobGUI mobGUI = new MobGUI();
                            mobGUI.openMobGUI(player);
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            loadMobConfigurations();
                            sender.sendMessage(Utils.getInstance().$("Mob configurations reloaded successfully."));
                        }))
                .register();
    }

    // Helper parsing methods
    private int parseInt(Object val, int fallback) {
        if (val instanceof Number) {
            return ((Number)val).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
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

    /**
     * A small helper to fetch nested keys like "helmet.custom_item_id" from a map,
     * returning the value or null if missing.
     */
    private Object getNested(Map<?,?> parent, String path) {
        String[] parts = path.split("\\.");
        Object cursor = parent;
        for (String part : parts) {
            if (cursor instanceof Map<?,?> m) {
                cursor = m.get(part);
            } else {
                return null;
            }
        }
        return cursor;
    }

    public MobManager getMobManager() {
        return mobManager;
    }
}
