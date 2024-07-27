package eu.xaru.mysticrpg.content.menus;

import org.bukkit.entity.Player;

import java.util.Map;

public class MenuItem {
    private String material;
    private String name;
    private boolean hideEnchants;
    private String description;
    private int row;
    private int column;
    private String command;
    private Map<String, Integer> enchants;

    // Getters and setters
    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public void setHideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Integer> getEnchantments() {
        return enchants;
    }

    public void setEnchantments(Map<String, Integer> enchants) {
        this.enchants = enchants;
    }

    public void runCommand(Player player) {
        if (command != null && !command.isEmpty()) {
            player.performCommand(command);
        }
    }
}
