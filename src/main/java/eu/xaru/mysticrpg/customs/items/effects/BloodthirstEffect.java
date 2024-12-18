package eu.xaru.mysticrpg.customs.items.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class BloodthirstEffect implements Effect {
    @Override
    public String getName() {
        return "BLOODTHIRST";
    }

    @Override
    public void apply(EntityDamageByEntityEvent event, Player player) {
        double damage = event.getFinalDamage();
        double lifesteal = damage * 0.1;
        double newHealth = Math.min(player.getHealth() + lifesteal, player.getMaxHealth());
        player.setHealth(newHealth);
        player.sendMessage("Your bloodthirst restores some of your health!");
    }

    @Override
    public void apply(ItemStack itemStack, Player player) {
        // No immediate effect on application
    }
}
