package eu.xaru.mysticrpg.player.titles;

import eu.xaru.mysticrpg.player.stats.StatType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a single title with a name and optional stat bonuses.
 */
public class PlayerTitle {

    private final String name;
    private final Map<StatType, Integer> statBonuses;

    /**
     * @param name The display name of the title.
     * @param statBonuses A map of StatType to integer bonuses that this title provides.
     */
    public PlayerTitle(String name, Map<StatType, Integer> statBonuses) {
        this.name = name;
        this.statBonuses = Collections.unmodifiableMap(statBonuses);
    }

    public String getName() {
        return name;
    }

    public Map<StatType, Integer> getStatBonuses() {
        return statBonuses;
    }
}
