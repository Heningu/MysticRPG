package eu.xaru.mysticrpg.world;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

/**
 * Handles administrative commands for managing game modes and fly speed.
 */
public class AdminCMDModule implements IBaseModule {

    private DebugLoggerModule logger;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        if (logger == null) {
            throw new IllegalStateException("DebugLoggerModule not initialized. AdminCMDModule cannot function without it.");
        }
        registerCommands();
        logger.log(Level.INFO, "AdminCMDModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "AdminCMDModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "AdminCMDModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "AdminCMDModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Registers administrative commands for changing game modes and adjusting fly speed.
     */
    private void registerCommands() {
        // Command to change the player's game mode
        new CommandAPICommand("gm")
                .withPermission("mysticrpg.admincmd.gm")
                .withArguments(new IntegerArgument("mode", 0, 2))
                .executesPlayer((player, args) -> {
                    int mode = (int) args.get(0);
                    switch (mode) {
                        case 0 -> player.setGameMode(GameMode.SURVIVAL);
                        case 1 -> player.setGameMode(GameMode.CREATIVE);
                        case 2 -> player.setGameMode(GameMode.SPECTATOR);
                        default -> player.sendMessage("§cInvalid game mode. Use 0 (Survival), 1 (Creative), or 2 (Spectator).");
                    }
                    player.sendMessage("§aGame mode set to " + player.getGameMode().name());
                })
                .register();

        // Command to adjust the player's fly speed
        new CommandAPICommand("flyspeed")
                .withPermission("mysticrpg.admincmd.flyspeed")
                .withArguments(new IntegerArgument("speed", 1, 10))
                .executesPlayer((player, args) -> {
                    int speed = (int) args.get(0);
                    float flySpeed = Math.min(speed / 10.0f, 10.0f); // Cap speed at maximum 10.0
                    player.setFlySpeed(flySpeed);
                    if (speed == 1) {
                        player.sendMessage("Fly speed set to default");
                    } else {
                        player.sendMessage("§aFly speed set to " + speed);
                    }
                })
                .register();
    }
}
