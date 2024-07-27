package eu.xaru.mysticrpg.content.classes;

import org.bukkit.Material;
import org.json.simple.JSONObject;

public class PlayerClass {
    private final String className;
    private final String description;
    private final Material material;

    public PlayerClass(JSONObject classData) {
        this.className = (String) classData.get("class_name");
        this.description = (String) classData.get("description");
        this.material = Material.valueOf((String) classData.get("material"));
    }

    public String getClassName() {
        return className;
    }

    public String getDescription() {
        return description;
    }

    public Material getMaterial() {
        return material;
    }
}
