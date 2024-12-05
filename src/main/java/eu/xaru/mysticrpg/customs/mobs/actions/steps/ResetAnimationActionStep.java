package eu.xaru.mysticrpg.customs.mobs.actions.steps;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;
import eu.xaru.mysticrpg.customs.mobs.MobManager;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;

public class ResetAnimationActionStep implements ActionStep {

    private final MobManager mobManager;

    public ResetAnimationActionStep(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public void execute(CustomMobInstance mobInstance) {
    }
}
