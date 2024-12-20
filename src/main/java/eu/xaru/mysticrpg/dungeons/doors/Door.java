package eu.xaru.mysticrpg.dungeons.doors;

import org.bukkit.Location;

public class Door {
    private final String doorId;
    private final Location bottomLeft;
    private final Location topRight;
    private String triggerType; // e.g. "leftclick" or "rightclick"

    public Door(String doorId, Location bottomLeft, Location topRight) {
        this.doorId = doorId;
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.triggerType = "none"; // default no trigger
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

    public boolean isWithinDoor(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= bottomLeft.getX() && x <= topRight.getX()
                && y >= bottomLeft.getY() && y <= topRight.getY()
                && z >= bottomLeft.getZ() && z <= topRight.getZ();
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
}
