package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DeconstructEffect implements Effect {

    @Override
    public String getName() {
        return "DECONSTRUCT";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        // Not applicable for DeconstructEffect during combat
    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // Deconstruct logic will be handled elsewhere
    }
}
