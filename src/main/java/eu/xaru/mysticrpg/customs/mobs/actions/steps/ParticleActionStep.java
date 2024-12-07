package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class ParticleActionStep implements ActionStep {

    private final Particle particle;
    private final int count;

    public ParticleActionStep(String particleName, int count) {
        this.particle = Particle.valueOf(particleName.toUpperCase());
        this.count = count;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
        LivingEntity entity = mobInstance.getEntity();
        entity.getWorld().spawnParticle(particle, entity.getLocation(), count, 0.5, 1, 0.5, 0);
        DebugLogger.getInstance().log("Spawned particle '" + particle.name() + "' around mob.");
    }
}