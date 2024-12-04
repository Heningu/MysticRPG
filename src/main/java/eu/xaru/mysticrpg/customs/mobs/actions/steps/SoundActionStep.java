package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import org.bukkit.Sound;

public class SoundActionStep implements ActionStep {

    private final String soundName;

    public SoundActionStep(String soundName) {
        this.soundName = soundName;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
        mobInstance.getEntity().getWorld().playSound(mobInstance.getEntity().getLocation(), soundName, 1.0f, 1.0f);
    }
}
