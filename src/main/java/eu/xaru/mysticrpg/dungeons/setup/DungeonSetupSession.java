// File: eu/xaru/mysticrpg/dungeons/setup/DungeonSetupSession.java

package eu.xaru.mysticrpg.dungeons.setup;

import eu.xaru.mysticrpg.dungeons.config.DungeonConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DungeonSetupSession {

    private final Player player;
    private final DungeonConfig config;
    private boolean isSettingPortal;
    private Location portalPos1;

    public DungeonSetupSession(Player player, String dungeonId) {
        this.player = player;
        this.config = new DungeonConfig();
        this.config.setId(dungeonId);
    }

    public Player getPlayer() {
        return player;
    }

    public DungeonConfig getConfig() {
        return config;
    }

    public void setSpawnLocation(Location location) {
        config.setSpawnLocation(location);
        player.sendMessage("Dungeon spawn location set.");
    }

    public void addMobSpawnPoint(String mobId, Location location) {
        DungeonConfig.MobSpawnPoint spawnPoint = new DungeonConfig.MobSpawnPoint();
        spawnPoint.setMobId(mobId);
        spawnPoint.setLocation(location);
        config.getMobSpawnPoints().add(spawnPoint);
        player.sendMessage("Mob spawn point added.");
    }

    public void addChestLocation(String chestType, Location location) {
        DungeonConfig.ChestLocation chestLocation = new DungeonConfig.ChestLocation();
        chestLocation.setLocation(location);

        // Attempt to match the material
        Material material = Material.matchMaterial(chestType.toUpperCase());
        if (material == null) {
            // If material is null, default to CHEST and notify the player
            material = Material.CHEST;
            player.sendMessage("Invalid chest type provided. Defaulting to CHEST.");
        }
        chestLocation.setType(material);

        // Assign loot table ID based on chest type
        String lootTableId;
        if (material == Material.CHEST) {
            lootTableId = "default_loot";
        } else if (material == Material.TRAPPED_CHEST) {
            lootTableId = "elite_loot";
        } else {
            lootTableId = "default_loot";
        }
        chestLocation.setLootTableId(lootTableId);

        config.getChestLocations().add(chestLocation);

        // Inform the player about the added chest
        if (material == Material.CHEST && !chestType.equalsIgnoreCase("CHEST")) {
            player.sendMessage("Invalid chest type '" + chestType + "'. Defaulted to CHEST.");
        } else {
            player.sendMessage("Chest of type '" + material.toString() + "' added with loot table: " + lootTableId);
        }
    }

    // Portal Setup Methods
    public void startPortalSetup() {
        isSettingPortal = true;
        portalPos1 = null;
        player.sendMessage("Portal setup started. Please click on the portal location.");
    }

    public boolean isSettingPortal() {
        return isSettingPortal;
    }

    public void setPortalPos1(Location location) {
        portalPos1 = location;
        config.setPortalPos1(location);
        isSettingPortal = false;
        player.sendMessage("Portal position set at: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        player.sendMessage("Portal setup complete. Remember to save the dungeon configuration.");
    }

    public Location getPortalPos1() {
        return portalPos1;
    }
}
