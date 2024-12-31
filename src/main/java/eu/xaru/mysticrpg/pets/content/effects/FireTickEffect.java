package eu.xaru.mysticrpg.pets.content.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * "FireTick" effect.
 * Scales fire-ticks based on pet's level:
 *  - level 1 => 2 seconds (40 ticks)
 *  - level 5 => 4 seconds (80 ticks)
 *  - level 10 => 5 seconds (100 ticks)
 * Blocks reapplication if target is already on fire.
 */
public class FireTickEffect implements IPetEffect {

    @Override
    public String getId() {
        return "firetick";
    }

    @Override
    public String getDescription() {
        return "Sets targets on fire based on pet level.";
    }

    @Override
    public void apply(int petLevel, EntityDamageByEntityEvent event, Player attacker) {
        Entity target = event.getEntity();
        if (target.getFireTicks() > 0) {
            // Already burning => skip
            return;
        }
        int ticks = 40; // 2s by default
        if (petLevel >= 5 && petLevel < 10) {
            ticks = 80; // 4s
        } else if (petLevel >= 10) {
            ticks = 100; // 5s
        }
        target.setFireTicks(ticks);
    }
}
