package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class FieryEffect implements Effect {

    @Override
    public String getName() {
        return "FIERY";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        Entity target = event.getEntity();
        target.setFireTicks(60); // Sets the entity on fire for 3 seconds



    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // Not applicable for FieryEffect during item application
    }
}
