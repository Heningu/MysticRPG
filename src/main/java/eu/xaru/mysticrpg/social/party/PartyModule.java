package eu.xaru.mysticrpg.social.party;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class PartyModule implements IBaseModule {

    private MysticCore plugin;
    private PartyHelper partyHelper;
    private EventManager eventManager;
    

    @Override
    public void initialize() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.eventManager = new EventManager(plugin);
        this.partyHelper = new PartyHelper();

        DebugLogger.getInstance().log(Level.INFO, "PartyModule initialized", 0);
    }

    @Override
    public void start() {
        DebugLogger.getInstance().log(Level.INFO, "PartyModule started", 0);

        registerCommands();
        registerEvents();
    }

    @Override
    public void stop() {
        DebugLogger.getInstance().log(Level.INFO, "PartyModule stopped", 0);
    }

    @Override
    public void unload() {
        DebugLogger.getInstance().log(Level.INFO, "PartyModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    public PartyHelper getPartyHelper() {
        return this.partyHelper;
    }

    private void registerCommands() {
        // /party invite <player>
        new CommandAPICommand("party")
                .withAliases("p")
                .withSubcommand(new CommandAPICommand("invite")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args.get("player");
                            partyHelper.invitePlayer(player, target);
                        }))
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            partyHelper.leaveParty(player);
                        }))
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            partyHelper.acceptInvitation(player);
                        }))
                .withSubcommand(new CommandAPICommand("decline")
                        .executesPlayer((player, args) -> {
                            partyHelper.declineInvitation(player);
                        }))
                .register();
    }

    private void registerEvents() {
        // Handle player disconnect
        eventManager.registerEvent(org.bukkit.event.player.PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            partyHelper.handlePlayerDisconnect(player);
        });

        // Handle entity death for XP sharing
        eventManager.registerEvent(EntityDeathEvent.class, event -> {
            LivingEntity entity = event.getEntity();
            Player killer = entity.getKiller();
            if (killer != null) {
                // XP sharing is handled in MobManager
            }
        });
    }
}
