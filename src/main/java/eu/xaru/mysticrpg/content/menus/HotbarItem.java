package eu.xaru.mysticrpg.content.menus;

import java.util.Map;

public class HotbarItem {
    private boolean use;
    private String name;
    private String itemDescription;
    private int hotbarSlot;
    private String material;
    private Map<String, Integer> enchantments;
    private boolean hideEnchants;
    private boolean itemDropable;
    private boolean itemMoveable;
    private boolean keepItemOnDeath;
    private String command;

    // Getters and setters
    public boolean isUse() {
        return use;
    }

    public void setUse(boolean use) {
        this.use = use;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public int getHotbarSlot() {
        return hotbarSlot;
    }

    public void setHotbarSlot(int hotbarSlot) {
        this.hotbarSlot = hotbarSlot;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public void setHideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
    }

    public boolean isItemDropable() {
        return itemDropable;
    }

    public void setItemDropable(boolean itemDropable) {
        this.itemDropable = itemDropable;
    }

    public boolean isItemMoveable() {
        return itemMoveable;
    }

    public void setItemMoveable(boolean itemMoveable) {
        this.itemMoveable = itemMoveable;
    }

    public boolean isKeepItemOnDeath() {
        return keepItemOnDeath;
    }

    public void setKeepItemOnDeath(boolean keepItemOnDeath) {
        this.keepItemOnDeath = keepItemOnDeath;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
