package eu.xaru.mysticrpg.player.interaction;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.guis.player.InteractionGUI;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.player.interaction.trading.TradeListener;
import eu.xaru.mysticrpg.player.interaction.trading.TradeManager;
import eu.xaru.mysticrpg.player.interaction.trading.TradeRequestManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;
import java.util.logging.Level;

/**
 * InteractionModule handles player interactions related to trading.
 */
public class InteractionModule implements IBaseModule {


    private final EventManager eventManager = new EventManager(MysticCore.getPlugin(MysticCore.class));
    private TradeRequestManager tradeRequestManager;
    private TradeListener tradeListener;

    @Override
    public void initialize() {
      //  DebugLogger.getInstance().log(Level.INFO, "InteractionModule initialized", 0);
    }

    @Override
    public void start() {
        registerEvents();
        registerCommands();
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }


    public void registerEvents() {

        // Access the singleton TradeManager
        TradeManager tradeManager = TradeManager.getInstance();

        // Initialize TradeRequestManager
        tradeRequestManager = new TradeRequestManager(tradeManager);

        // Initialize TradeListener with TradeRequestManager
        tradeListener = new TradeListener(tradeRequestManager);

        eventManager.registerEvent(PlayerInteractEntityEvent.class, event -> {

            Player interactingPlayer = event.getPlayer();
            if (interactingPlayer.isSneaking()) {
                if (event.getRightClicked() instanceof Player targetPlayer) {
                    if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(targetPlayer)) {
                        // This is an NPC, not a real player, so we return and do nothing
                        return;
                    }
                    boolean isInCombat = false; // Template check for the future

                    if (!isInCombat) {
                        // Open the Interaction GUI
                        InteractionGUI interactionGUI = new InteractionGUI(tradeRequestManager);
                        interactionGUI.openInteractionGUI(interactingPlayer, targetPlayer);
                    }
                }
            }

        });
    }

    private void registerCommands() {
        // /trade_accept <initiatorName>
        new CommandAPICommand("trade_accept")
                .withArguments(new StringArgument("initiatorName"))
                .executesPlayer((player, args) -> {
                    String initiatorName = (String) args.get("initiatorName");
                    tradeRequestManager.acceptTradeRequest(initiatorName, player);
                })
                .register();

        // /trade_decline <initiatorName>
        new CommandAPICommand("trade_decline")
                .withArguments(new StringArgument("initiatorName"))
                .executesPlayer((player, args) -> {
                    String initiatorName = (String) args.get("initiatorName");
                    tradeRequestManager.declineTradeRequest(initiatorName, player);
                })
                .register();
    }
}
