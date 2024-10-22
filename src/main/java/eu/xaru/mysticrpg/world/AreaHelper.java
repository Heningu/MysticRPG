package eu.xaru.mysticrpg.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import eu.xaru.mysticrpg.customs.mobs.CustomMob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class AreaHelper {

    private final List<SpawnArea> spawnAreas = new ArrayList<>();
    private final List<CustomMob> mobConfigurations = new ArrayList<>();
    private final Map<Player, Location[]> playerAreaCorners = new HashMap<>();
    private final Map<Player, String> playerModifyState = new HashMap<>();

    public void defineArea(String areaId, Location corner1, Location corner2) {
        SpawnArea newArea = new SpawnArea(areaId, corner1, corner2);
        spawnAreas.add(newArea);
        Bukkit.getLogger().log(Level.INFO, "Defined new spawn area: " + areaId);
    }

    public void removeArea(String areaId) {
        spawnAreas.removeIf(area -> area.getAreaId().equalsIgnoreCase(areaId));
        Bukkit.getLogger().log(Level.INFO, "Removed spawn area: " + areaId);
    }

    public void modifyArea(String areaId, Location corner1, Location corner2) {
        for (SpawnArea area : spawnAreas) {
            if (area.getAreaId().equalsIgnoreCase(areaId)) {
                area.setCorner1(corner1);
                area.setCorner2(corner2);
                Bukkit.getLogger().log(Level.INFO, "Modified spawn area: " + areaId);
                return;
            }
        }
    }

    public SpawnArea getAreaById(String areaId) {
        for (SpawnArea area : spawnAreas) {
            if (area.getAreaId().equalsIgnoreCase(areaId)) {
                return area;
            }
        }
        return null;
    }

    public List<SpawnArea> getAllAreas() {
        return new ArrayList<>(spawnAreas);
    }

    public List<CustomMob> getMobConfigurations() {
        return mobConfigurations;
    }

    public void addMobConfiguration(CustomMob customMob) {
        mobConfigurations.add(customMob);
    }

    public void removeMobConfiguration(String mobName) {
        mobConfigurations.removeIf(mob -> mob.getName().equalsIgnoreCase(mobName));
    }

    public void setPlayerAreaCorner(Player player, int cornerIndex, Location location) {
        if (!playerAreaCorners.containsKey(player)) {
            playerAreaCorners.put(player, new Location[2]);
        }
        Location[] corners = playerAreaCorners.get(player);
        corners[cornerIndex] = location;
        playerAreaCorners.put(player, corners);
    }

    public Location[] getPlayerAreaCorners(Player player) {
        return playerAreaCorners.get(player);
    }

    public void enterModifyState(Player player, String areaName) {
        playerModifyState.put(player, areaName);
    }

    public void exitModifyState(Player player) {
        playerModifyState.remove(player);
    }

    public boolean isInModifyState(Player player) {
        return playerModifyState.containsKey(player);
    }

    public String getModifyStateAreaName(Player player) {
        return playerModifyState.get(player);
    }

    public void removePlayerFromModifyState(Player player) {
        playerModifyState.remove(player);
        playerAreaCorners.remove(player);
    }

    public void setRule(SpawnArea area, String rule, boolean state) {
        area.setFlag(rule, state);
    }

    public List<String> getAvailableFlags() {
        return List.of("playersCanPVP", "mobGriefing", "chatAllowed", "commandsAllowed");
    }

    // Nested class representing the spawn area
    public class SpawnArea {
        private String areaId;
        private Location corner1;
        private Location corner2;
        private final Map<String, Boolean> flags;

        public SpawnArea(String areaId, Location corner1, Location corner2) {
            this.areaId = areaId;
            this.corner1 = corner1;
            this.corner2 = corner2;
            this.flags = new HashMap<>();

            // Initialize default flags
            flags.put("playersCanPVP", false);
            flags.put("mobGriefing", false);
            flags.put("chatAllowed", false);
            flags.put("commandsAllowed", false);
        }

        public String getAreaId() {
            return areaId;
        }

        public boolean isInArea(Location location) {
            double minX = Math.min(corner1.getX(), corner2.getX());
            double maxX = Math.max(corner1.getX(), corner2.getX());
            double minY = Math.min(corner1.getY(), corner2.getY());
            double maxY = Math.max(corner1.getY(), corner2.getY());
            double minZ = Math.min(corner1.getZ(), corner2.getZ());
            double maxZ = Math.max(corner1.getZ(), corner2.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        }

        public Location getRandomLocation(Random random) {
            double x = minX() + random.nextDouble() * (maxX() - minX());
            double y = minY() + random.nextDouble() * (maxY() - minY());
            double z = minZ() + random.nextDouble() * (maxZ() - minZ());
            return new Location(corner1.getWorld(), x, y, z);
        }

        public void setCorner1(Location corner1) {
            this.corner1 = corner1;
        }

        public void setCorner2(Location corner2) {
            this.corner2 = corner2;
        }

        public void setFlag(String flag, boolean state) {
            if (flags.containsKey(flag)) {
                flags.put(flag, state);
            } else {
                Bukkit.getLogger().log(Level.WARNING, "Unknown flag: " + flag);
            }
        }

        public boolean getFlag(String flag) {
            return flags.getOrDefault(flag, false);
        }

        public Map<String, Boolean> getFlags() {
            return flags;
        }

        private double minX() {
            return Math.min(corner1.getX(), corner2.getX());
        }

        private double maxX() {
            return Math.max(corner1.getX(), corner2.getX());
        }

        private double minY() {
            return Math.min(corner1.getY(), corner2.getY());
        }

        private double maxY() {
            return Math.max(corner1.getY(), corner2.getY());
        }

        private double minZ() {
            return Math.min(corner1.getZ(), corner2.getZ());
        }

        private double maxZ() {
            return Math.max(corner1.getZ(), corner2.getZ());
        }
    }
}
