package eu.xaru.mysticrpg.pets;

/**
 * Represents a pet configuration loaded from a .yml file.
 */
public class Pet {

    private final String id;
    private final String name;
    private final String displayItem;   // If you still want to store an icon for a pet menu
    private final String modelId;       // ModelEngine model ID
    private final String idleAnimation; // Animation name for idle
    private final String walkAnimation; // Animation name for walking

    public Pet(String id,
               String name,
               String displayItem,
               String modelId,
               String idleAnimation,
               String walkAnimation) {
        this.id = id;
        this.name = name;
        this.displayItem = displayItem;
        this.modelId = modelId;
        this.idleAnimation = idleAnimation;
        this.walkAnimation = walkAnimation;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayItem() {
        return displayItem;
    }

    public String getModelId() {
        return modelId;
    }

    public String getIdleAnimation() {
        return idleAnimation;
    }

    public String getWalkAnimation() {
        return walkAnimation;
    }
}
