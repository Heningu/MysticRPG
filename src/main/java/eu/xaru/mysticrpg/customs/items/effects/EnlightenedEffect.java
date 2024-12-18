package eu.xaru.mysticrpg.customs.items.effects;

import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EnlightenedEffect implements Effect {

    @Override
    public String getName() {
        return "ENLIGHTENED";
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
    public double getXpMultiplier() {
        return 1.05; // +5% xp
    }
}
