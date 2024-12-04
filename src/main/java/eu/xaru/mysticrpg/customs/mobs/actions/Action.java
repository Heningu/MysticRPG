package eu.xaru.mysticrpg.customs.mobs.actions;
import eu.xaru.mysticrpg.customs.mobs.actions.Condition;


import java.util.List;

public class Action {

    private final double cooldown; // in seconds
    private final List<Condition> targetConditions;
    private final List<ActionStep> steps;

    private long lastExecutionTime; // in milliseconds

    public Action(double cooldown, List<Condition> targetConditions, List<ActionStep> steps) {
        this.cooldown = cooldown;
        this.targetConditions = targetConditions;
        this.steps = steps;
        this.lastExecutionTime = 0;
    }

    // Getters and Setters
    public double getCooldown() {
        return cooldown;
    }

    public List<Condition> getTargetConditions() {
        return targetConditions;
    }

    public List<ActionStep> getSteps() {
        return steps;
    }

    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(long lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }
}
