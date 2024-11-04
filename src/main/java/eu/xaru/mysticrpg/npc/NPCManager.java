package eu.xaru.mysticrpg.npc;

import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCManager {

    private final Platform<World, Player, ItemStack, Plugin> npcPlatform;
    private final Map<String, Npc<World, Player, ItemStack, Plugin>> npcs = new HashMap<>();

    public NPCManager(Platform<World, Player, ItemStack, Plugin> npcPlatform) {
        this.npcPlatform = npcPlatform;
    }

    public void createNPC(Location location, String name) {
        Profile profile = Profile.resolved(name, UUID.randomUUID());
        npcPlatform.newNpcBuilder()
                .profile(profile)
                .position(BukkitPlatformUtil.positionFromBukkitModern(location))
                .buildAndTrack()
                .thenAccept(npc -> npcs.put(name, npc));
    }

    public boolean deleteNPC(String name) {
        Npc<World, Player, ItemStack, Plugin> npc = npcs.remove(name);
        if (npc != null) {
            npc.remove();
            return true;
        }
        return false;
    }

    // Additional methods for managing NPCs can be added here
}
