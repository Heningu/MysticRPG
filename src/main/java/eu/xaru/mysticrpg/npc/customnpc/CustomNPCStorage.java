package eu.xaru.mysticrpg.npc.customnpc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomNPCStorage {

    public static List<CustomNPC> loadAllCustomNPCs() {
        List<CustomNPC> result = new ArrayList<>();

        File folder = new File(JavaPlugin.getProvidingPlugin(CustomNPCStorage.class)
                .getDataFolder(), "customnpcs");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return result;

        for (File file : files) {
            try {
                YamlConfiguration yml = new YamlConfiguration();
                yml.load(file);

                String npcId = file.getName().replace(".yml", "");
                String npcName = yml.getString("name", npcId);

                String worldName = yml.getString("location.world", "world");
                double x = yml.getDouble("location.x", 0.0);
                double y = yml.getDouble("location.y", 64.0);
                double z = yml.getDouble("location.z", 0.0);

                String modelId = yml.getString("modelId", "");

                World w = Bukkit.getWorld(worldName);
                if (w == null && !Bukkit.getWorlds().isEmpty()) {
                    w = Bukkit.getWorlds().get(0);
                }

                Location loc = new Location(w, x, y, z);

                CustomNPC npc = new CustomNPC(npcId, npcName, loc, modelId);
                result.add(npc);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
