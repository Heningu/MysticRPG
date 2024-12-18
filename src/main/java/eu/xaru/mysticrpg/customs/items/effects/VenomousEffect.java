package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VenomousEffect implements Effect {
    @Override
    public String getName() {
        return "VENOMOUS";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0)); // 3s poison
            player.sendMessage("Your venom weakens your foe!");
        }
    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on application
    }
}
