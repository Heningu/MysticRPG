package eu.xaru.mysticrpg.games;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Contains the random slot spin logic, checking wins, and handling payouts.
 */
public class SlotGameHelper {

    private final JavaPlugin plugin;
    private final EconomyHelper economyHelper;
    private final Random random = new Random();

    /**
     * Change these symbols to whatever you want in the slot machine reels.
     */
    private final Material[] slotItems = {
            Material.APPLE,
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.EMERALD,
            Material.IRON_INGOT
    };

    public SlotGameHelper(JavaPlugin plugin, EconomyHelper economyHelper) {
        this.plugin = plugin;
        this.economyHelper = economyHelper;
    }

    /**
     * Returns the plugin reference for scheduling tasks.
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the economy helper, so other classes can check or modify a player's gold.
     */
    public EconomyHelper getEconomy() {
        return economyHelper;
    }

    /**
     * Randomly picks 3 symbols from {@link #slotItems}.
     *
     * @return A List of 3 Materials
     */
    public List<Material> spinSlots() {
        List<Material> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Material randomSymbol = slotItems[random.nextInt(slotItems.length)];
            result.add(randomSymbol);
        }
        return result;
    }

    /**
     * Checks if the 3 final items are the same.
     */
    public boolean checkWin(List<Material> finalItems) {
        if (finalItems.size() < 3) return false;
        return finalItems.get(0) == finalItems.get(1)
                && finalItems.get(1) == finalItems.get(2);
    }

    /**
     * Pays out if the player wins, multiplied by 10.
     */
    public void payOut(Player player, int bet) {
        int currentBalance = economyHelper.getBankGold(player);
        economyHelper.setBankGold(player, currentBalance + (bet * 10));
    }
}
