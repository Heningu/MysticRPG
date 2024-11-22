package eu.xaru.mysticrpg.pets;

public class Pet {
    private final String id;
    private final String name;
    private final String displayItem;

    public Pet(String id, String name, String displayItem) {
        this.id = id;
        this.name = name;
        this.displayItem = displayItem;
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
}
