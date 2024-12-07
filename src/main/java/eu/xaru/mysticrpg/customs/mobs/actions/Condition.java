package eu.xaru.mysticrpg.customs.mobs.actions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public interface Condition {
    boolean evaluate(LivingEntity mob, Entity target);
}