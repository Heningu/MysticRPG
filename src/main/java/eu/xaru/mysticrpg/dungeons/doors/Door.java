package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Location;

/**
 * Represents a Door with a bounding box [bottomLeft => topRight]
 * and a trigger type (leftclick, rightclick, or none).
 */
public class Door {

    private final String doorId;
    private final Location bottomLeft;
    private final Location topRight;
    private String triggerType; // e.g. "leftclick", "rightclick", or "none"

    public Door(String doorId, Location bottomLeft, Location topRight) {
        this.doorId = doorId;
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.triggerType = "none";
    }

    public String getDoorId() {
        return doorId;
    }

    public Location getBottomLeft() {
        return bottomLeft;
    }

    public Location getTopRight() {
        return topRight;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Checks if a block location is inside this bounding box.
     */
    public boolean isWithinDoor(Location loc) {
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= bottomLeft.getX() && x <= topRight.getX()
                && y >= bottomLeft.getY() && y <= topRight.getY()
                && z >= bottomLeft.getZ() && z <= topRight.getZ();
    }
}
