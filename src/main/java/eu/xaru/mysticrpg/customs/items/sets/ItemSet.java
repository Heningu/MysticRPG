package eu.xaru.mysticrpg.customs.items.sets;

import eu.xaru.mysticrpg.player.stats.StatType;
import java.util.Map;

public class ItemSet {
    private final String id;
    // Map of "numberOfPieces" -> Map<StatType, Double multiplier>
    private final Map<Integer, Map<StatType, Double>> pieceBonuses;

    public ItemSet(String id, Map<Integer, Map<StatType, Double>> pieceBonuses) {
        this.id = id;
        this.pieceBonuses = pieceBonuses;
    }

    public String getId() {
        return id;
    }

    public Map<Integer, Map<StatType, Double>> getPieceBonuses() {
        return pieceBonuses;
    }
}
