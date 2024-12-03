// File: eu/xaru/mysticrpg/dungeons/doors/Door.java

package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Location;

public class Door {
    private final String doorId;
    private final Location bottomLeft;
    private final Location topRight;

    /**
     * Constructor for Door.
     *
     * @param doorId      Unique identifier for the door.
     * @param bottomLeft  The bottom-left corner of the door area.
     * @param topRight    The top-right corner of the door area.
     */
    public Door(String doorId, Location bottomLeft, Location topRight) {
        this.doorId = doorId;
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
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

    /**
     * Checks if a given location is within the door's area.
     *
     * @param location The location to check.
     * @return True if the location is within the door area, false otherwise.
     */
    public boolean isWithinDoor(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= bottomLeft.getX() && x <= topRight.getX()
                && y >= bottomLeft.getY() && y <= topRight.getY()
                && z >= bottomLeft.getZ() && z <= topRight.getZ();
    }
}
