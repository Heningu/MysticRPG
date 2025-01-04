/*
package eu.xaru.mysticrpg.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class NPCManager {

    private final Map<String, XaruNPC> npcs = new HashMap<>();

    public NPCManager() {
    }

    */
/**
     * Create an NPC with optional model usage.
     * @param location Where to spawn
     * @param id The unique ID for your NPC
     * @param name The display name
     * @param modeled If true => use ModelEngine
     * @param modelId Which model ID to apply (if modeled)
     *//*

    public void createNPC(Location location, String id, String name,
                          boolean modeled, String modelId) {

        XaruNPC npc = new XaruNPC(id, name, location, modeled, modelId);
        npcs.put(id, npc);

        // Immediately spawn the newly created NPC:
        npc.spawnIfMissing();

        // If Citizens created it => store the ID in npc.data() for re‐association
        net.citizensnpcs.api.npc.NPC cNpc = npc.getNpcEntity();
        if (cNpc != null) {
            cNpc.data().setPersistent("xaruid", id);

            // also set trait
            NPCInteractTrait trait = cNpc.getOrAddTrait(NPCInteractTrait.class);
            trait.setXaruNPC(npc);
        }
    }

    */
/**
     * Overload for "normal" creation
     *//*

    public void createNPC(Location location, String id, String name) {
        createNPC(location, id, name, false, null);
    }

    public boolean deleteNPC(String id) {
        XaruNPC npc = npcs.remove(id);
        if (npc != null) {
            npc.despawn();
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            net.citizensnpcs.api.npc.NPC npcEntity = npc.getNpcEntity();
            if (npcEntity != null) {
                registry.deregister(npcEntity);
            }
            return true;
        }
        return false;
    }

    public void deleteAllNPCs() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        // Despawn and deregister each NPC
        for (XaruNPC xaruNPC : npcs.values()) {
            xaruNPC.despawn();
            if (xaruNPC.getNpcEntity() != null) {
                registry.deregister(xaruNPC.getNpcEntity());
            }
        }

        // Finally, clear our internal map
        npcs.clear();
    }

    public XaruNPC getNPC(String id) {
        return npcs.get(id);
    }

    public Map<String, XaruNPC> getAllNPCs() {
        return new HashMap<>(npcs);
    }
}
*/
