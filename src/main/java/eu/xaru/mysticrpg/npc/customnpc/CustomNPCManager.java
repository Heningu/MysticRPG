package eu.xaru.mysticrpg.npc.customnpc;

import eu.xaru.mysticrpg.entityhandling.EntityHandler;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomNPCManager {

    private final Map<String, CustomNPC> npcMap = new HashMap<>();

    public CustomNPC createNPC(String id, String name, Location loc, String modelId) {
        CustomNPC npc = new CustomNPC(id, name, loc, modelId);
        npc.save();
        npcMap.put(id, npc);

        // spawn stands
        EntityHandler.getInstance().spawnNPC(npc, true);

        return npc;
    }

    public boolean deleteNPC(String id) {
        CustomNPC npc = npcMap.get(id);
        if (npc == null) {
            return false;
        }
        EntityHandler.getInstance().deleteNPC(npc);

        npcMap.remove(id);
        CustomNPCUtils.deleteNpcFile(id);
        return true;
    }

    public CustomNPC getNPC(String id) {
        return npcMap.get(id);
    }

    public Collection<CustomNPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcMap.values());
    }

    public void loadAllFromDisk() {
        for (CustomNPC npc : CustomNPCStorage.loadAllCustomNPCs()) {
            npcMap.put(npc.getId(), npc);
        }
    }
}
