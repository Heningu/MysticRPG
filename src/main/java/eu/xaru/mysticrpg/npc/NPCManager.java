package eu.xaru.mysticrpg.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class NPCManager {

    private final Map<String, NPC> npcs = new HashMap<>();

    public NPCManager() {
        // Constructor
    }

    /**
     * Creates an NPC at the given location with the specified name.
     *
     * @param location The location where the NPC will spawn.
     * @param name     The name of the NPC.
     */
    public void createNPC(Location location, String name) {
        NPC npc = new NPC(name, location);
        npcs.put(name, npc);
    }

    /**
     * Deletes an NPC by name.
     *
     * @param name The name of the NPC to delete.
     * @return True if the NPC was found and deleted, false otherwise.
     */
    public boolean deleteNPC(String name) {
        NPC npc = npcs.remove(name);
        if (npc != null) {
            npc.despawn();
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            net.citizensnpcs.api.npc.NPC npcEntity = npc.npcEntity;
            if (npcEntity != null) {
                registry.deregister(npcEntity);
            }
            return true;
        }
        return false;
    }

    /**
     * Retrieves an NPC by name.
     *
     * @param name The name of the NPC.
     * @return The NPC instance if found, null otherwise.
     */
    public NPC getNPC(String name) {
        return npcs.get(name);
    }

    /**
     * Retrieves all managed NPCs.
     *
     * @return A map of NPC names to their corresponding NPC instances.
     */
    public Map<String, NPC> getAllNPCs() {
        return new HashMap<>(npcs);
    }

    // Additional methods for managing NPCs can be added here
}
