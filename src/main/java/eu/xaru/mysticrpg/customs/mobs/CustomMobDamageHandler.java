package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Only if you want special logic for when your custom mob hits a player.
 * This is optional.
 */
public class CustomMobDamageHandler implements Listener {

    private final MobManager mobManager;

    public CustomMobDamageHandler(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onMobAttackPlayer(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim
                && event.getDamager() instanceof LivingEntity attacker) {

            CustomMobInstance inst = mobManager.findMobInstance(attacker);
            if (inst != null) {
                // example: let's do a custom damage approach
                // set event to 0 or do your own approach
                event.setCancelled(true);

                double customDamage = inst.getCustomMob().getBaseDamage();
                // Then do: victim.damage(customDamage, attacker);
                victim.damage(customDamage, attacker);
            }
        }
    }
}
