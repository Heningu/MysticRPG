package eu.xaru.mysticrpg.customs.items;

import org.bukkit.attribute.AttributeModifier;

public class AttributeData {
    private final double value;
    private final AttributeModifier.Operation operation;

    public AttributeData(double value, AttributeModifier.Operation operation) {
        this.value = value;
        this.operation = operation;
    }

    public double getValue() {
        return value;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }
}
