package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class RegenerateEffect implements Effect {

    @Override
    public String getName() {
        return "Shaman's Lifeforce";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {


    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on application
    }

    // always regenerate 2 HP each second (scaling)

}
