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

    public void createNPC(Location location, String id, String name) {
        XaruNPC npc = new XaruNPC(id, name, location);
        npcs.put(id, npc);

        // Immediately spawn the newly created NPC:
        npc.spawnIfMissing();

        net.citizensnpcs.api.npc.NPC cNpc = npc.getNpcEntity();
        if (cNpc != null) {
            // Instead of a Persistence trait, use npc.data():
            cNpc.data().setPersistent("xaruid", id);

            // also set trait
            NPCInteractTrait trait = cNpc.getOrAddTrait(NPCInteractTrait.class);
            trait.setXaruNPC(npc);
        }
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

    public XaruNPC getNPC(String id) {
        return npcs.get(id);
    }

    public Map<String, XaruNPC> getAllNPCs() {
        return new HashMap<>(npcs);
    }
}
