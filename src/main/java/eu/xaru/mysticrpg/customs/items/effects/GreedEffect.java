package eu.xaru.mysticrpg.customs.items.effects;

import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.economy.EconomyModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class GreedEffect implements Effect {

    @Override
    public String getName() {
        return "GREED";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        // Demonstration logic if needed - currently handled elsewhere.
        // No immediate change needed here since the logic is now centralized in the manager.
    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on item application
    }

    @Override
    public double getGoldMultiplier() {
        return 1.10; // +10% gold
    }
}
