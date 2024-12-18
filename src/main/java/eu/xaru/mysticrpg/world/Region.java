package eu.xaru.mysticrpg.world;

import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Represents a defined rectangular cuboid region in the world. A region can have its own flags,
 * potion effects, and a custom title/subtitle that show when players enter it.
 */
public class Region {

    private final String id;
    private Location pos1;
    private Location pos2;
    private final Map<String, Boolean> flags = new HashMap<>();
    private final Set<PotionEffectType> effects = new HashSet<>();
    private String title;
    private String subtitle;

    /**
     * Constructs a new Region with a given unique ID.
     *
     * @param id the unique identifier for this region
     */
    public Region(String id) {
        this.id = id;
    }

    /**
     * @return the unique ID of this region
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the first corner of the region.
     *
     * @param pos1 the first corner Location
     */
    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    /**
     * Sets the second corner of the region.
     *
     * @param pos2 the second corner Location
     */
    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    /**
     * @return the first corner of the region
     */
    public Location getPos1() {
        return pos1;
    }

    /**
     * @return the second corner of the region
     */
    public Location getPos2() {
        return pos2;
    }

    /**
     * Checks if a given location is inside this region.
     *
     * @param loc the location to check
     * @return true if inside, false otherwise
     */
    public boolean contains(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int y2 = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return loc.getBlockX() >= x1 && loc.getBlockX() <= x2
                && loc.getBlockY() >= y1 && loc.getBlockY() <= y2
                && loc.getBlockZ() >= z1 && loc.getBlockZ() <= z2;
    }

    /**
     * Sets a flag to true/false for this region.
     *
     * @param flag  the flag name
     * @param value true or false
     */
    public void setFlag(String flag, boolean value) {
        flags.put(flag.toLowerCase(), value);
    }

    /**
     * Retrieves the value of a flag for this region.
     *
     * @param flag the flag name
     * @return the Boolean value if set, or null if not defined
     */
    public Boolean getFlag(String flag) {
        return flags.get(flag.toLowerCase());
    }

    /**
     * Adds a potion effect that applies to players entering this region.
     *
     * @param type the potion effect type to add
     */
    public void addEffect(PotionEffectType type) {
        effects.add(type);
    }

    /**
     * Removes a potion effect from this region.
     *
     * @param type the potion effect type to remove
     */
    public void removeEffect(PotionEffectType type) {
        effects.remove(type);
    }

    /**
     * @return the set of potion effects applied to players in this region
     */
    public Set<PotionEffectType> getEffects() {
        return effects;
    }

    /**
     * Sets an enter-title and subtitle for this region.
     *
     * @param title    the main title
     * @param subtitle the subtitle
     */
    public void setTitle(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    /**
     * Removes the title from the region.
     */
    public void removeTitle() {
        this.title = null;
        this.subtitle = null;
    }

    /**
     * @return the title for this region, or null if none
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the subtitle for this region, or null if none
     */
    public String getSubtitle() {
        return subtitle;
    }
}
