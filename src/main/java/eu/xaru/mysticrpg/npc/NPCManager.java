package eu.xaru.mysticrpg.npc;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NPCManager {

    private final Platform<World, Player, ItemStack, Plugin> npcPlatform;
    private final HashMap<String, Npc<World, Player, ItemStack, Plugin>> npcs = new HashMap<>();

    public NPCManager(Platform<World, Player, ItemStack, Plugin> npcPlatform) {
        this.npcPlatform = npcPlatform;
    }

    /**
     * Creates an NPC asynchronously and returns a CompletableFuture.
     *
     * @param location The location where the NPC will spawn.
     * @param name     The name of the NPC.
     * @return A CompletableFuture that will complete with the created Npc instance.
     */
    public CompletableFuture<Npc<World, Player, ItemStack, Plugin>> createNPC(Location location, String name) {
        Profile profile = Profile.unresolved(name);
        CompletableFuture<Npc<World, Player, ItemStack, Plugin>> future = new CompletableFuture<>();

        npcPlatform.newNpcBuilder()
                .position(BukkitPlatformUtil.positionFromBukkitModern(location))
                .flag(Npc.LOOK_AT_PLAYER, true)
                .flag(Npc.HIT_WHEN_PLAYER_HITS, true)
                .profile(profile)
                .thenAccept(npcBuilder -> {
                    Npc<World, Player, ItemStack, Plugin> npc = npcBuilder.buildAndTrack();
                    npcs.put(name, npc);
                    future.complete(npc);
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });

        return future;
    }




    /**
     * Deletes an NPC synchronously.
     *
     * @param name The name of the NPC to delete.
     * @return True if the NPC was found and deleted, false otherwise.
     */
    public CompletableFuture<Boolean> deleteNPC(String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Npc<World, Player, ItemStack, Plugin> npc = npcs.remove(name);
        if (npc != null) {
            npc.unlink();
            future.complete(true);
        } else {
            future.complete(false);
        }
        return future;
    }

    /**
     * Retrieves an NPC by name.
     *
     * @param name The name of the NPC.
     * @return The Npc instance if found, null otherwise.
     */
    public Npc<World, Player, ItemStack, Plugin> getNPC(String name) {
        return npcs.get(name);
    }

    /**
     * Retrieves all managed NPCs.
     *
     * @return A map of NPC names to their corresponding Npc instances.
     */
    public Map<String, Npc<World, Player, ItemStack, Plugin>> getAllNPCs() {
        return new HashMap<>(npcs);
    }

    // Additional methods for managing NPCs can be added here
}
