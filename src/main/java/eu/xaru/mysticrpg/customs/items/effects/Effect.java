package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public interface Effect {
    String getName();

    void apply(EntityDamageByEntityEvent event, Player player);

    void apply(ItemStack itemStack, Player player);
}
