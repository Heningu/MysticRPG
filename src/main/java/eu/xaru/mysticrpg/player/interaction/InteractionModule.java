package eu.xaru.mysticrpg.player.interaction;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.player.InteractionGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class InteractionModule implements IBaseModule {


    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));


    @Override
    public void initialize() {

        DebugLogger.getInstance().log(Level.INFO, "InteractionModule initialized", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "InteractionModule started", 0);
        registerEvents();
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "InteractionModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "InteractionModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }


    public void registerEvents(){

        eventManager.registerEvent(PlayerInteractEntityEvent.class, event -> {

            Player interactingPlayer = event.getPlayer();
            if (interactingPlayer.isSneaking()) {
                if (event.getRightClicked() instanceof Player targetPlayer) {
                    boolean isInCombat = false; //Template check for the future

                    if (!isInCombat) {
                        // Optionally, you can pass the targetPlayer to the GUI if needed
                        InteractionGUI interactionGUI = new InteractionGUI();
                        interactionGUI.openInteractionGUI(interactingPlayer, targetPlayer);
                    }
                }
            }

        });


    }


}
