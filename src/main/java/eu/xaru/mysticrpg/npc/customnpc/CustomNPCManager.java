package eu.xaru.mysticrpg.npc.customnpc;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all CustomNPC objects in memory (ID -> CustomNPC).
 * On startup, we call loadAllFromDisk() to read them from customnpcs/<id>.yml.
 * Then the CustomNPCModule calls npc.spawn() for each, so they appear in-game.
 */
public class CustomNPCManager {

    private final Map<String, CustomNPC> npcMap = new HashMap<>();

    /**
     * Creates a new CustomNPC, spawns it in-game via npc.spawn(),
     * and saves its .yml. This is used for /xarunpc create ...
     */
    public CustomNPC createNPC(String id, String name, Location loc, String modelId) {
        CustomNPC npc = new CustomNPC(id, name, loc, modelId);
        npcMap.put(id, npc);

        // Spawns the physical stands (via EntityHandler)
        npc.spawn();
        npc.save();

        return npc;
    }

    /**
     * Deletes the NPC from memory and from the world, removing the .yml file as well.
     */
    public boolean deleteNPC(String id) {
        CustomNPC npc = npcMap.remove(id);
        if (npc == null) {
            return false;
        }
        npc.despawn();
        CustomNPCUtils.deleteNpcFile(id);
        return true;
    }

    public CustomNPC getNPC(String id) {
        return npcMap.get(id);
    }

    public Collection<CustomNPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcMap.values());
    }

    /**
     * Loads all CustomNPCs from disk (customnpcs/<id>.yml),
     * but does NOT spawn them here; we spawn them in CustomNPCModule.start().
     */
    public void loadAllFromDisk() {
        for (CustomNPC npc : CustomNPCStorage.loadAllCustomNPCs()) {
            npcMap.put(npc.getId(), npc);
        }
    }
}
