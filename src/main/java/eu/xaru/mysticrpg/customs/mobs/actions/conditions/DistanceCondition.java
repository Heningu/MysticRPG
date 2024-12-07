package eu.xaru.mysticrpg.customs.mobs.actions.conditions;

import eu.xaru.mysticrpg.customs.mobs.actions.Condition;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class DistanceCondition implements Condition {

    private final String operator;
    private final double value;

    public DistanceCondition(String conditionParam) {
        // Trim any whitespace
        conditionParam = conditionParam.trim();

        // Possible operators
        String[] operators = {"<=", ">=", "!=", "==", "<", ">"};

        String op = null;
        int opIndex = -1;
        for (String possibleOp : operators) {
            opIndex = conditionParam.indexOf(possibleOp);
            if (opIndex != -1) {
                op = possibleOp;
                break;
            }
        }

        if (op == null) {
            throw new IllegalArgumentException("Invalid operator in distance condition: " + conditionParam);
        }

        String valueStr = conditionParam.substring(opIndex + op.length()).trim();
        operator = op;
        value = Double.parseDouble(valueStr);
    }


    @Override
    public boolean evaluate(LivingEntity mob, Entity target) {
        if (mob == null || target == null) {
            DebugLogger.getInstance().log("DistanceCondition: mob or target is null.");
            return false;
        }

        double distance = mob.getLocation().distance(target.getLocation());
        DebugLogger.getInstance().log("DistanceCondition: mob=" + mob.getName() + ", target=" + target.getName() + ", distance=" + distance);

        switch (operator) {
            case "<":
                return distance < value;
            case ">":
                return distance > value;
            case "<=":
                return distance <= value;
            case ">=":
                return distance >= value;
            case "==":
                return distance == value;
            case "!=":
                return distance != value;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "DistanceCondition{" + "operator='" + operator + '\'' + ", value=" + value + '}';
    }
}