package eu.xaru.mysticrpg.customs.items;

public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;


    public static Rarity getRarity(String rarity) {
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(rarity)) {
                return r;
            }
        }
        return null;
    }

    public static Rarity getRarity(int rarity) {
        for (Rarity r : values()) {
            if (r.ordinal() == rarity) {
                return r;
            }
        }
        return null;
    }

    public String getColor() {
        return switch (this) {
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
            case MYTHIC -> "§d";
            default -> "§f";
        };
    }

}
