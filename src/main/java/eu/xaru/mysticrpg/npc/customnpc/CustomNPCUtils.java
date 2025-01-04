package eu.xaru.mysticrpg.npc.customnpc;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Utility for deleting an NPC's .yml file from disk.
 */
public class CustomNPCUtils {

    public static void deleteNpcFile(String npcId) {
        File folder = new File(JavaPlugin.getProvidingPlugin(CustomNPCUtils.class)
                .getDataFolder(), "customnpcs");
        File file = new File(folder, npcId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
}
