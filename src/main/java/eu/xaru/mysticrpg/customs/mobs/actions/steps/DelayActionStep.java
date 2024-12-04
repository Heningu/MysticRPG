package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;

public class DelayActionStep implements ActionStep {

    private final double delaySeconds;

    public DelayActionStep(double delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
        // The delay is handled in the MobManager's executeActionSteps method
    }

    public double getDelaySeconds() {
        return delaySeconds;
    }
}
