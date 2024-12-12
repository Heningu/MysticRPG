package eu.xaru.mysticrpg.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class NPCManager {

    private final Map<String, NPC> npcs = new HashMap<>();

    public NPCManager() {
    }

    public void createNPC(Location location, String id, String name) {
        NPC npc = new NPC(id, name, location);
        npcs.put(id, npc);
    }

    public boolean deleteNPC(String id) {
        NPC npc = npcs.remove(id);
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

    public NPC getNPC(String id) {
        return npcs.get(id);
    }

    public Map<String, NPC> getAllNPCs() {
        return new HashMap<>(npcs);
    }
}
