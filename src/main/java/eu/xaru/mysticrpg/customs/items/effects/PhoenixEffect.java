package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class PhoenixEffect implements Effect {
    @Override
    public String getName() {
        return "Phoenix's Will";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {


    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on application
    }

    // Effect to check if player would die CURRENT_HP <= 0 then do 1 HP and dont take damage for 5 seconds (Cooldown until death)
}
