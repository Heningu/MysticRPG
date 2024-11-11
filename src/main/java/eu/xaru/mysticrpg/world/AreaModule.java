package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class AreaModule implements IBaseModule {

    private final AreaHelper areaHelper = new AreaHelper();
    private DebugLoggerModule logger;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            throw new IllegalStateException("DebugLoggerModule not initialized. AreaModule cannot function without it.");
        }
        registerAreaCommand();
        logger.log(Level.INFO, "AreaModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "AreaModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "AreaModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "AreaModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void registerAreaCommand() {
        new CommandAPICommand("area")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("name"))
                        .executesPlayer((player, args) -> {
                            String areaName = (String) args.get("name");
                            if (areaHelper.getAreaById(areaName) != null) {
                                player.sendMessage(Utils.getInstance().$("§cError: An area with the name '" + areaName + "' already exists."));
                                return;
                            }
                            areaHelper.setPlayerAreaCorner(player, 0, null); // Clear any previous state
                            areaHelper.setPlayerAreaCorner(player, 1, null);
                            areaHelper.enterModifyState(player, areaName);
                            player.sendMessage(Utils.getInstance().$("§aUse §b/area set 1 §aand §b/area set 2 §ato set corners for area: §e" + areaName));
                        }))
                .withSubcommand(new CommandAPICommand("set")
                        .withArguments(new StringArgument("corner"))
                        .executesPlayer((player, args) -> {
                            String corner = (String) args.get("corner");
                            if (!areaHelper.isInModifyState(player)) {
                                player.sendMessage(Utils.getInstance().$("§cYou must create an area first using §b/area create <name>§c."));
                                return;
                            }
                            Location currentLocation = player.getLocation();
                            Location[] corners = areaHelper.getPlayerAreaCorners(player);
                            if (corners[0] != null && corners[1] != null) {
                                player.sendMessage(Utils.getInstance().$("§cBoth corners are already set. Use §b/area clear §cto reset them first."));
                                return;
                            }
                            if (corner.equals("1")) {
                                areaHelper.setPlayerAreaCorner(player, 0, currentLocation);
                                player.sendMessage(Utils.getInstance().$("§aFirst corner set at your current location."));
                            } else if (corner.equals("2")) {
                                areaHelper.setPlayerAreaCorner(player, 1, currentLocation);
                                player.sendMessage(Utils.getInstance().$("§aSecond corner set at your current location."));

                                // Automatically save the area after setting the second corner
                                String areaName = areaHelper.getModifyStateAreaName(player);
                                if (corners[0] != null && corners[1] != null) {
                                    areaHelper.defineArea(areaName, corners[0], corners[1]);
                                    areaHelper.exitModifyState(player);
                                    player.sendMessage(Utils.getInstance().$("§aArea §e" + areaName + " §ahas been successfully saved."));
                                }
                            } else {
                                player.sendMessage(Utils.getInstance().$("§cInvalid corner. Use '1' or '2'."));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("modify")
                        .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> areaHelper.getAllAreas().stream().map(AreaHelper.SpawnArea::getAreaId).toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String areaName = (String) args.get("name");
                            if (areaHelper.getAreaById(areaName) == null) {
                                player.sendMessage(Utils.getInstance().$("§cArea not found: §e" + areaName));
                                return;
                            }
                            areaHelper.enterModifyState(player, areaName);
                            player.sendMessage(Utils.getInstance().$("§aYou are now modifying area: §e" + areaName + "§a. Use §b/area clear §ato reset corners, then §b/area set 1 §aand §b/area set 2 §ato define new corners. Use §b/area save §ato save changes."));
                        }))
                .withSubcommand(new CommandAPICommand("clear")
                        .executesPlayer((player, args) -> {
                            if (!areaHelper.isInModifyState(player)) {
                                player.sendMessage(Utils.getInstance().$("§cYou must be in modify mode to clear an area's corners."));
                                return;
                            }
                            areaHelper.setPlayerAreaCorner(player, 0, null);
                            areaHelper.setPlayerAreaCorner(player, 1, null);
                            player.sendMessage(Utils.getInstance().$("§aCleared the corners for the area. Use §b/area set 1 §aand §b/area set 2 §ato set new corners."));
                        }))
                .withSubcommand(new CommandAPICommand("save")
                        .executesPlayer((player, args) -> {
                            if (!areaHelper.isInModifyState(player)) {
                                player.sendMessage(Utils.getInstance().$("§cYou must be in modify mode to save an area."));
                                return;
                            }
                            String areaName = areaHelper.getModifyStateAreaName(player);
                            Location[] corners = areaHelper.getPlayerAreaCorners(player);
                            if (corners[0] == null || corners[1] == null) {
                                player.sendMessage(Utils.getInstance().$("§cYou must set both corners before saving the area."));
                                return;
                            }
                            areaHelper.modifyArea(areaName, corners[0], corners[1]);
                            areaHelper.exitModifyState(player);
                            player.sendMessage(Utils.getInstance().$("§aArea §e" + areaName + " §ahas been successfully modified and saved."));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            List<AreaHelper.SpawnArea> areas = areaHelper.getAllAreas();
                            if (areas.isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("§cNo areas have been defined."));
                            } else {
                                player.sendMessage(Utils.getInstance().$("§aDefined Areas:"));
                                for (AreaHelper.SpawnArea area : areas) {
                                    player.sendMessage(Utils.getInstance().$("§e- " + area.getAreaId()));
                                }
                            }
                        }))
                .withSubcommand(new CommandAPICommand("rule")
                        .withArguments(new MultiLiteralArgument("list", "set"))
                        .withArguments(new StringArgument("rule").replaceSuggestions(ArgumentSuggestions.strings(info -> areaHelper.getAvailableFlags().toArray(new String[0]))))
                        .withArguments(new StringArgument("state").replaceSuggestions(ArgumentSuggestions.strings("true", "false")))
                        .executesPlayer((player, args) -> {
                            String action = (String) args.get(0);
                            if (action.equalsIgnoreCase("list")) {
                                if (!areaHelper.isInModifyState(player)) {
                                    player.sendMessage(Utils.getInstance().$("§cYou must be in modify mode to list area rules."));
                                    return;
                                }
                                String areaName = areaHelper.getModifyStateAreaName(player);
                                AreaHelper.SpawnArea area = areaHelper.getAreaById(areaName);
                                player.sendMessage(Utils.getInstance().$("§aRules for area §e" + areaName + "§a:"));
                                for (Map.Entry<String, Boolean> entry : area.getFlags().entrySet()) {
                                    player.sendMessage(Utils.getInstance().$("§e- " + entry.getKey() + ": §b" + entry.getValue()));
                                }
                            } else if (action.equalsIgnoreCase("set")) {
                                if (!areaHelper.isInModifyState(player)) {
                                    player.sendMessage(Utils.getInstance().$("§cYou must be in modify mode to edit area rules."));
                                    return;
                                }
                                String rule = (String) args.get(1);
                                String stateStr = (String) args.get(2);
                                boolean state = Boolean.parseBoolean(stateStr);
                                String areaName = areaHelper.getModifyStateAreaName(player);
                                AreaHelper.SpawnArea area = areaHelper.getAreaById(areaName);
                                if (area == null) {
                                    player.sendMessage(Utils.getInstance().$("§cArea not found: §e" + areaName));
                                    return;
                                }
                                areaHelper.setRule(area, rule, state);
                                player.sendMessage(Utils.getInstance().$("§aRule §e" + rule + " §ahas been set to §b" + state + " §afor area §e" + areaName));
                            }
                        }))
                .register();
    }
}