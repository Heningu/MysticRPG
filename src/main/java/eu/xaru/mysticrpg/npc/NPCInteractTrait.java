/*
package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class NPCInteractTrait extends Trait {

    // We can store an XaruNPC reference, but it cannot be final
    // because Citizens needs a no-args constructor.
    private XaruNPC npc;

    // REQUIRED no-arguments constructor for Citizens:
    public NPCInteractTrait() {
        super("NPCInteractTrait");
    }

    // Optional: If you still want to pass in an XaruNPC
    // from your code at runtime, you can do so via a setter:
    public void setXaruNPC(XaruNPC npc) {
        this.npc = npc;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            // Only call npc.interact(...) if npc != null
            if (npc != null) {
                npc.interact(player);
            } else {
                player.sendMessage(ChatColor.RED + "This NPC has no associated XaruNPC instance!");
            }
        }
    }

    @EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            if (npc != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        Utils.getInstance().$("Please don't attack " + npc.getName() + "!")));
            } else {
                player.sendMessage(ChatColor.RED + "This NPC has no associated XaruNPC instance!");
            }
        }
    }
}
*/
