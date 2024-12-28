package eu.xaru.mysticrpg.games;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages the Slot Machine "game" command using CommandAPI.
 */
public class SlotGameManager {

    private final JavaPlugin plugin;
    private final EconomyHelper economyHelper;
    private final SlotGameHelper slotGameHelper;

    public SlotGameManager(EconomyHelper economyHelper) {
        // If you rely on MysticCore as your main plugin, do:
        this.plugin = MysticCore.getPlugin(MysticCore.class);

        this.economyHelper = economyHelper;
        this.slotGameHelper = new SlotGameHelper(plugin, economyHelper);

        // Register the /game command via CommandAPI
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("game")
                // You can add permission requirements here if you wish
                .executesPlayer((player, args) -> {
                    // Open the slot machine GUI for the player
                    SlotGameGUI gui = new SlotGameGUI(slotGameHelper);
                    gui.openSlotGUI(player);
                })
                .register();
    }
}
