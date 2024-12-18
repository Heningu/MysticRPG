package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class QuickStrikeEffect implements Effect {
    @Override
    public String getName() {
        return "QUICKSTRIKE";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1)); // 3s speed II
        player.sendMessage("Your quick strike grants you a burst of speed!");
    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on item application
    }
}
