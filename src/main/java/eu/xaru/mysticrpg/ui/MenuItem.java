package eu.xaru.mysticrpg.ui;

import java.util.List;

public class MenuItem {
    private final String material;
    private final String name;
    private final List<String> description;
    private final boolean hideEnchants;
    private final int row;
    private final int column;

    public MenuItem(String material, String name, List<String> description, boolean hideEnchants, int row, int column) {
        this.material = material;
        this.name = name;
        this.description = description;
        this.hideEnchants = hideEnchants;
        this.row = row;
        this.column = column;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
