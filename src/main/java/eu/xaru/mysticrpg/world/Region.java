package eu.xaru.mysticrpg.world;

import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a defined rectangular cuboid region in the world. A region can have its own flags,
 * potion effects (with strength), and a custom title/subtitle that show when players enter it.
 */
public class Region {

    private final String id;
    private Location pos1;
    private Location pos2;
    private final Map<String, Boolean> flags = new HashMap<>();
    // Now store effects as a map from PotionEffectType to amplifier
    private final Map<PotionEffectType, Integer> effects = new HashMap<>();
    private String title;
    private String subtitle;

    public Region(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

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

    public void setFlag(String flag, boolean value) {
        flags.put(flag.toLowerCase(), value);
    }

    public Boolean getFlag(String flag) {
        return flags.get(flag.toLowerCase());
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    /**
     * Adds or updates a potion effect with a given amplifier.
     */
    public void addEffect(PotionEffectType type, int amplifier) {
        effects.put(type, amplifier);
    }

    public void removeEffect(PotionEffectType type) {
        effects.remove(type);
    }

    public Map<PotionEffectType, Integer> getEffects() {
        return effects;
    }

    public void setTitle(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public void removeTitle() {
        this.title = null;
        this.subtitle = null;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
