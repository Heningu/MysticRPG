package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CustomMobDamageHandler implements Listener {

    private final MobManager mobManager;

    public CustomMobDamageHandler(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity attacker && event.getEntity() instanceof Player player) {
            CustomMobInstance mobInstance = mobManager.findMobInstance(attacker);
            if (mobInstance != null) {
                // Get the total attack damage from the attacker's attributes
                double totalDamage = getTotalAttackDamage(attacker);

                // Apply damage to the player
                event.setDamage(totalDamage);

                // Implement custom HP handling for the player if needed
            }
        }
    }

    private double getTotalAttackDamage(LivingEntity entity) {
        AttributeInstance attackAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            return attackAttribute.getValue();
        }
        return 0.0;
    }
}
