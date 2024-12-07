package eu.xaru.mysticrpg.customs.mobs.actions;

import eu.xaru.mysticrpg.customs.mobs.CustomMobInstance;

public interface ActionStep {
    void execute(CustomMobInstance mobInstance);
}